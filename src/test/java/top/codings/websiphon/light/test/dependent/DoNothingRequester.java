package top.codings.websiphon.light.test.dependent;

import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.BaseRequest;
import top.codings.websiphon.light.requester.support.CombineRequester;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DoNothingRequester extends CombineRequester implements AsyncRequester {
    private QueueResponseHandler queueResponseHandler;

    public DoNothingRequester() {
        super(null);
    }

    protected DoNothingRequester(CombineRequester requester) {
        super(requester);
    }

    @Override
    public CompletableFuture<IRequester> init() {
        return CompletableFuture.completedFuture(this);
    }

    @Override
    public CompletableFuture executeAsync(IRequest request) {
        return new CompletableFuture();
    }

    @Override
    public IRequest create(String url) {
        return create(url, null);
    }

    @Override
    public IRequest create(String url, Object userData) {
        return new BaseRequest() {
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
    public IResponseHandler getResponseHandler() {
        return queueResponseHandler;
    }

    @Override
    public CompletableFuture<IRequester> shutdown(boolean force) {
        return CompletableFuture.completedFuture(this);
    }

    @Override
    public void setResponseHandler(QueueResponseHandler responseHandler) {
        this.queueResponseHandler = responseHandler;
    }
}
