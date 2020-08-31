package top.codings.websiphon.light.function.processor.support;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.bean.PluginType;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.utils.HttpPathUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@AllArgsConstructor
@PluginDefinition(
        name = "m3u8视频分片URL提取处理器",
        description = "用于提取m3u8视频每个分片的URL，必须配合M3u8DownloadProcessor以及M3u8SegmentProcessor进行食用",
        type = PluginType.PROCESSOR
)
public class M3u8ExtInfProcessor extends AbstractProcessor<byte[]> {
    private String basePath;

    @Override
    protected Object process0(byte[] data, IRequest request, ICrawler crawler) throws Exception {
        List<ExtInf> list = parseExtInf(data);
        if (list.isEmpty()) {
            return data;
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        File parent = Path.of(basePath, uuid).toFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        Lock lock = new ReentrantLock();
        for (int i = 0; i < list.size(); i++) {
            ExtInf extInf = list.get(i);
            int finalI = i;
            HttpPathUtil.absolutely(extInf.path, request.getUri()).ifPresent(s -> {
                if (log.isTraceEnabled()) {
                    log.trace("m3u8分片URL -> {}", s);
                }
                Map<String, Object> map = Map.of(
                        "file", Path.of(basePath, uuid, String.format("%06d.ts", finalI)).toFile(),
                        "count", list.size(),
                        "lock", lock,
                        "type", "m3u8"
                );
                crawler.push(s, request.getProxy(), map);
            });
        }
        return null;
    }

    private List<ExtInf> parseExtInf(byte[] bytes) {
        try {
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
        } catch (Exception e) {
            log.error("m3u8提取ts分片信息失败", e);
            return Collections.emptyList();
        }
    }

    private static class ExtInf {
        private String[] properties;
        private String path;
    }

}
