# 轻量化的爬虫框架

> 属于websiphon的轻量化版本  
> 当前版本 0.2.1

### 简要介绍

1. `优点`
    * 由于使用静态代理的方式，自由定制化的灵活度极高
    * 可自行扩展开发与websiphon标准版相同的功能
    * 基于自定义ClassLoader实现了插件化功能
    * 依赖第三方jar包较少，体积小
    * 使用共享机制保证业务处理的并发安全警告
    * 使用感知接口实现感知机制，使得业务类灵活配置各种感知功能
    * 基于jdk11编译，利用其新特性提高运行效率
    * 内置m3u8视频下载处理器

2. `缺点`
    * 无法兼容jdk11以下的版本
    * 相较于Websiphon标准版，内置功能模块集成度较低
    
### 快速开始

> 只需极少代码即可启动一个爬虫

```java
ICrawler crawler = new BaseCrawler(
        new AbstractResponseHandler() {
            @Override
            public void handle(IRequest request) {
                // 此处写响应处理逻辑
            }
        }
);
crawler.startup()
       .whenCompleteAsync((c, throwable) -> {
                             if (throwable != null) {
                                 log.error("爬虫启动失败", throwable);
                             } else {
                                 log.debug("[{}]爬虫启动成功", c.config().getName());
                                 c.push("https://www.baidu.com");
                             }
                         });
// 主动关闭爬虫
// crawler.shutdown();
```