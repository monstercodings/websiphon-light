package top.codings.websiphon.light.loader.bean;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PluginType {
    REQUESTER("网络请求器"),
    HANDLER("响应管理器"),
    PROCESSOR("响应处理器"),
    ;
    private String desc;

    public String desc() {
        return desc;
    }
}
