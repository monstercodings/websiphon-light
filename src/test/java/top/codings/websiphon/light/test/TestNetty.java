package top.codings.websiphon.light.test;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import org.apache.http.entity.ContentType;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.platform.commons.util.StringUtils;
import top.codings.websiphon.light.utils.HttpCharsetUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestNetty {
    public static void main(String[] args) throws Exception {
        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
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
//                            System.out.println("通道关闭");
                            workerGroup.shutdownGracefully();
                        });
                        socketChannel.pipeline()
                                /*.addLast("@IdleStateHandler", new IdleStateHandler(0, 0, 3, TimeUnit.SECONDS) {
                                    @Override
                                    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
                                        System.out.println("触发Idle事件 首次:" + evt.isFirst() + " | 状态:" + evt.state().name());
                                        ctx.close();
                                    }
                                })*/
                                .addLast("@HttpClientCodec", new HttpClientCodec())
                                .addLast(new HttpContentDecompressor())
                                .addLast(new HttpObjectAggregator(1024 * 512))
                                .addLast(new SimpleChannelInboundHandler<HttpResponse>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpResponse httpResponse) throws Exception {
                                        if (!httpResponse.decoderResult().isSuccess()) {
                                            System.err.println("响应解析失败");
                                            channelHandlerContext.close();
                                            return;
                                        }
                                        int code = httpResponse.status().code();
                                        if (code < 200 || code >= 300) {
                                            System.err.println("响应异常 -> " + code + " " + httpResponse.status().reasonPhrase());
                                            channelHandlerContext.close();
                                            return;
                                        }
                                        String contentTypeStr = httpResponse.headers().get("content-type");
//                                        System.out.println("Content-Type:" + contentTypeStr);
                                        byte[] body;
                                        if (httpResponse instanceof FullHttpResponse) {
                                            FullHttpResponse response = (FullHttpResponse) httpResponse;
                                            body = ByteBufUtil.getBytes(response.content());
                                        } else {
                                            System.out.println("响应类型尚未有处理方案 -> " + httpResponse.getClass().getName());
                                            channelHandlerContext.close();
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
                                            System.err.println("无法找到编码类型");
                                            channelHandlerContext.close();
                                            return;
                                        }
                                        if (mimeType.contains("text")) {
                                            // 文本解析
                                            String content = new String(body, charset);
                                            content = content.length() > 10 ? content.substring(0, 10) : content;
                                            System.out.println(String.format("[%s] %s\n%s", Thread.currentThread().getName(), content, "------------------------------------------------------------"));
//                                            request.requestResult.setResponseType(IRequest.ResponseType.TEXT);
//                                            request.requestResult.setData(Optional.ofNullable(new String(body, charset)).orElse("<html>该网页无内容</html>"));
                                        } else if (mimeType.contains("json")) {
                                            // JSON解析
                                            System.out.println(JSON.toJSONString(new String(body, charset)));
//                                            request.requestResult.setResponseType(IRequest.ResponseType.JSON);
//                                            request.requestResult.setData(JSON.parse(Optional.ofNullable(new String(body, charset)).orElse("{}")));
                                        } else {
                                            // 字节解析
//                                            request.requestResult.setResponseType(IRequest.ResponseType.UNKNOW);
//                                            request.requestResult.setData(body);
                                        }
                                        channelHandlerContext.close();
//                                        System.out.println("------------------------------------------------------------");
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        if (cause instanceof TooLongFrameException) {
                                            System.err.println("内容太长无法请求成功");
                                            ctx.close();
                                            return;
                                        }
                                    }
                                })
                        ;
                    }
                })
        ;
        URI uri = URI.create("https://news.163.com");
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
        final String path = StringUtils.isBlank(uri.getPath()) ? "/" : uri.getPath();
        int finalPort = port;
        ExecutorService exe = Executors.newCachedThreadPool();
        for (int i = 0; i < 30; i++) {
            exe.submit(() -> {
                bootstrap.connect(host, finalPort).addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess()) {
                        if (scheme.equalsIgnoreCase("https")) {
                            InetSocketAddress address = (InetSocketAddress) channelFuture.channel().remoteAddress();
                            SSLEngine sslEngine = sslContext.createSSLEngine(address.getHostString(), address.getPort());
                            sslEngine.setUseClientMode(true);
                            channelFuture.channel().pipeline().addBefore("@HttpClientCodec", "@SSL", new SslHandler(sslEngine));
                        }

                        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path, Unpooled.EMPTY_BUFFER);
                        request.headers()
                                .set("Host", host + ":" + finalPort)
                                .set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                                .set("Accept-Encoding", "gzip, deflate, compress")
                                .set("Accept-Language", "zh-CN,zh;q=0.9")
                                .set("Cache-Control", "no-cache")
                                .set("Connection", "keep-alive")
                                .set("DNT", "1")
                                .set("Origin", uri.toString())
                                .set("Referer", uri.toString())
                                .set("Upgrade-Insecure-Requests", "1")
                                .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
                        channelFuture.channel().writeAndFlush(request).addListener((ChannelFutureListener) channelFuture1 -> {
                            if (!channelFuture1.isSuccess()) {
                                System.err.println("发送请求失败");
                            }
                        });
                    } else {
                        System.err.println("连接失败");
                        channelFuture.channel().close();
                    }
                });
            });
        }
    }
}
