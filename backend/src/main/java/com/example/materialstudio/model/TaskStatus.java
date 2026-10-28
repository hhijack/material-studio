package com.example.materialstudio.model;

/** 任务整体状态：等待处理 → 处理中 → 成功/失败。 */
public enum TaskStatus {
    WAITING, PROCESSING, SUCCESS, FAILED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
}
