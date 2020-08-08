package top.codings.websiphon.light.requester.support;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.Getter;
import top.codings.websiphon.light.function.IFilter;
import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.SyncRequester;

import java.net.http.HttpRequest;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

public class DistinctRequester extends CombineRequester<IRequest> implements AsyncRequester<IRequest>, SyncRequester<IRequest> {
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
                BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), maxCount, fpp);

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
    public CompletableFuture<IRequest> executeAsync(IRequest request) {
        if (request.getHttpRequest() instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) request.getHttpRequest();
            if (filter.put(httpRequest.uri().toString())) {
                return requester.executeAsync(request);
            }
        }
        return CompletableFuture.completedFuture(request);
    }

    @Override
    public void setResponseHandler(IResponseHandler responseHandler) {
        ((SyncRequester) requester).setResponseHandler(responseHandler);
    }

    @Override
    public void setResponseHandler(QueueResponseHandler responseHandler) {
        ((AsyncRequester) requester).setResponseHandler(responseHandler);
    }
}
