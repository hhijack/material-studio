package com.example.materialstudio.service;

import com.example.materialstudio.mapper.TaskMapper;
import com.example.materialstudio.mapper.TaskStepMapper;
import com.example.materialstudio.model.Stage;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 任务执行过程中的状态落库（每个边界一次独立事务）。 */
@Service
public class TaskProgressService {

    private final TaskMapper taskMapper;
    private final TaskStepMapper stepMapper;

    public TaskProgressService(TaskMapper taskMapper, TaskStepMapper stepMapper) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
    }

    /** 返回受影响行数：0 表示任务已不在 WAITING（防御性条件，见 01-D6）。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markProcessing(String taskId) {
        return taskMapper.markProcessing(taskId, LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void beginStage(String taskId, Stage stage) {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.setCurrentStage(taskId, stage);
        stepMapper.markRunning(taskId, stage, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeStage(String taskId, Stage stage, String message) {
        stepMapper.markPassed(taskId, stage, message, LocalDateTime.now());
    }

    /** 该阶段失败 → 记录失败阶段与原因，任务整体置为 FAILED（后续步骤不再执行）。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failStageAndTask(String taskId, Stage stage, String reason) {
        LocalDateTime now = LocalDateTime.now();
        stepMapper.markFailed(taskId, stage, reason, now);
        taskMapper.markFailed(taskId, stage, reason, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeTask(String taskId, String resultUrl) {
        taskMapper.markSuccess(taskId, resultUrl, LocalDateTime.now());
    }

    /** 引擎之外的兜底：仅任务置失败（可能尚无任何步骤记录）。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failTask(String taskId, String reason) {
        taskMapper.markFailed(taskId, null, reason, LocalDateTime.now());
    }
}
