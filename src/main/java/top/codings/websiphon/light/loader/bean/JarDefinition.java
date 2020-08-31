package top.codings.websiphon.light.loader.bean;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;

@Setter
@Getter
@NoArgsConstructor
public class JarDefinition {
    private String name;
    private String version;
    private String description;
    private String author;
    private String homepage;
    private String packaging;
//    private String fullPath;
    private boolean inner;
    private ClassDefinition[] classDefinitions;
    @JSONField(serialize = false, deserialize = false)
    private File localFile;

    public JarDefinition(String name, String version, String description, String author, String homepage, String packaging, File localFile) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.author = author;
        this.homepage = homepage;
        this.packaging = packaging;
        this.localFile = localFile;
    }
}
