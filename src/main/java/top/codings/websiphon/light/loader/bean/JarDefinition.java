package top.codings.websiphon.light.loader.bean;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JarDefinition {
    private String name;
    private String version;
    private String description;
    private String author;
    private String homepage;
    private String fullPath;
    private boolean inner;
    private ClassDefinition[] classDefinitions;

    public JarDefinition(String name, String version, String description, String author, String homepage, String fullPath) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.author = author;
        this.homepage = homepage;
        this.fullPath = fullPath;
    }
}
