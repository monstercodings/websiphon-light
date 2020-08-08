package top.codings.websiphon.light.requester;

import top.codings.websiphon.light.manager.IResponseHandler;

public interface SyncRequester<T extends IRequest> extends IRequester<T> {
    /**
     * 设置响应处理器
     *
     * @param responseHandler
     */
    void setResponseHandler(IResponseHandler responseHandler);
}
