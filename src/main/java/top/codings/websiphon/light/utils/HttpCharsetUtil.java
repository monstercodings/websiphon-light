package top.codings.websiphon.light.utils;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.Charset;

public class HttpCharsetUtil {

    public static Charset findCharset(byte[] bytes) {
        return findCharset(new String(bytes));
    }

    public static Charset findCharset(String text) {
        try {
            Document document = Jsoup.parse(text);
            Elements metas = document.select("meta");
            for (Element meta : metas) {
                String encoding = meta.attr("charset");
                if (StringUtils.isNotBlank(encoding)) {
                    return Charset.forName(encoding.toLowerCase());
                }
                String content = meta.attr("content");
                if (StringUtils.isNotBlank(content) && content.contains("charset")) {
                    return Charset.forName(
                            content.substring(content.indexOf("charset=") + "charset=".length()).toLowerCase()
                    );
                }
            }
            return Charset.defaultCharset();
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }
}
