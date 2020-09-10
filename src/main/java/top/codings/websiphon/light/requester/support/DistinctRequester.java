package top.codings.websiphon.light.requester.support;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.function.filter.IFilter;
import top.codings.websiphon.light.requester.IRequest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DistinctRequester extends CombineRequester<IRequest> {
    @Getter
    private IFilter<String, Boolean> filter;

    public DistinctRequester(CombineRequester requester) {
        this(requester, null);
    }

    public DistinctRequester(CombineRequester requester, IFilter<String, Boolean> filter) {
        this(requester, filter, 200000000, 0.001d);
    }

    public DistinctRequester(CombineRequester requester, long maxCount, double fpp) {
        this(requester, null, maxCount, fpp);
    }

    private DistinctRequester(CombineRequester requester, IFilter<String, Boolean> filter, long maxCount, double fpp) {
        super(requester);
        if (null == filter) {
            this.filter = new IFilter<>() {
                BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), maxCount, fpp);

                @Override
                public Boolean put(String s) {
                    return bloomFilter.put(s);
                }

                @Override
                public Boolean contain(String s) {
                    return bloomFilter.mightContain(s);
                }
            };
        } else {
            this.filter = filter;
        }
    }

    @Override
    public CompletableFuture<IRequest> execute(IRequest request) {
        String url = request.getUri().toString();
        if (StringUtils.isNotBlank(url) && filter.put(url)) {
            return requester.execute(request);
        }
        return CompletableFuture.supplyAsync(() -> request);
    }
}
