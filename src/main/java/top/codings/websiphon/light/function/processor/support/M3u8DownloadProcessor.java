package top.codings.websiphon.light.function.processor.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import top.codings.websiphon.light.bean.M3u8;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.loader.bean.PluginType;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.utils.HttpPathUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Shared
@PluginDefinition(
        name = "m3u8视频下载处理器",
        description = "用于下载m3u8类型的视频",
        type = PluginType.PROCESSOR
)
public class M3u8DownloadProcessor extends AbstractProcessor<byte[]> {
    private static final ContentType CONTENT_TYPE = ContentType.parse("application/x-mpegURL");
    private String basePath;
    private boolean localStorage;
    private boolean downloadConcurrency;
    private Class<? extends UserDataStack> stackClass;

    public M3u8DownloadProcessor() {
        this(null, false);
    }

    public M3u8DownloadProcessor(String basePath) {
        this(basePath, false);
    }

    public M3u8DownloadProcessor(boolean downloadConcurrency) {
        this(null, downloadConcurrency);
    }

    public M3u8DownloadProcessor(String basePath, boolean downloadConcurrency) {
        if (StringUtils.isNotBlank(basePath)) {
            localStorage = true;
        }
        this.basePath = basePath;
        this.downloadConcurrency = downloadConcurrency;
    }

    @Override
    protected void init(ICrawler crawler, int index) throws Exception {
        if (index == 0) {
            stackClass = getStackClass();
        }
    }

    protected Class<? extends UserDataStack> getStackClass() {
        return UserDataStack.class;
    }

    @Override
    protected Object process0(byte[] data, IRequest request, ICrawler crawler) throws Exception {
        Object userData = request.getUserData();
        if (
                !CONTENT_TYPE.getMimeType().equals(request.getContentType().getMimeType()) &&
                        !(userData instanceof UserDataStack)
        ) {
            return data;
        }
        int code = request.getRequestResult().getCode();
        if (code < 200 || code >= 300) {
            if (userData instanceof UserDataStack) {
                ((UserDataStack) userData).finish.set(true);
                request.setUserData(((UserDataStack) userData).getUserData());
            }
            return data;
        }
        UserDataStack stack;
        if (userData instanceof UserDataStack) {
            stack = (UserDataStack) userData;
        } else {
            if (stackClass != null) {
                stack = stackClass.getConstructor().newInstance();
            } else {
                stack = new UserDataStack();
            }
            stack.userData = userData;
            request.setUserData(stack);
        }
        switch (stack.stage) {
            case 0:
                stack.stage++;
                stageOne(data, request, crawler);
                break;
            case 1:
                stack.stage++;
                stageSecond(data, request, crawler);
                break;
            case 2:
                return stageThird(data, request, crawler);
            default:
                return data;
        }
        return null;
    }

    @Override
    protected boolean isMatchHandleError(IRequest request, Throwable throwable) {
        return request.getUserData() instanceof UserDataStack;
    }

    @Override
    protected void whenError(Throwable throwable, IRequest request, ICrawler crawler) throws Exception {
        if (request.getUserData() instanceof UserDataStack) {
            request.setUserData(((UserDataStack) request.getUserData()).userData);
        }
    }

