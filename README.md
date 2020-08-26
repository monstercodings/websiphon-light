# 轻量化的爬虫框架

> 属于websiphon的轻量化版本  
> 当前版本 0.0.83

### 简要介绍

1. `优点`
    * 由于使用静态代理的方式，自由定制化的灵活度极高
    * 可自行扩展开发与websiphon标准版相同的功能
    * 基于ClassLoader实现了插件化功能
    * 依赖第三方jar包较少，体积小
    * 基于jdk11编译，利用其新特性提高运行效率

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
    crawler.startup();
    crawler.push("https://www.baidu111.com");
    // 主动关闭爬虫
    // crawler.shutdown();
```