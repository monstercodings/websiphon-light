package top.codings.websiphon.light.function.filter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.Charset;

public class ArticleFilter implements IFilter<String, Boolean> {
    private BloomFilter<String> bloomFilter;

    public ArticleFilter() {
        this(null);
    }

    public ArticleFilter(int maxCount, double fpp) {
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), maxCount, fpp);
    }

    public ArticleFilter(BloomFilter<String> bloomFilter) {
        if (null == bloomFilter) {
            bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), 200000000, 0.001d);
        }
        this.bloomFilter = bloomFilter;
    }

    @Override
    public Boolean put(String s) {
        return bloomFilter.put(s);
    }

    @Override
    public Boolean contain(String s) {
        return bloomFilter.mightContain(s);
    }
}
