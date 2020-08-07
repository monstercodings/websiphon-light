package top.codings.websiphon.light.requester.support;

import com.alibaba.fastjson.JSON;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.bean.DataStat;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StatRequester extends CombineRequester implements AsyncRequester {
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
    public void init() {
        if (dataStat.getRefreshTimestamp() > 0) {
            exe = Executors.newSingleThreadExecutor();
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
        super.init();
    }

    @Override
    public void shutdown(boolean force) {
        if (null != exe) {
            if (force) exe.shutdownNow();
            else exe.shutdown();
        }
        super.shutdown(force);
    }

    @Override
    public CompletableFuture<BuiltinRequest> executeAsync(BuiltinRequest request) {
        dataStat.getRequestCountTotal().increment();
        return requester.executeAsync(request).whenComplete((builtinRequest, throwable) -> {
            if (builtinRequest.requestResult.succeed) {
                dataStat.getNetworkRequestSuccessCountTotal().increment();
            }
        });
    }

    @Override
    public void setResponseHandler(QueueResponseHandler responseHandler) {
        ((AsyncRequester) requester).setResponseHandler(responseHandler);
    }
}
