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
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.ssl.SSLContextBuilder;
import top.codings.websiphon.light.config.RequesterConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.loader.bean.PluginType;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.utils.HttpCharsetUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Shared
@PluginDefinition(
        name = "Netty请求器",
        description = "基于Netty4而定制化开发的请求器，具备效率高，内存占用少，配置丰富，可限制上传下载速度等特点",
        version = "0.0.1",
        type = PluginType.REQUESTER)
public class NettyRequester extends CombineRequester<NettyRequest> {
    private Bootstrap bootstrap;
    private NioEventLoopGroup workerGroup;
    private SSLContext sslContext;
    private ScheduledExecutorService executor;
    private GlobalTrafficShapingHandler trafficHandler;
    private RequesterConfig config;
    @Getter
    @Setter
    private IResponseHandler responseHandler;
    private Proxy globalProxy;

    public NettyRequester() {
        this(null, null);
    }

    public NettyRequester(RequesterConfig config) {
        this(null, config);

    }

    protected NettyRequester(CombineRequester requester, RequesterConfig config) {
        super(requester);
        if (config == null) {
            config = RequesterConfig.builder()
                    .connectTimeoutMillis(6000)
                    .idleTimeMillis(6000)
                    .ignoreSslError(false)
                    .maxContentLength(1024 * 512)
                    .networkErrorStrategy(NetworkErrorStrategy.RESPONSE)
                    .build();
        }
        setStrategy(config.getNetworkErrorStrategy());
        if (config.getProxy() != null) {
            globalProxy = config.getProxy();
        }
        this.config = config;
        executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        trafficHandler = new GlobalTrafficShapingHandler(executor, config.getUploadBytesPerSecond(), config.getDownloadBytesPerSecond());
        /*new Thread(() -> {
            TrafficCounter counter = trafficHandler.trafficCounter();
            for (; ; ) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                System.out.println(counter.toString());
            }
        }).start();*/
    }

