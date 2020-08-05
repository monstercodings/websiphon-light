package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.manager.IRegistry;

/**
 * 拥有注册功能的爬虫
 */
public interface RegistrableCrawler {
    IRegistry registry();
}
