package top.codings.websiphon.light.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.loader.PluginDefinitionLoader;

@Slf4j
public class TestPluginDefinitionLoader {
    @Test
    public void test() {
        PluginDefinitionLoader loader = new PluginDefinitionLoader("config");
        log.debug("{}", loader.getDefinitions());
    }
}
