package com.example.materialstudio.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.* 配置项：数据目录、会话 Cookie、CORS。 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String dataDir, Session session, Cors cors) {

    public record Session(String cookieName, long maxAgeSeconds) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
