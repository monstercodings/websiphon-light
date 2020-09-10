package top.codings.websiphon.light.crawler.support;

import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ReplaceableCrawler;
import top.codings.websiphon.light.requester.support.FakeRequester;

import java.util.Map;

public class FakeCrawler extends CombineCrawler implements ReplaceableCrawler {
    private Map<String, Object> builtHeaders;

    public FakeCrawler() {
    }

    public FakeCrawler(Map<String, Object> builtHeaders) {
        this.builtHeaders = builtHeaders;
    }

    @Override
    protected void doProxy() {
        setRequester(new FakeRequester(getRequester(), builtHeaders));
    }
}
