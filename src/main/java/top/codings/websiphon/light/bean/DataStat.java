package top.codings.websiphon.light.bean;

import lombok.Getter;

import java.util.concurrent.atomic.LongAdder;

/**
 * 用于具备统计功能的爬虫
 */
@Getter
public abstract class DataStat<R> {
    /**
     * 请求数总计
     */
    protected LongAdder requestCountTotal = new LongAdder();
    /**
     * 最后记录时的数量
     */
    protected long requestCountLast;
    /**
     * 已处理的响应总计
     */
    protected LongAdder responseCountTotal = new LongAdder();
    /**
     * 最后记录时的处理响应数
     */
    protected long responseCountLast;
    /**
     * 网络请求成功数总计
     */
    protected LongAdder networkRequestSuccessCountTotal = new LongAdder();
    /**
     * 最后记录的时间戳
     */
    protected long lastRecordTimestamp;
    /**
     * 刷新时间
     */
    protected int refreshTimestamp;

    public DataStat(int refreshTimestamp) {
        this.refreshTimestamp = refreshTimestamp;
    }

    public void refresh() {
        lastRecordTimestamp = System.currentTimeMillis();
        requestCountLast = requestCountTotal.sum();
        responseCountLast = responseCountTotal.sum();
    }

    public abstract R output();
}
