package top.codings.websiphon.light.requester.support;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.requester.IRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class FakeRequester extends CombineRequester<IRequest> {
    private Map<String, Object> innerHeaders;

    public FakeRequester(CombineRequester requester) {
        this(requester, null);
    }

    public FakeRequester(CombineRequester requester, Map<String, Object> innerHeaders) {
        super(requester);
        if (null == innerHeaders) {
            innerHeaders = new HashMap<>();
            innerHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            innerHeaders.put("Accept-Encoding", "gzip, deflate, compress");
            innerHeaders.put("Accept-Language", "zh-CN,zh;q=0.9");
            innerHeaders.put("Cache-Control", "no-cache");
            innerHeaders.put("Connection", "keep-alive");
            innerHeaders.put("DNT", "1");
            innerHeaders.put("Pragma", "no-cache");
            innerHeaders.put("Upgrade-Insecure-Requests", "1");
            innerHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
        }
        this.innerHeaders = innerHeaders;
    }

    @Override
    public CompletableFuture<IRequest> execute(IRequest request) {
        request.setHeaders(innerHeaders);
        return super.execute(request);
    }
}
