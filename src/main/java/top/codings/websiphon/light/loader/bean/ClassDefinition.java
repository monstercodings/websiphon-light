package top.codings.websiphon.light.loader.bean;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ClassDefinition {
    private String name;
    private String className;
    private String description;
    private JarDefinition jarDefinition;
}
