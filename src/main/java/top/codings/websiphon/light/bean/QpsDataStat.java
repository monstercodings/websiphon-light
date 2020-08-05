package top.codings.websiphon.light.bean;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Getter
public class QpsDataStat extends DataStat<Map<String, Object>> {
    /**
     * 发送到kafka到文章数总计
     */
    protected LongAdder sendToKafkaCountTotal = new LongAdder();
    /**
     * 上次记录时发送到kafka到文章数总计
     */
    protected long sendToKafkaCountLast;
    protected LongAdder sendToKafkaSucceedTotal = new LongAdder();
    protected long sendToKafkaSucceedLast;

    public QpsDataStat(int refreshTimestamp) {
        super(refreshTimestamp);
    }

    @Override
    public Map<String, Object> output() {
        long rct = requestCountTotal.sum();
        long rcl = requestCountLast;
        float reqQps = (rct - rcl) * 1.0f / (refreshTimestamp / 1000f);
        long respct = responseCountTotal.sum();
        long respcl = responseCountLast;
        float respQps = (respct - respcl) * 1.0f / (refreshTimestamp / 1000f);
        long reqSuccessCountTotal = networkRequestSuccessCountTotal.sum();
        float successPercent = reqSuccessCountTotal * 100f / (rct == 0 ? 1 : rct);
        long stkct = sendToKafkaCountTotal.sum();
        long stkcl = sendToKafkaCountLast;
        float sendToKafkaQps = (stkct - stkcl) * 1f / (refreshTimestamp / 1000f);
        long stkst = sendToKafkaSucceedTotal.sum();
        Map<String, Object> map = new HashMap<>();
        map.put("请求数总计", rct);
        map.put("请求QPS", reqQps + "/s");
        map.put("处理QPS", respQps + "/s");
        map.put("请求成功总计", reqSuccessCountTotal);
        map.put("已处理的响应总计", respct);
        map.put("成功率", successPercent + "%");
        map.put("推送至队列数总计", stkct);
        map.put("推送至队列成功总计", stkst);
        map.put("推送至队列QPS", sendToKafkaQps + "/s");
        return map;
    }

    @Override
    public void refresh() {
        super.refresh();
        sendToKafkaCountLast = sendToKafkaCountTotal.sum();
        sendToKafkaSucceedLast = sendToKafkaSucceedTotal.sum();
    }
}