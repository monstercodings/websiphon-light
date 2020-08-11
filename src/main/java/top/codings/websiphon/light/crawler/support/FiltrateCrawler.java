package top.codings.websiphon.light.crawler.support;

import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.FilterableCrawler;
import top.codings.websiphon.light.function.filter.ArticleFilter;
import top.codings.websiphon.light.function.filter.CleanableFilter;
import top.codings.websiphon.light.function.filter.TwoCombineFilter;
import top.codings.websiphon.light.function.filter.IFilter;
import top.codings.websiphon.light.requester.support.DistinctRequester;

public class FiltrateCrawler extends CombineCrawler implements FilterableCrawler {
    //    private IFilter<String, Boolean> iFilter;
    private CleanableFilter cleanableFilter;

    public FiltrateCrawler() {
        this(null);
    }

    public FiltrateCrawler(IFilter<String, Boolean> iFilter) {
        if (iFilter instanceof CleanableFilter) {
            cleanableFilter = (CleanableFilter) iFilter;
        } else if (null == iFilter) {
            // 使用组合过滤器
            cleanableFilter = new TwoCombineFilter(new ArticleFilter());
        } else {
            throw new RuntimeException("去重功能爬虫暂不支持除基础和可清除之外的过滤器");
        }
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
