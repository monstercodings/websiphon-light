package top.codings.websiphon.light.loader.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ClassDefinition {
    private String name;
    private String className;
    private String version;
    private String description;
    private PluginType type;
    private JarDefinition jarDefinition;
}
