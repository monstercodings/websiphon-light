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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.ssl.SSLContextBuilder;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.utils.HttpCharsetUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyRequester extends CombineRequester<NettyRequest> {
    private Bootstrap bootstrap;
    private NioEventLoopGroup workerGroup;
    private SSLContext sslContext;
    @Getter
    @Setter
    private IResponseHandler responseHandler;

    public NettyRequester() {
        this(null);
    }

    protected NettyRequester(CombineRequester requester) {
        super(requester);
    }

    @Override
    public CompletableFuture<IRequester> init() {
        bootstrap = new Bootstrap();
        workerGroup = new NioEventLoopGroup();
        bootstrap
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 6000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        /*socketChannel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                            request.lock();
                            try {
                                cf.completeAsync(() -> request);
                            } finally {
                                request.unlock();
                            }
                        });
                        socketChannel.pipeline()
                                .addLast("@IdleStateHandler", new IdleStateHandler(0, 0, 6, TimeUnit.SECONDS) {
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
                                            request.setStatus(IRequest.Status.RESPONSE);
                                            request.requestResult = new IRequest.RequestResult();
                                            if (!httpResponse.decoderResult().isSuccess()) {
                                                request.requestResult.setSucceed(false);
                                                request.requestResult.setThrowable(new RuntimeException("响应解析失败"));
                                                responseHandler.handle(request);
                                                return;
                                            }
                                            int code = httpResponse.status().code();
                                            request.requestResult.setCode(code);
                                            if (code < 200 || code >= 300) {
                                                request.requestResult.setResponseType(IRequest.ResponseType.ERROR_CODE);
                                                responseHandler.handle(request);
                                                return;
                                            }
                                            String contentTypeStr = httpResponse.headers().get("content-type");
                                            byte[] body;
                                            if (httpResponse instanceof FullHttpResponse) {
                                                FullHttpResponse response = (FullHttpResponse) httpResponse;
                                                body = ByteBufUtil.getBytes(response.content());
                                            } else {
                                                log.warn("响应类型尚未有处理方案 -> %s", httpResponse.getClass().getName());
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
                                            if ((mimeType.contains("text") || mimeType.contains("json")) && charset == null) {
                                                channelHandlerContext.close();
                                                request.requestResult.setResponseType(IRequest.ResponseType.NO_CHARSET);
                                                responseHandler.handle(request);
                                                return;
                                            }
                                            if (mimeType.contains("text")) {
                                                // 文本解析
                                                request.requestResult.setResponseType(IRequest.ResponseType.TEXT);
                                                request.requestResult.setData(Optional.ofNullable(new String(body, charset)).orElse("<html>该网页无内容</html>"));
                                            } else if (mimeType.contains("json")) {
                                                // JSON解析
                                                request.requestResult.setResponseType(IRequest.ResponseType.JSON);
                                                request.requestResult.setData(JSON.parse(Optional.ofNullable(new String(body, charset)).orElse("{}")));
                                            } else {
                                                // 字节解析
                                                request.requestResult.setResponseType(IRequest.ResponseType.BYTE);
                                                request.requestResult.setData(body);
                                            }
                                            responseHandler.handle(request);
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
                                            request.setStatus(IRequest.Status.ERROR);
                                            request.requestResult = new IRequest.RequestResult();
                                            request.requestResult.setThrowable(cause);
                                            request.requestResult.setSucceed(false);
                                            if (getStrategy() == IRequester.NetworkErrorStrategy.RESPONSE) {
                                                responseHandler.handle(request);
                                            }
                                        } finally {
                                            request.unlock();
                                            ctx.close();
                                        }
                                    }
                                })
                        ;*/
                    }
                })
        ;
        try {
            sslContext = SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new FrameworkException("初始化SSL套件失败", e));
        }
        return CompletableFuture.completedFuture(this);
    }

    @Override
    public CompletableFuture<NettyRequest> execute(final NettyRequest request) {
        CompletableFuture<NettyRequest> cf = new CompletableFuture();
        URI uri = request.getUri();
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
            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) inChannelFuture -> {
                request.lock();
                try {
                    cf.completeAsync(() -> request);
                } finally {
                    request.unlock();
                }
            });
            if (channelFuture.isSuccess()) {
                channelFuture.channel().pipeline()
                        .addLast("@IdleStateHandler", new IdleStateHandler(0, 0, 6, TimeUnit.SECONDS) {
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
                                    request.setStatus(IRequest.Status.RESPONSE);
                                    request.requestResult = new IRequest.RequestResult();
                                    if (!httpResponse.decoderResult().isSuccess()) {
                                        request.requestResult.setSucceed(false);
                                        request.requestResult.setThrowable(new RuntimeException("响应解析失败"));
                                        if (null != responseHandler) {
                                            responseHandler.handle(request);
                                        }
                                        return;
                                    }
                                    int code = httpResponse.status().code();
                                    request.requestResult.setCode(code);
                                    if (code < 200 || code >= 300) {
                                        request.requestResult.setResponseType(IRequest.ResponseType.ERROR_CODE);
                                        if (null != responseHandler) {
                                            responseHandler.handle(request);
                                        }
                                        return;
                                    }
                                    String contentTypeStr = httpResponse.headers().get("content-type");
                                    byte[] body;
                                    if (httpResponse instanceof FullHttpResponse) {
                                        FullHttpResponse response = (FullHttpResponse) httpResponse;
                                        body = ByteBufUtil.getBytes(response.content());
                                    } else {
                                        log.warn("响应类型尚未有处理方案 -> %s", httpResponse.getClass().getName());
                                        request.requestResult.setSucceed(false);
                                        request.requestResult.setThrowable(new RuntimeException("响应类型不匹配"));
                                        return;
                                    }
                                    Charset charset = null;
                                    String mimeType;
                                    ContentType contentType;
                                    if (StringUtils.isNotBlank(contentTypeStr)) {
                                        contentType = ContentType.parse(contentTypeStr);
                                        mimeType = contentType.getMimeType();
                                        charset = contentType.getCharset();
                                    } else {
                                        mimeType = "text/html";
                                    }
                                    if (charset == null) {
                                        charset = HttpCharsetUtil.findCharset(body);
                                    }
                                    if ((mimeType.contains("text") || mimeType.contains("json")) && charset == null) {
                                        request.requestResult.setResponseType(IRequest.ResponseType.NO_CHARSET);
                                        if (null != responseHandler) {
                                            responseHandler.handle(request);
                                        }
                                        return;
                                    }
                                    if (mimeType.contains("text")) {
                                        // 文本解析
                                        request.requestResult.setResponseType(IRequest.ResponseType.TEXT);
                                        request.requestResult.setData(Optional.ofNullable(new String(body, charset)).orElse("<html>该网页无内容</html>"));
                                    } else if (mimeType.contains("json")) {
                                        // JSON解析
                                        request.requestResult.setResponseType(IRequest.ResponseType.JSON);
                                        request.requestResult.setData(JSON.parse(Optional.ofNullable(new String(body, charset)).orElse("{}")));
                                    } else {
                                        // 字节解析
                                        request.requestResult.setResponseType(IRequest.ResponseType.BYTE);
                                        request.requestResult.setData(body);
                                    }
                                    if (null != responseHandler) {
                                        responseHandler.handle(request);
                                    }
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
                                    request.setStatus(IRequest.Status.ERROR);
                                    request.requestResult = new IRequest.RequestResult();
                                    request.requestResult.setThrowable(cause);
                                    request.requestResult.setSucceed(false);
                                    if (getStrategy() == IRequester.NetworkErrorStrategy.RESPONSE && null != responseHandler) {
                                        responseHandler.handle(request);
                                    }
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
                channelFuture.channel().writeAndFlush(httpRequest).addListener((ChannelFutureListener) innerFuture -> {
                    if (!innerFuture.isSuccess()) {
                        channelError(request, innerFuture);
                    }
                });
            } else {
                channelError(request, channelFuture);
            }
        });

        return cf;
    }

    private void channelError(NettyRequest request, ChannelFuture future) {
        request.setStatus(IRequest.Status.ERROR);
        request.requestResult = new IRequest.RequestResult();
        request.requestResult.setSucceed(false);
        request.requestResult.setThrowable(future.cause());
        if (getStrategy() == IRequester.NetworkErrorStrategy.RESPONSE && null != responseHandler) {
            responseHandler.handle(request);
        }
        future.channel().close();
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
        if (StringUtils.isNotBlank(uri.getQuery())) {
            path = path.concat("?").concat(uri.getQuery());
        }
        HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path, Unpooled.EMPTY_BUFFER);
        NettyRequest request = new NettyRequest(httpRequest, userData);
        request.setUri(uri);
        return request;
    }

    @Override
    public CompletableFuture<IRequester> shutdown(boolean force) {
        CompletableFuture completableFuture = new CompletableFuture();
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().addListener((GenericFutureListener<Future<? super Object>>) future -> {
                sslContext = null;
                workerGroup = null;
                completableFuture.completeAsync(() -> this);
            });
        } else {
            completableFuture.complete(this);
        }
        return completableFuture;
    }
}
