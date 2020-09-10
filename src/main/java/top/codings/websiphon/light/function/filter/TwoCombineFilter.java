package top.codings.websiphon.light.function.filter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.StandardCharsets;

public class TwoCombineFilter implements CleanableFilter<String, Boolean> {
    private int maxCount = 200000000;
    private double fpp = 0.001d;
    private BloomFilter<String> localFilter;
    private IFilter<String, Boolean> outerFilter;

    public TwoCombineFilter(IFilter<String, Boolean> outerFilter) {
        this.outerFilter = outerFilter;
        localFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), maxCount, fpp);
    }

    public TwoCombineFilter(int maxCount, double fpp, IFilter<String, Boolean> outerFilter) {
        this.maxCount = maxCount;
        this.fpp = fpp;
        this.outerFilter = outerFilter;
        localFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), maxCount, fpp);
    }

    @Override
    public Boolean put(String s) {
        if (outerFilter.contain(s)) {
            return false;
        }
        return localFilter.put(s);
    }

    @Override
    public Boolean contain(String s) {
        if (outerFilter.contain(s)) {
            return true;
        }
        return localFilter.mightContain(s);
    }

    @Override
    public void clear() {
        localFilter = null;
        localFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), maxCount, fpp);
    }

    /**
     * 获取被组合的过滤器
     *
     * @return
     */
    public IFilter<String, Boolean> target() {
        return outerFilter;
    }
}
