package top.codings.websiphon.light.requester.support;

import com.alibaba.fastjson.JSON;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.bean.DataStat;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StatRequester extends CombineRequester<IRequest> implements AsyncRequester<IRequest> {
    private final static String NAME = "统计请求器";
    @Setter
    private boolean debug;
    private DataStat dataStat;
    private ExecutorService exe;

    public StatRequester(CombineRequester requester, DataStat dataStat) {
        this(requester, dataStat, false);
    }

    public StatRequester(CombineRequester requester, DataStat dataStat, boolean debug) {
        super(requester);
        this.dataStat = dataStat;
        this.debug = debug;
    }

    @Override
    public CompletableFuture<IRequester> init() {
        CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> {
            if (dataStat.getRefreshTimestamp() > 0) {
                exe = Executors.newSingleThreadExecutor(new DefaultThreadFactory(NAME));
                exe.submit(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(dataStat.getRefreshTimestamp());
                            if (debug) {
                                log.info("\n{}", JSON.toJSONString(dataStat.output(), true));
                            }
                            dataStat.refresh();
                        } catch (InterruptedException e) {
                            return;
                        } catch (Exception e) {
                            log.error("爬虫统计异常", e);
                        }
                    }
                });
            }
            return this;
        });
        return completableFuture.thenCombineAsync(super.init(), (o, o2) -> o2);
    }

    @Override
    public CompletableFuture<IRequester> shutdown(boolean force) {
        if (null != exe) {
            if (force) exe.shutdownNow();
            else exe.shutdown();
            try {
                exe.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
            }
        }
        return super.shutdown(force);
    }

    @Override
    public CompletableFuture<IRequest> executeAsync(IRequest request) {
        return requester.executeAsync(request).whenCompleteAsync((req, throwable) -> {
            dataStat.getRequestCountTotal().increment();
            if (req.getRequestResult().isSucceed()) {
                dataStat.getNetworkRequestSuccessCountTotal().increment();
            }
        });
    }

    @Override
    public void setResponseHandler(QueueResponseHandler responseHandler) {
        ((AsyncRequester) requester).setResponseHandler(responseHandler);
    }
}
