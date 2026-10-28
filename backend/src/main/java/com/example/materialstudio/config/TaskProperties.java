package com.example.materialstudio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** task.execution 配置：async（默认，单线程队列）或 sync（同线程执行，测试用）。 */
@ConfigurationProperties(prefix = "task")
public record TaskProperties(Execution execution) {

    public record Execution(String mode) {
        public boolean isSync() {
            return "sync".equalsIgnoreCase(mode);
        }
    }
}
