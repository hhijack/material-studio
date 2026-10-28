package com.example.materialstudio.model;

import java.time.LocalDateTime;

/** 制作任务。status 与 currentStage 分别表达整体状态与当前阶段。 */
public record Task(
        String id,
        String userId,
        String templateId,
        TaskStatus status,
        Stage currentStage,
        Stage failStage,
        String failReason,
        String resultUrl,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {
}
