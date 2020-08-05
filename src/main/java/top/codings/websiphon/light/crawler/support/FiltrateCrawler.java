package top.codings.websiphon.light.crawler.support;

import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.FilterableCrawler;
import top.codings.websiphon.light.function.ArticleFilter;
import top.codings.websiphon.light.function.CleanableFilter;
import top.codings.websiphon.light.function.CombineFilter;
import top.codings.websiphon.light.function.IFilter;
import top.codings.websiphon.light.requester.support.DistinctRequester;

public class FiltrateCrawler extends CombineCrawler implements FilterableCrawler {
    private IFilter<String, Boolean> iFilter;
    private CleanableFilter cleanableFilter;

    public FiltrateCrawler() {
        // 初始化过滤器
        iFilter = new ArticleFilter();
        // 使用组合过滤器
        cleanableFilter = new CombineFilter(iFilter);
    }

    @Override
    protected void doProxy() {
        // 将原请求器代理为可过滤的请求器
        setRequester(new DistinctRequester(
                getRequester(),
                cleanableFilter
        ));
    }

    @Override
    public IFilter filter() {
        return cleanableFilter;
    }

    @Override
    public void clear() {
        cleanableFilter.clear();
    }
}
