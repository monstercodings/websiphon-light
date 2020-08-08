package top.codings.websiphon.light.requester;

import top.codings.websiphon.light.manager.QueueResponseHandler;

public interface AsyncRequester<T extends IRequest> extends IRequester<T> {
    /**
     * 设置响应处理器
     *
     * @param responseHandler
     */
    void setResponseHandler(QueueResponseHandler responseHandler);
}
