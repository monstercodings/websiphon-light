package top.codings.websiphon.light.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Article {
    protected String url;
    protected String title;
    protected String author;
    protected String content;
    protected long createdAt;
}
