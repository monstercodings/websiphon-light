package top.codings.websiphon.light.requester.support;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.Map;

@Getter
@Setter
public class NettyRequest extends BaseRequest<HttpRequest> {
    protected HttpRequest httpRequest;
    @Setter
    private Channel channel;

    public NettyRequest(HttpRequest httpRequest) {
        this(httpRequest, null);
    }

    public NettyRequest(HttpRequest httpRequest, Object userData) {
        this.httpRequest = httpRequest;
        this.userData = userData;
        this.uri = URI.create(httpRequest.uri());
    }

    @Override
    public void setHeaders(Map<String, Object> headers) {
        headers.forEach((s, o) -> {
            if (httpRequest.headers().contains(s)) {
                return;
            }
            httpRequest.headers().set(s, o);
        });
    }

    @Override
    public void stop() {
        if (null != channel) {
            System.out.println("关闭通道");
            channel.close();
            return;
        }
        if (future != null) {
            System.out.println("主动停止");
            future.complete(this);
        }
    }

    @Override
    public void release() {
        super.release();
        httpRequest = null;
    }
}
