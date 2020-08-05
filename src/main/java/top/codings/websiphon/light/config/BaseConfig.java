package top.codings.websiphon.light.config;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BaseConfig {
    private KafkaConfig kafkaConfig;
    private RedisConfig redisConfig;

    @Getter
    @Builder
    public static class KafkaConfig {
        /**
         * Kafka服务器地址
         */
        private String kafkaHosts;
        /**
         * 推送文章的主题
         */
        private String articleTopic;
        /**
         * 子任务完成推送主题
         */
        private String subTaskTopic;
    }

    @Getter
    @Builder
    public static class RedisConfig {
        private String hosts;
        private String username;
        private String password;
    }
}