    private void stageOne(byte[] data, IRequest request, ICrawler crawler) {
        List<ExtXStreamInf> list = parseExtXStreamInf(data);
        if (list.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("解析m3u8的分片头文件失败");
            }
            return;
        }
        ExtXStreamInf extXStreamInf = list.get(0);
        HttpPathUtil.absolutely(extXStreamInf.path, request.getUri()).ifPresent(s -> {
            if (log.isTraceEnabled()) {
                log.trace("Segment's URL -> {}", s);
            }
            crawler.push(s, request.getProxy(), request.getUserData());
        });
    }

    private void stageSecond(byte[] data, IRequest request, ICrawler crawler) throws Exception {
        List<ExtInf> extInfs = parseExtInf(data);
        if (extInfs.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("解析m3u8的各分片文件URL失败");
            }
            return;
        }
        UserDataStack stack = (UserDataStack) request.getUserData();
        // 根据分片数量创建对应的内容槽
        stack.segments = new byte[extInfs.size()][];
        LongAdder adder = new LongAdder();
        List<Map<String, Object>> waits = new LinkedList<>();
        for (int i = 0; i < extInfs.size(); i++) {
            ExtInf extInf = extInfs.get(i);
            int finalI = i;
            HttpPathUtil.absolutely(extInf.path, request.getUri()).ifPresent(s -> {
                if (log.isTraceEnabled()) {
                    log.trace("m3u8分片URL -> {}", s);
                }
                waits.add(Map.of(
                        "url", s,
                        "count", extInfs.size(),
                        "index", finalI,
                        "adder", adder
                ));
            });
        }
        if (downloadConcurrency) {
            waits.parallelStream().forEach(som -> {
                UserDataStack copy = copyUserDataStack(stack, som);
                crawler.push(som.get("url").toString(), request.getProxy(), copy);
            });
        } else {
            stack.queue = new ArrayDeque(waits);
            Map<String, Object> som = stack.queue.poll();
            UserDataStack copy = copyUserDataStack(stack, som);
            crawler.push(som.get("url").toString(), request.getProxy(), copy);
        }
    }

    private M3u8 stageThird(byte[] data, IRequest request, ICrawler crawler) throws Exception {
        UserDataStack stack = (UserDataStack) request.getUserData();
        // 把数据放到对应的槽位上
        stack.segments[(Integer) stack.params.get("index")] = data;
        LongAdder adder = (LongAdder) stack.params.get("adder");
        // 将已下载数+1
        adder.increment();
        if (stack.queue != null && !stack.queue.isEmpty()) {
            Map<String, Object> som = stack.queue.poll();
            UserDataStack copy = copyUserDataStack(stack, som);
            crawler.push(som.get("url").toString(), request.getProxy(), copy);
            return null;
        }
        int count = (int) stack.params.get("count");
        if (adder.sum() != count) {
            return null;
        }
        if (!stack.finish.compareAndSet(false, true)) {
            return null;
        }
        byte[] content = new byte[0];
        for (byte[] segment : stack.segments) {
            byte[] bytes = new byte[content.length + segment.length];
            System.arraycopy(content, 0, bytes, 0, content.length);
            System.arraycopy(segment, 0, bytes, content.length, segment.length);
            content = bytes;
        }
        if (localStorage) {
            File file = Path.of(basePath, UUID.randomUUID().toString().replace("-", "") + ".mp4").toFile();
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileUtils.writeByteArrayToFile(file, content);
            return null;
        }
        request.setUserData(stack.userData);
        return M3u8.builder()
                .content(content)
                .build();
    }

    private UserDataStack copyUserDataStack(UserDataStack stack, Map<String, Object> som) {
        UserDataStack copy;
        try {
            copy = stackClass.getConstructor().newInstance();
        } catch (Exception e) {
            log.error("拷贝m3u8内部数据失败");
            copy = new UserDataStack();
        }
        copy.userData = stack.userData;
        copy.stage = stack.stage;
        copy.segments = stack.segments;
        copy.queue = stack.queue;
        copy.params = som;
        copy.copy(stack);
        return copy;
    }

    private List<ExtXStreamInf> parseExtXStreamInf(byte[] bytes) {
        String content = new String(bytes);
        if (StringUtils.isBlank(content) || !content.startsWith("#EXTM3U")) {
            return Collections.emptyList();
        }
        List<ExtXStreamInf> list = new LinkedList<>();
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String string = lines[i].replace("\r", "");
            if (string.startsWith("#EXT-X-STREAM-INF")) {
                i++;
                ExtXStreamInf extXStreamInf = new ExtXStreamInf();
                extXStreamInf.path = lines[i];
                String[] infos = string.replace("#EXT-X-STREAM-INF:", "").split(",");
                for (String info : infos) {
                    String[] ss = info.split("=");
                    switch (ss[0].toUpperCase()) {
                        case "BANDWIDTH":
                            extXStreamInf.bandwidth = Integer.parseInt(ss[1]);
                            break;
                        case "PROGRAM-ID":
                            extXStreamInf.programId = Integer.parseInt(ss[1]);
                            break;
                        case "RESOLUTION":
                            extXStreamInf.resolution = ss[1];
                            break;
                        case "CODECS":
                            extXStreamInf.codecs = ss[1].replace("\"", "");
                            break;
                        default:
                            break;
                    }
                }
                list.add(extXStreamInf);
            }
        }
        list.sort((o1, o2) -> o2.bandwidth - o1.bandwidth);
        return list;
    }

    private List<ExtInf> parseExtInf(byte[] bytes) {
        String content = new String(bytes);
        if (StringUtils.isBlank(content) || !content.startsWith("#EXTM3U")) {
            return Collections.emptyList();
        }
        String[] lines = content.split("\n");
        List<ExtInf> list = new ArrayList<>(lines.length / 2 + 1);
        for (int i = 0; i < lines.length; i++) {
            ExtInf extInf = new ExtInf();
            String line = lines[i].replace("\r", "");
            if (line.startsWith("#EXTINF")) {
                i++;
                extInf.properties = line.replace("#EXTINF:", "").split(",");
                extInf.path = lines[i];
                list.add(extInf);
            }
        }
        return list;
    }

    protected static class ExtXStreamInf {
        private int programId;
        private int bandwidth;
        private String resolution;
        private String codecs;
        private String path;
    }

    protected static class ExtInf {
        private String[] properties;
        private String path;
    }

    @NoArgsConstructor
    protected static class UserDataStack {
        @Getter
        @Setter
        private Object userData;
        private Queue<Map<String, Object>> queue;
        @Getter
        private int stage;
        @Getter
        private byte[][] segments;
        @Getter
        private Map<String, Object> params;
        private AtomicBoolean finish = new AtomicBoolean(false);

        protected void copy(UserDataStack old) {
            // 留给继承该类的子类实现的回调方法
        }
    }
}
