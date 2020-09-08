package top.codings.websiphon.light.requester.support;

import com.alibaba.fastjson.JSON;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.bean.DataStat;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StatRequester extends CombineRequester<IRequest> {
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
    public void init(ICrawler crawler) throws Exception {
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
        super.init(crawler);
    }

    @Override
    public void close() throws Exception {
        if (null != exe) {
            exe.shutdownNow();
            try {
                exe.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
        super.close();
    }

    @Override
    public CompletableFuture<IRequest> execute(IRequest request) {
        return super.execute(request).whenCompleteAsync((req, throwable) -> {
            dataStat.getRequestCountTotal().increment();
            if (req.getRequestResult().isSucceed()) {
                dataStat.getNetworkRequestSuccessCountTotal().increment();
            }
        });
    }

}
