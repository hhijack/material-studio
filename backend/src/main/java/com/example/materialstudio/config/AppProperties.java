package com.example.materialstudio.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.* 配置项：数据目录、会话 Cookie、CORS、启动恢复。 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String dataDir, Session session, Cors cors, Recovery recovery) {

    public record Session(String cookieName, long maxAgeSeconds, String secret) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** 启动恢复未完成任务（R4）：生产可在环境变量中关闭。 */
    public record Recovery(boolean onStartup) {
    }
}
