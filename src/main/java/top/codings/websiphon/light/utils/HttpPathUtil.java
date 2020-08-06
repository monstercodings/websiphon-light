package top.codings.websiphon.light.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpPathUtil {
    private static Pattern pattern = Pattern.compile("(\\w+)://([^/:]+)(:\\d*)?([^# ]*)");

    /**
     * 将相对路径的超链接转换为绝对路径的超链接
     * http://{path}
     * https://{path}
     * //{path}
     * /{path}
     * ./{path}
     * ../{path}
     * {path}
     *
     * @param relativePath
     * @param fullPath
     * @return
     */
    public static Optional<String> absolutely(String relativePath, String fullPath) {
        return absolutely(relativePath, URI.create(fullPath));


        /*String nowDomain = fullPath;
        if (nowDomain.startsWith("http")) {
            nowDomain = nowDomain.substring(nowDomain.indexOf("//") + 2);
        }
        if (nowDomain.contains("/")) {
            nowDomain = nowDomain.substring(0, nowDomain.indexOf("/"));
        }
        if (StringUtils.isBlank(relativePath) || relativePath.startsWith("javascript")) {
            return null;
        }
        if (relativePath.startsWith("//")) {
            relativePath = fullPath.substring(0, fullPath.indexOf("//")) + relativePath;
        } else if (relativePath.startsWith("/")) {
            relativePath = fullPath.substring(0, fullPath.indexOf("//")) + "//" + nowDomain + relativePath;
        } else if (relativePath.startsWith("http")) {
        } else if (relativePath.startsWith("../")) {
            int loc = relativePath.indexOf("../");
            while (loc >= 0) {
                relativePath = relativePath.substring(3);
                loc = relativePath.indexOf("../");
                if (fullPath.endsWith("/")) {
                    fullPath = fullPath.substring(0, fullPath.length() - 1);
                }
                fullPath = fullPath.substring(0, fullPath.lastIndexOf("/") < 0 ? fullPath.length() : fullPath.lastIndexOf("/"));
            }
            relativePath = fullPath + "/" + relativePath;
        } else if (relativePath.startsWith("./")) {
            if (fullPath.endsWith("/")) {
                fullPath = fullPath.substring(0, fullPath.length() - 1);
                relativePath = fullPath + relativePath.substring(1);
            } else {
                relativePath = fullPath.substring(0, fullPath.lastIndexOf("/")) + relativePath.substring(1);
            }
//            href = realUrl.substring(0, realUrl.indexOf("//")) + "//" + nowDomain + href.substring(1);
        } else if (relativePath.startsWith(nowDomain)) {
            relativePath = fullPath.substring(0, fullPath.indexOf(":") + 1) + "//" + relativePath;
        } else {
            relativePath = fullPath.substring(0, fullPath.lastIndexOf("/") + 1) + relativePath;
        }
        return relativePath;*/
    }

    public static Optional<String> absolutely(String relativePath, URI fullPath) {
        if (StringUtils.isBlank(relativePath)) {
            return Optional.empty();
        }
        relativePath = relativePath.trim();
        if (relativePath.startsWith("http")) {
            try {
                URI absoluteUri = URI.create(relativePath);
                return Optional.ofNullable(absoluteUri.toString());
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        int port = fullPath.getPort();
        String portstr;
        if (port < 0) {
            portstr = "";
        } else {
            portstr = String.format(":%s", port);
        }
        String scheme = fullPath.getScheme();
        String host = String.format("%s%s", fullPath.getHost(), portstr);
        String path = fullPath.getPath();
        if (relativePath.startsWith("//")) {
            return Optional.ofNullable(String.join(":", scheme, relativePath));
        } else if (relativePath.startsWith("/")) {
            return Optional.ofNullable(String.format("%s://%s%s", scheme, host, relativePath));
        } else if (relativePath.startsWith("./")) {
            relativePath = relativePath.substring(2);
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            return Optional.ofNullable(String.format("%s://%s%s%s", scheme, host, path, relativePath));
        } else if (relativePath.startsWith("../")) {
            String prev = relativePath;
            int count = 0;
            for (; prev.startsWith("../"); count++) {
                if (prev.length() == 3) {
                    prev = "";
                    break;
                }
                prev = prev.substring(3);
            }
            String[] paths = path.split("/");
            if (paths.length < count) {
                count = paths.length;
            }
            String[] nps = new String[paths.length - count];
            System.arraycopy(paths, 0, nps, 0, nps.length);
            return Optional.ofNullable(String.format("%s://%s%s/%s", scheme, host, String.join("/", nps), prev));
        } else {
            return Optional.ofNullable(String.format("%s://%s%s/%s", scheme, host, path, relativePath));
        }
    }

    public static String extractDomain(String url) {
        if (url.startsWith("http")) {
            url = url.substring(url.indexOf("//") + 2);
        }
        if (url.contains("/")) {
            url = url.substring(0, url.indexOf("/"));
        }
        return url;
    }

    public static HttpProtocol resolve(String url) throws IllegalArgumentException {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("URL不能为空");
        }
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format("URL不正确 %s", url));
        }
        String scheme = matcher.group(1);
        String domain = matcher.group(2);
        int port = -1;
        if (StringUtils.isNotBlank(matcher.group(3)) && matcher.group(3).length() > 1) {
            port = Integer.parseInt(matcher.group(3).substring(1));
        }
        if (port == -1) {
            switch (scheme) {
                case "http":
                    port = 80;
                    break;
                default:
                    port = 443;
            }
        }
        String path = matcher.group(4);
        if (StringUtils.isBlank(path)) {
            path = "/";
        }
        return new HttpProtocol(scheme, path, domain, port);
    }

    /**
     * URL合法性校验
     *
     * @param url
     * @return
     */
    public static boolean urlLegalVerify(String url) {
        return pattern.matcher(url).find();
    }

    @Data
    @AllArgsConstructor
    public static class HttpProtocol {
        String scheme;
        String path;
        String host;
        int port;
    }
}
