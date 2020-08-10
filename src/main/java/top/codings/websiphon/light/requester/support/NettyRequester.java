package top.codings.websiphon.light.requester.support;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.ssl.SSLContextBuilder;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.utils.HttpCharsetUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NettyRequester extends CombineRequester<NettyRequest> implements AsyncRequester<NettyRequest> {
    private NioEventLoopGroup workerGroup;
    private SSLContext sslContext;
    @Setter
    @Getter
    private QueueResponseHandler responseHandler;

    public NettyRequester() {
        this(null);
    }

    protected NettyRequester(CombineRequester requester) {
        super(requester);
    }

    @Override
    public void init() {
        workerGroup = new NioEventLoopGroup();
        try {
            sslContext = SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build();
        } catch (Exception e) {
            throw new RuntimeException("初始化SSL套件失败", e);
        }
    }

    @Override
    public CompletableFuture<NettyRequest> executeAsync(final NettyRequest request) {
        CompletableFuture<NettyRequest> cf = new CompletableFuture();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                            cf.completeAsync(() -> request);
                        });
                        socketChannel.pipeline()
                                .addLast("@IdleStateHandler", new IdleStateHandler(0, 0, 3, TimeUnit.SECONDS) {
                                    @Override
                                    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
                                        request.lock();
                                        try {
                                            if (null != request.requestResult) {
                                                return;
                                            }
                                            request.requestResult = new IRequest.RequestResult();
                                            request.requestResult.setThrowable(new RuntimeException("网络传输超时"));
                                            request.requestResult.setSucceed(false);
                                        } finally {
                                            request.unlock();
                                            ctx.close();
                                        }
                                    }
                                })
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpContentDecompressor())
                                .addLast(new HttpObjectAggregator(1024 * 512))
                                .addLast(new SimpleChannelInboundHandler<HttpResponse>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpResponse httpResponse) throws Exception {
                                        request.lock();
                                        try {
                                            if (null != request.requestResult) {
                                                return;
                                            }
                                            request.requestResult = new IRequest.RequestResult();
                                            if (!httpResponse.decoderResult().isSuccess()) {
                                                request.requestResult.setSucceed(false);
                                                request.requestResult.setThrowable(new RuntimeException("响应解析失败"));
                                                responseHandler.push(request);
                                                return;
                                            }
                                            int code = httpResponse.status().code();
                                            request.httpResponse.setCode(code);
                                            if (code < 200 || code >= 300) {
                                                request.requestResult.setResponseType(IRequest.ResponseType.ERROR_CODE);
                                                responseHandler.push(request);
                                                return;
                                            }
                                            String contentTypeStr = httpResponse.headers().get("content-type");
                                            byte[] body;
                                            if (httpResponse instanceof FullHttpResponse) {
                                                FullHttpResponse response = (FullHttpResponse) httpResponse;
                                                body = ByteBufUtil.getBytes(response.content());
                                            } else {
                                                System.out.println("响应类型尚未有处理方案 -> " + httpResponse.getClass().getName());
                                                request.requestResult.setSucceed(false);
                                                request.requestResult.setThrowable(new RuntimeException("响应类型不匹配"));
                                                return;
                                            }
                                            Charset charset;
                                            String mimeType;
                                            ContentType contentType;
                                            if (StringUtils.isNotBlank(contentTypeStr)) {
                                                contentType = ContentType.parse(contentTypeStr);
                                                mimeType = contentType.getMimeType();
                                                charset = contentType.getCharset();
                                            } else {
                                                charset = HttpCharsetUtil.findCharset(body);
                                                mimeType = "text/html";
                                            }
                                            if (charset == null) {
                                                channelHandlerContext.close();
                                                request.requestResult.setResponseType(IRequest.ResponseType.NO_CHARSET);
                                                responseHandler.push(request);
                                                return;
                                            }
                                            if (mimeType.contains("text")) {
                                                // 文本解析
                                                System.out.println(new String(body, charset));
                                                request.requestResult.setResponseType(IRequest.ResponseType.TEXT);
                                                request.requestResult.setData(Optional.ofNullable(new String(body, charset)).orElse("<html>该网页无内容</html>"));
                                            } else if (mimeType.contains("json")) {
                                                // JSON解析
                                                System.out.println(JSON.toJSONString(new String(body, charset)));
                                                request.requestResult.setResponseType(IRequest.ResponseType.JSON);
                                                request.requestResult.setData(JSON.parse(Optional.ofNullable(new String(body, charset)).orElse("{}")));
                                            } else {
                                                // 字节解析
                                                request.requestResult.setResponseType(IRequest.ResponseType.BYTE);
                                                request.requestResult.setData(body);
                                            }
                                            responseHandler.push(request);
                                        } catch (Exception e) {
                                            request.requestResult.setSucceed(false);
                                            request.requestResult.setThrowable(e);
                                        } finally {
                                            request.unlock();
                                            channelHandlerContext.close();
                                        }
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        request.lock();
                                        try {
                                            if (null != request.requestResult) {
                                                return;
                                            }
                                            request.requestResult = new IRequest.RequestResult();
                                            request.requestResult.setThrowable(cause);
                                            request.requestResult.setSucceed(false);
                                        } finally {
                                            request.unlock();
                                            ctx.close();
                                        }
                                        /*if (cause instanceof TooLongFrameException) {
                                            System.err.println("内容太长无法请求成功");
                                            ctx.close();
                                            return;
                                        }
                                        super.exceptionCaught(ctx, cause);*/
                                    }
                                })
                        ;
                    }
                })
        ;
        URI uri = request.httpResponse.getUri();
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        if (port <= 0) {
            if (scheme.equalsIgnoreCase("https")) {
                port = 443;
            } else {
                port = 80;
            }
        }
        int finalPort = port;
        bootstrap.connect(host, port).addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                if (scheme.equalsIgnoreCase("https")) {
                    InetSocketAddress address = (InetSocketAddress) channelFuture.channel().remoteAddress();
                    SSLEngine sslEngine = sslContext.createSSLEngine(address.getHostString(), address.getPort());
                    sslEngine.setUseClientMode(true);
                    channelFuture.channel().pipeline().addAfter("@IdleStateHandler", "@SSL", new SslHandler(sslEngine));
                }
                HttpRequest httpRequest = request.httpRequest;
                if (!httpRequest.headers().contains("User-Agent")) {
                    httpRequest.headers().set("User-Agent", "H.J/http-client/0.0.1");
                }
                httpRequest.headers()
                        .set("Host", host + ":" + finalPort)
                        .set("Origin", uri.toString())
                        .set("Referer", uri.toString())
