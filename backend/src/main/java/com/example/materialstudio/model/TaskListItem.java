package com.example.materialstudio.model;

import java.time.LocalDateTime;

/** 历史列表条目：任务字段 + 模板标题/封面（join template）。 */
public record TaskListItem(
        String id,
        String templateId,
        String templateTitle,
        String templateCoverUrl,
        TaskStatus status,
        Stage currentStage,
        Stage failStage,
        String failReason,
        String resultUrl,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {
}
