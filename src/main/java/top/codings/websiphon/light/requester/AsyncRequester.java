package top.codings.websiphon.light.requester;

import top.codings.websiphon.light.function.handler.QueueResponseHandler;

public interface AsyncRequester<T extends IRequest> extends IRequester<T> {
    /**
     * 设置响应处理器
     *
     * @param responseHandler
     */
    void setResponseHandler(QueueResponseHandler responseHandler);
}
