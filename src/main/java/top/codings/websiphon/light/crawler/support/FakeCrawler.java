package top.codings.websiphon.light.crawler.support;

import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ReplaceableCrawler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.FakeRequester;

public class FakeCrawler extends CombineCrawler implements ReplaceableCrawler {
    @Override
    protected void doProxy() {
        IRequester requester = getRequester();
        if (!(requester instanceof AsyncRequester)) {
            throw new RuntimeException("伪装头请求器必须依赖异步请求器");
        }
        setRequester(new FakeRequester(getRequester()));
    }
}
