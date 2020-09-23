package top.codings.websiphon.light.test.csv;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.handler.ChainResponseHandler;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.function.processor.IProcessor;
import top.codings.websiphon.light.requester.IRequest;

@Slf4j
public class TrainDataResponseHandler extends ChainResponseHandler<IRequest> {
    @Override
    protected IProcessor processorChain() {
        return new AbstractProcessor<JSONObject>() {
            @Override
            protected Object process0(JSONObject data, IRequest request, ICrawler crawler) throws Exception {
                int code = data.getIntValue("code");
                String message = data.getString("message");
                if (code != 0) {
                    log.warn("响应异常[{}] -> {}", code, message);
                    return null;
                }
                TrainData trainData = (TrainData) request.getUserData();
//                String result = TestTrainData.INTEGER_STRING_MAP.get(data.getJSONArray("results").get(0));
                String result = TestTrainData.INTEGER_STRING_MAP.get(data.getInteger("data"));
                trainData.setPrediction(result);
                log.debug("预测完成 -> {} | {}", trainData.getEmotion(), trainData.getPrediction());
                return null;
            }
        };
    }

    @Override
    protected void handleError(IRequest request, Throwable throwable, ICrawler crawler) {
        log.error("请求失败", throwable);
    }

    @Override
    public void finish(ICrawler crawler) {
        crawler.shutdown();
    }
}
