package top.codings.websiphon.light.loader.bean;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JarDefinition {
    private String name;
    private String version;
    private String description;
    private String fullPath;
    private ClassDefinition[] classDefinitions;
}
