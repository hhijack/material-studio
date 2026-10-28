package com.example.materialstudio.model;

/** 模拟器配置：各阶段模拟耗时与注入失败阶段（null=不注入）。 */
public record SimConfig(long materialCheckMs, long simulateGenerateMs, long resultFinalizeMs, Stage failStage) {

    public long durationOf(Stage stage) {
        return switch (stage) {
            case MATERIAL_CHECK -> materialCheckMs;
            case SIMULATE_GENERATE -> simulateGenerateMs;
            case RESULT_FINALIZE -> resultFinalizeMs;
        };
    }

    public static final long MIN_MS = 0;
    public static final long MAX_MS = 60_000;
}
