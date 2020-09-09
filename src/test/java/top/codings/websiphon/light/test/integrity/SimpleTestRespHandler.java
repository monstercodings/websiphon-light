package top.codings.websiphon.light.test.integrity;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.function.handler.AbstractResponseHandler;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.requester.IRequest;

@Slf4j
@Shared
public class SimpleTestRespHandler extends AbstractResponseHandler {
    @Override
    public void handle(IRequest request) {
        log.debug("爬取完成 -> {}", request.getRequestResult().isSucceed());
    }
}
