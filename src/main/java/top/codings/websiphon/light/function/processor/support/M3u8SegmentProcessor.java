package top.codings.websiphon.light.function.processor.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.bean.PluginType;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.utils.HttpPathUtil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@PluginDefinition(
        name = "m3u8视频分片信息连接提取处理器",
        description = "用于提取m3u8视频分片信息URL的处理器，必须配合M3u8DownloadProcessor以及M3u8ExtInfProcessor进行食用",
        type = PluginType.PROCESSOR
)
public class M3u8SegmentProcessor extends AbstractProcessor<byte[]> {
    @Override
    protected Object process0(byte[] data, IRequest request, ICrawler crawler) throws Exception {
        List<ExtXStreamInf> list = parseExtXStreamInf(data);
        if (!list.isEmpty()) {
            ExtXStreamInf extXStreamInf = list.get(0);
            HttpPathUtil.absolutely(extXStreamInf.path, request.getUri()).ifPresent(s -> {
                if (log.isTraceEnabled()) {
                    log.trace("Segment's URL -> {}", s);
                }
                crawler.push(s, request.getProxy());
            });
            return null;
        }
        return data;
    }

    private List<ExtXStreamInf> parseExtXStreamInf(byte[] bytes) {
        try {
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
                        }
                    }
                    list.add(extXStreamInf);
                }
            }
            list.sort((o1, o2) -> o2.bandwidth - o1.bandwidth);
            return list;
        } catch (Exception e) {
            log.error("m3u8解析分片信息地址失败", e);
            return Collections.emptyList();
        }
    }

    private static class ExtXStreamInf {
        private int programId;
        private int bandwidth;
        private String resolution;
        private String codecs;
        private String path;
    }

}
