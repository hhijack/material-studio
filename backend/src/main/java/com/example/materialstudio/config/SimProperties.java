package com.example.materialstudio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** sim.* 模拟配置的启动默认值；运行期可通过 sim_config 表 + /api/dev/sim-config 覆盖。 */
@ConfigurationProperties(prefix = "sim")
public record SimProperties(long materialCheckMs, long simulateGenerateMs, long resultFinalizeMs,
                            String failStage, boolean devApiEnabled) {
}