    @Override
    protected void init(ICrawler crawler, int index) throws Exception {
        if (index > 0) {
            return;
        }
        bootstrap = new Bootstrap();
        workerGroup = new NioEventLoopGroup();
        bootstrap
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMillis())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addFirst("@GlobalTrafficShapingHandler", trafficHandler);
                    }
                })
        ;
        if (config.isIgnoreSslError()) {
            sslContext = SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build();
        } else {
            sslContext = SSLContext.getDefault();
        }
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
        ChannelFuture connectFuture;
        // TODO 需要妥善处理非HTTP代理协议的请求
        if (request.getProxy() != null) {
            Proxy proxy = request.getProxy();
            if (proxy == Proxy.NO_PROXY) {
                connectFuture = bootstrap.connect(host, port);
            } else {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                connectFuture = bootstrap.connect(address.getHostString(), address.getPort());
                connectFuture.channel().attr(AttributeKey.valueOf("proxy")).set(proxy);
            }
        } else if (globalProxy != null) {
            InetSocketAddress address = (InetSocketAddress) globalProxy.address();
            connectFuture = bootstrap.connect(address.getHostString(), address.getPort());
            connectFuture.channel().attr(AttributeKey.valueOf("proxy")).set(globalProxy);
        } else {
            connectFuture = bootstrap.connect(host, port);
        }
        connectFuture.addListener((ChannelFutureListener) channelFuture -> {
            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) inChannelFuture -> {
                request.lock();
                try {
                    if (request.requestResult == null && request.getStatus() != IRequest.Status.TIMEOUT) {
                        request.requestResult = new IRequest.RequestResult();
                        request.requestResult.setSucceed(false);
                        request.setStatus(IRequest.Status.ERROR);
                        FrameworkException exception;
                        if (
                                channelFuture.channel().eventLoop().isShutdown() ||
                                        channelFuture.channel().eventLoop().isTerminated() ||
                                        channelFuture.channel().eventLoop().isShuttingDown()
                        ) {
                            exception = new FrameworkException("请求任务被中止");
                        } else {
                            exception = new FrameworkException("请求任务的Http连接被异常关闭，可能是对方网站服务器关闭了该连接");
                        }
                        request.requestResult.setThrowable(exception);
                        if (null != responseHandler && getStrategy() == NetworkErrorStrategy.RESPONSE) {
                            responseHandler.handle(request);
                        }
                    }
                    cf.completeAsync(() -> request);
                } finally {
                    request.unlock();
                }
            });
            if (!channelFuture.isSuccess()) {
                channelError(request, channelFuture.channel(), channelFuture.cause());
                return;
            }
            Proxy proxy = (Proxy) channelFuture.channel().attr(AttributeKey.valueOf("proxy")).get();
            if (null != proxy) {
                String uristr = host + ":" + finalPort;
                DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(
                        HttpVersion.HTTP_1_1, HttpMethod.CONNECT, uristr, Unpooled.EMPTY_BUFFER);
                defaultFullHttpRequest.headers().set("Host", uristr);
                channelFuture.channel().pipeline().addLast("@HttpClientCodec", new HttpClientCodec());
                channelFuture.channel().writeAndFlush(defaultFullHttpRequest).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            channelError(request, future.channel(), future.cause());
                            return;
                        }
                        future.channel().pipeline()
                                .addLast("@HttpObjectAggregator", new HttpObjectAggregator(Integer.MAX_VALUE))
                                .addLast("@ProxyHandler", new SimpleChannelInboundHandler<HttpResponse>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) throws Exception {
                                        if (msg.decoderResult().isSuccess() && msg.status().code() == 200) {
                                            ctx.channel().pipeline().remove("@HttpClientCodec");
                                            ctx.channel().pipeline().remove("@HttpObjectAggregator");
                                            ctx.channel().pipeline().remove("@ProxyHandler");
                                            doTargetRequest(request, uri, scheme, host, finalPort, ctx.channel());
                                            return;
                                        }
                                        channelError(request, ctx.channel(), new FrameworkException("无法连接代理服务器"));
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        channelError(request, ctx.channel(), cause);
                                    }
                                });
                    }
                });
                return;
            }
            doTargetRequest(request, uri, scheme, host, finalPort, channelFuture.channel());
        });

        return cf;
    }

    private void doTargetRequest(NettyRequest request, URI uri, String scheme, String host, int finalPort, Channel channel) {
        channel.pipeline()
                .addLast("@IdleStateHandler", new IdleStateHandler(0, 0, config.getIdleTimeMillis(), TimeUnit.MILLISECONDS) {
                    @Override
                    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
                        request.lock();
                        try {
                            if (null != request.requestResult || request.getStatus() == IRequest.Status.TIMEOUT) {
                                return;
                            }
                            request.requestResult = new IRequest.RequestResult();
                            request.requestResult.setThrowable(new FrameworkException("网络传输超时"));
                            request.requestResult.setSucceed(false);
                        } finally {
                            request.unlock();
                            ctx.close();
                        }
                    }
                })
                .addLast(new HttpClientCodec())
                .addLast(new HttpContentDecompressor())
                .addLast(new HttpObjectAggregator(config.getMaxContentLength()))
                .addLast(new SimpleChannelInboundHandler<HttpResponse>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpResponse httpResponse) throws Exception {
                        request.lock();
                        try {
                            if (null != request.requestResult || request.getStatus() == IRequest.Status.TIMEOUT) {
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
                                request.requestResult.setData("");
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
                            ContentType contentType = ContentType.DEFAULT_BINARY;
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
                            if (mimeType.contains("json") && charset == null) {
                                charset = Charset.forName("utf-8");
                            }
                            if (mimeType.contains("text") && charset == null) {
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
                            request.setContentType(contentType);
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
                            if (null != request.requestResult || request.getStatus() == IRequest.Status.TIMEOUT) {
                                return;
                            }
                            request.setStatus(IRequest.Status.ERROR);
                            request.requestResult = new IRequest.RequestResult();
                            request.requestResult.setThrowable(cause);
                            request.requestResult.setSucceed(false);
                            if (getStrategy() == NetworkErrorStrategy.RESPONSE && null != responseHandler) {
                                responseHandler.handle(request);
                            }
                        } finally {
                            request.unlock();
                            ctx.close();
                        }
                    }
                })
        ;
        if (scheme.equalsIgnoreCase("https")) {
            InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
            SSLEngine sslEngine = sslContext.createSSLEngine(address.getHostString(), address.getPort());
            sslEngine.setUseClientMode(true);
            channel.pipeline().addAfter("@IdleStateHandler", "@SSL", new SslHandler(sslEngine));
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
        channel.writeAndFlush(httpRequest).addListener((ChannelFutureListener) innerFuture -> {
            if (!innerFuture.isSuccess()) {
                channelError(request, innerFuture.channel(), innerFuture.cause());
            }
        });
    }

    private void channelError(NettyRequest request, Channel channel, Throwable throwable) {
        request.lock();
        try {
            if (request.requestResult == null) {
                request.setStatus(IRequest.Status.ERROR);
                request.requestResult = new IRequest.RequestResult();
                request.requestResult.setSucceed(false);
                request.requestResult.setThrowable(throwable);
                if (getStrategy() == NetworkErrorStrategy.RESPONSE && null != responseHandler) {
                    responseHandler.handle(request);
                }
            }
            channel.close();
        } finally {
            request.unlock();
        }
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
    protected void close(int index) {
        if (index != 0) {
            return;
        }
        if (null != executor) {
            trafficHandler.release();
            executor.shutdownNow();
            try {
                executor.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = null;
            trafficHandler = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().awaitUninterruptibly();
            sslContext = null;
            workerGroup = null;
        }
    }

    public TrafficCounter getTrafficCounter() {
        return trafficHandler.trafficCounter();
    }
}
