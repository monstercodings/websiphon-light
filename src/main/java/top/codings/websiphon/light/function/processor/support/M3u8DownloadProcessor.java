package top.codings.websiphon.light.function.processor.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.bean.PluginType;
import top.codings.websiphon.light.requester.IRequest;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.locks.Lock;

@Slf4j
@PluginDefinition(
        name = "m3u8视频流下载处理器",
        description = "用于下载m3u8视频分片的处理器，必须配合M3u8SegmentProcessor以及M3u8ExtInfProcessor进行食用",
        type = PluginType.PROCESSOR
)
public class M3u8DownloadProcessor extends AbstractProcessor<byte[]> {
    @Override
    protected Object process0(byte[] data, IRequest request, ICrawler crawler) throws Exception {
        Map<String, Object> map = (Map<String, Object>) request.getUserData();
        if (map == null || !"m3u8".equals(map.get("type"))) {
            return data;
        }
        File file = (File) map.get("file");
        int recordCount = (int) map.get("count");
        Lock lock = (Lock) map.get("lock");
        FileUtils.writeByteArrayToFile(file, data);
        int tsFileCount = FileUtils.listFiles(file.getParentFile(), new String[]{"ts"}, true).size();
        if (recordCount != tsFileCount || !lock.tryLock()) {
            return null;
        }
        try {
            tsFileCount = FileUtils.listFiles(file.getParentFile(), new String[]{"ts"}, true).size();
            if (recordCount != tsFileCount) {
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("ts文件已全部下载完成");
            }
            File save = Path.of(file.getParentFile().getPath(), file.getParentFile().getName() + ".mp4").toFile();
            File[] files = FileUtils.listFiles(file.getParentFile(), new String[]{"ts"}, true).toArray(File[]::new);
            Arrays.parallelSort(files, Comparator.comparing(File::getName));
            for (File f : files) {
                FileUtils.writeByteArrayToFile(save, FileUtils.readFileToByteArray(f), true);
                f.delete();
            }
            if (log.isDebugEnabled()) {
                log.debug("ts文件已合成为mp4文件 -> {}", save.getAbsolutePath());
            }
        } finally {
            lock.unlock();
        }
        return null;
    }
}
