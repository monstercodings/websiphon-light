package top.codings.websiphon.light.requester;

import top.codings.websiphon.light.manager.QueueResponseHandler;

public interface AsyncRequester extends IRequester {
    /**
     * 设置响应处理器
     * @param responseHandler
     */
    void setResponseHandler(QueueResponseHandler responseHandler);
}