//                        .set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
//                        .set("Accept-Encoding", "gzip, deflate, compress")
//                        .set("Accept-Language", "zh-CN,zh;q=0.9")
//                        .set("Cache-Control", "no-cache")
//                        .set("Connection", "keep-alive")
//                        .set("DNT", "1")
//                        .set("Upgrade-Insecure-Requests", "1")
//                        .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36")
                ;
                channelFuture.channel().writeAndFlush(httpRequest).addListener((ChannelFutureListener) channelFuture1 -> {
                    if (!channelFuture1.isSuccess()) {
                        request.requestResult = new IRequest.RequestResult();
                        request.requestResult.setSucceed(false);
                        request.requestResult.setThrowable(channelFuture1.cause());
                        channelFuture1.channel().close();
                    }
                });
            } else {
                request.requestResult = new IRequest.RequestResult();
                request.requestResult.setSucceed(false);
                request.requestResult.setThrowable(channelFuture.cause());
                channelFuture.channel().close();
            }
        });

        return cf;
    }

    @Override
    public NettyRequest create(String url) {
        return create(url, null);
    }

    @Override
    public NettyRequest create(String url, Object userData) {
        URI uri = URI.create(url);
        String path = uri.getPath();
        if (StringUtils.isBlank(path)) {
            path = "/";
        }
        HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path, Unpooled.EMPTY_BUFFER);
        NettyRequest request = new NettyRequest(httpRequest, userData);
        request.setHttpResponse(new NettyRequest.HttpResponse(uri));
        return request;
    }

    @Override
    public void shutdown(boolean force) {
        workerGroup.shutdownGracefully().addListener((GenericFutureListener<Future<? super Object>>) future -> {
            responseHandler.shutdown(force);
            sslContext = null;
            workerGroup = null;
        });
    }
}
