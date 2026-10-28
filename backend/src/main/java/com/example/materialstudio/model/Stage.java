package com.example.materialstudio.model;

/** 制作任务的三个执行阶段，按 steps 顺序执行。 */
public enum Stage {
    MATERIAL_CHECK("素材检查"),
    SIMULATE_GENERATE("模拟生成"),
    RESULT_FINALIZE("结果整理");

    private final String label;

    Stage(String label) {
        this.label = label;
    }

    /** 展示用名称，如「素材检查」。 */
    public String label() {
        return label;
    }

    public static Stage parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return Stage.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
