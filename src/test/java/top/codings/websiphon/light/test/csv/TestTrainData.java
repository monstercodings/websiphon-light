package top.codings.websiphon.light.test.csv;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.config.RequesterConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.BaseCrawler;
import top.codings.websiphon.light.crawler.support.FakeCrawler;
import top.codings.websiphon.light.crawler.support.RateLimitCrawler;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.CombineRequester;
import top.codings.websiphon.light.requester.support.NettyRequest;
import top.codings.websiphon.light.requester.support.NettyRequester;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public class TestTrainData {
    public final static Map<Integer, String> INTEGER_STRING_MAP = new ConcurrentHashMap<>();
    private static Map<String, Collection<TrainData>> map;

    static {
        INTEGER_STRING_MAP.put(1, "正面");
        INTEGER_STRING_MAP.put(0, "中立");
        INTEGER_STRING_MAP.put(-1, "负面");
        /*INTEGER_STRING_MAP.put(0, "正面");
        INTEGER_STRING_MAP.put(1, "中立");
        INTEGER_STRING_MAP.put(2, "负面");*/
    }

    public static void main(String[] args) throws Exception {
        String newApiResult = handleData(new File("config/result_new_model.txt"));
        String oldApiResult = handleData(new File("config/result_old_model.txt"));
        String paddleApiResult = handleData(new File("config/result_paddle_model.txt"));
        String baiduApiResult = handleData(new File("config/result_baidu_model.txt"));
        String s = String.format("\n情感模型 - 新版预测结果\n%s\n" +
                        "----------------------------------------" +
                        "\n情感模型 - 旧版预测结果\n%s\n" +
                        "----------------------------------------" +
                        "\n情感模型 - Paddle版预测结果\n%s\n" +
                        "----------------------------------------" +
                        "\n情感模型 - Baidu版预测结果\n%s\n",
                newApiResult, oldApiResult, paddleApiResult, baiduApiResult);
        System.out.println(s);
//        System.out.println(newApiResult);
//        predictionData("result_baidu_model", "http://124.88.116.204:10052/article/nlp/baidu/emotion");
//        predictionData("result_old_model", "http://124.88.116.204:10052/article/nlp/old/emotion");
//        predictionData("result_paddle_model", "http://121.201.107.77:61002/predict/xunchaduan_sentiment");
    }

    private static String handleData(File file) throws IOException {
        map = JSONObject.parseObject(
                FileUtils.readFileToString(file, "utf-8"),
                new TypeReference<Map<String, Collection<TrainData>>>() {
                });
        Map<String, EmotionStat> statMap = new ConcurrentHashMap<>();
        map.keySet().parallelStream().forEach(key -> statMap.put(key, new EmotionStat(key)));
        map.entrySet()
                .parallelStream()
                .forEach(entry -> entry.getValue().parallelStream()
                        .forEach(trainData -> {
                            String emotion = trainData.getEmotion();
                            String prediction = trainData.getPrediction();

                            statMap.values().parallelStream()
                                    .forEach(emotionStat -> {
                                        if (emotion.equals(emotionStat.getEmotion())) {
                                            if (emotion.equals(prediction)) {
                                                emotionStat.getTpAdder().increment();
                                            } else {
                                                emotionStat.getFnAdder().increment();
                                            }
                                            return;
                                        } else {
                                            if (prediction.equals(emotionStat.getEmotion())) {
                                                emotionStat.getFpAdder().increment();
                                            } else {
                                                emotionStat.getTnAdder().increment();
                                            }
                                        }
                                    });
                        }));
        StringBuffer sb = new StringBuffer();
        LongAdder tpAdder = new LongAdder();
        LongAdder fpAdder = new LongAdder();
        LongAdder tnAdder = new LongAdder();
        LongAdder fnAdder = new LongAdder();
        statMap.entrySet().parallelStream().forEach(entry -> {
            EmotionStat stat = entry.getValue();
            stat.finish();
            tpAdder.add(stat.getTp());
            fpAdder.add(stat.getFp());
            tnAdder.add(stat.getTn());
            fnAdder.add(stat.getFn());
            sb.append(
                    String.format(
                            "[%s] F1:%.2f | 精准率:%.2f%% | 召回率:%.2f%%\n",
                            stat.getEmotion(),
                            stat.getF1(),
                            stat.getP() * 100,
                            stat.getR() * 100
                    )
            );
        });
        float p = tpAdder.sum() * 1f / (tpAdder.sum() + fpAdder.sum());
        float r = tpAdder.sum() * 1f / (tpAdder.sum() + fnAdder.sum());
        float f1 = 2f / (1f / p + 1f / r);
        sb.append(
                String.format(
                        "[%s] F1:%.2f | 精准率:%.2f%% | 召回率:%.2f%%",
                        "总体",
                        f1,
                        p * 100,
                        r * 100
                )
        );
        return sb.toString();
    }

    private static void predictionData(String name, String url) throws ExecutionException, InterruptedException {
        map = readData();
        ICrawler crawler = createCrawler(name, new TrainDataResponseHandler());
        map.entrySet()
                .parallelStream()
                .forEach(stringCollectionEntry -> stringCollectionEntry.getValue().parallelStream()
                        .forEach(trainData -> crawler.push(createRequest(trainData, url))));
    }

    private static ICrawler createCrawler(String name, IResponseHandler responseHandler) throws InterruptedException, ExecutionException {
        CombineRequester requester = new NettyRequester(RequesterConfig.builder()
                .connectTimeoutMillis(30000)
                .ignoreSslError(true)
                .maxContentLength(Integer.MAX_VALUE)
                .idleTimeMillis(30000)
                .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                .build());
        ICrawler crawler = new BaseCrawler(
                CrawlerConfig.builder()
                        .name(name)
                        .shutdownHook(c -> finish(c.config().getName()))
                        .build(),
                responseHandler,
                requester
        )
                .wrapBy(new FakeCrawler())
//                .wrapBy(new FiltrateCrawler())
                .wrapBy(new RateLimitCrawler(1, 30000, 0f, (request, c) -> {
                    log.debug("请求任务超时 -> {}", request.getUri().toString());
                }));
        crawler.startup().whenCompleteAsync((c, throwable) -> {
            if (throwable != null) {
                log.error("启动爬虫失败", throwable);
                return;
            }
        }).get();
        return crawler;
    }

    @SneakyThrows
    private static IRequest createRequest(TrainData trainData, String url) {
        Map<String, Object> map = new HashMap<>();
        map.put("content", trainData.getContent());
//        map.put("data", Arrays.asList(Arrays.asList(trainData.getContent())));
        byte[] bytes = JSON.toJSONString(map).getBytes("utf-8");
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
//                "http://124.88.116.204:10052/article/nlp/old/emotion",
//                "http://14.21.37.151:10053/firm/predict",
                url,
                Unpooled.wrappedBuffer(bytes));
        request.headers()
                .set("Content-Type", "application/json;charset=UTF-8")
                .set("content-length", bytes.length)
        ;
        NettyRequest nettyRequest = new NettyRequest(request);
        nettyRequest.setUserData(trainData);
//        TimeUnit.MILLISECONDS.sleep(500);
        return nettyRequest;
    }

    @SneakyThrows
    private static Map<String, Collection<TrainData>> readData() {
        List<String> lines = FileUtils.readLines(new File("/Users/hj/Documents/project/python/company/kedun/python-spider/data/test_data.csv"), "utf-8");
        Map<String, Collection<TrainData>> map = new ConcurrentHashMap<>();
        lines.parallelStream()
                .forEach(s -> {
                    String[] strings = s.split("\t");
                    String emotion = strings[0];
                    String content = strings[1];
                    Collection<TrainData> collection;
                    if ((collection = map.get(emotion)) == null) {
                        synchronized (map) {
                            if ((collection = map.get(emotion)) == null) {
                                collection = Collections.newSetFromMap(new ConcurrentHashMap<>());
                                map.put(emotion, collection);
                            }
                        }
                    }
                    collection.add(new TrainData(emotion, content));
                });
        return map;
    }

    public static void finish(String name) {
        try {
            FileUtils.writeStringToFile(new File("config/" + name + ".txt"), JSON.toJSONString(map, true), "utf-8");
        } catch (IOException e) {
            log.error("写入文件异常");
            return;
        }
        log.info("任务完成");
    }
}
