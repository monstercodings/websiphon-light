package top.codings.websiphon.light.test.dependent;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.BaseRequest;
import top.codings.websiphon.light.requester.support.CombineRequester;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DoNothingRequester extends CombineRequester {
    private ExecutorService exe = Executors.newSingleThreadExecutor();
    private IResponseHandler responseHandler;

    public DoNothingRequester() {
        super(null);
    }

    protected DoNothingRequester(CombineRequester requester) {
        super(requester);
    }

    @Override
    public void init(ICrawler crawler) {
        exe.submit(() -> {
        });
    }

    @Override
    public CompletableFuture execute(IRequest request) {
        return new CompletableFuture();
    }

    @Override
    public IRequest create(String url) {
        return create(url, null);
    }

    @Override
    public IRequest create(String url, Object userData) {
        return new BaseRequest() {
            private URI uri = URI.create(url);

            @Override
            public URI getUri() {
                return uri;
            }

            @Override
            public Object getHttpRequest() {
                return null;
            }

            @Override
            public void setHttpRequest(Object httpRequest) {

            }

            @Override
            public void setHeaders(Map headers) {

            }
        };
    }

    @Override
    public void setResponseHandler(IResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    public IResponseHandler getResponseHandler() {
        return responseHandler;
    }

    @Override
    public void close() {
        exe.shutdownNow();
        try {
            exe.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        }
    }
}
