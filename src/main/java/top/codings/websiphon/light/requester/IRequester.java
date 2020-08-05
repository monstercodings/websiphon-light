package top.codings.websiphon.light.requester;

import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.support.BuiltinRequest;
import top.codings.websiphon.light.requester.support.BuiltinRequester;

import java.util.concurrent.CompletableFuture;

public interface IRequester {
    /**
     * 初始化请求器
     */
    void init();

    /**
     * 发起异步网络请求
     *
     * @param request 请求对象
     * @return
     */
    CompletableFuture<BuiltinRequest> executeAsync(BuiltinRequest request);

    /**
     * 关闭请求器
     *
     * @param force
     */
    void shutdown(boolean force);

    /**
     * 查看请求器是否繁忙
     *
     * @return
     */
    boolean isBusy();

    IResponseHandler getResponseHandler();

    static RequesterBuilder newBuilder(boolean sync) {
        return new RequesterBuilder(sync);
    }

    class RequesterBuilder {
        private IRequester requester;
        private boolean sync;

        public RequesterBuilder(boolean sync) {
            this.sync = sync;
            if (sync) {
                throw new RuntimeException("同步请求器暂未支持");
            } else {
                requester = new BuiltinRequester();
            }
        }

        public RequesterBuilder responseHandler(IResponseHandler responseHandler) {
            if (sync) {
                SyncRequester syncRequester = (SyncRequester) requester;
                syncRequester.setResponseHandler(responseHandler);
            } else {
                AsyncRequester asyncRequester = (AsyncRequester) requester;
                asyncRequester.setResponseHandler((QueueResponseHandler) responseHandler);
            }
            return this;
        }

        public IRequester build() {
            return requester;
        }
    }
}
