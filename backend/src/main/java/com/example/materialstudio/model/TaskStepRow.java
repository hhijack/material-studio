package com.example.materialstudio.model;

import java.time.LocalDateTime;

/** task_step 表一行：某任务某阶段的独立执行边界记录。 */
public record TaskStepRow(
        long id,
        String taskId,
        int stepIndex,
        Stage stage,
        StepStatus status,
        String message,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {
}
