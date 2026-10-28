package com.example.materialstudio.service;

import com.example.materialstudio.mapper.AssetMapper;
import com.example.materialstudio.mapper.TaskAssetMapper;
import com.example.materialstudio.mapper.TaskMapper;
import com.example.materialstudio.mapper.TaskStepMapper;
import com.example.materialstudio.model.Asset;
import com.example.materialstudio.model.Stage;
import com.example.materialstudio.model.Task;
import com.example.materialstudio.model.TaskListItem;
import com.example.materialstudio.model.TaskStatus;
import com.example.materialstudio.model.TaskStepRow;
import com.example.materialstudio.model.StepStatus;
import com.example.materialstudio.model.Template;
import com.example.materialstudio.model.User;
import com.example.materialstudio.web.ApiException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 任务受理（校验+落库+入队，立即返回）与只读查询。 */
@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskStepMapper stepMapper;
    private final TaskAssetMapper taskAssetMapper;
    private final AssetMapper assetMapper;
    private final TemplateService templateService;
    private final TaskWorkerService worker;

    public TaskService(TaskMapper taskMapper, TaskStepMapper stepMapper, TaskAssetMapper taskAssetMapper,
                       AssetMapper assetMapper, TemplateService templateService, TaskWorkerService worker) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.taskAssetMapper = taskAssetMapper;
        this.assetMapper = assetMapper;
        this.templateService = templateService;
        this.worker = worker;
    }

    public record CreateResult(String taskId) {
    }

    /** 受理：参数/素材校验 → 任务与步骤记录落库（WAITING）→ 入队。不等待制作结束。 */
    @Transactional
    public CreateResult create(User user, String templateId, List<String> assetIds) {
        Template template = templateService.get(templateId);
        if (assetIds == null || assetIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ASSETS_REQUIRED", "请先上传素材");
        }
        Set<String> distinctIds = new LinkedHashSet<>(assetIds);
        if (distinctIds.size() != template.requiredImageCount()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ASSET_COUNT_MISMATCH",
                    "该模板需要恰好 " + template.requiredImageCount() + " 张图片，实际提交 " + distinctIds.size() + " 张");
        }
        List<Asset> assets = assetMapper.findByIds(List.copyOf(distinctIds));
        if (assets.size() != distinctIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ASSET_NOT_FOUND", "部分素材不存在，请重新上传");
        }
        for (Asset asset : assets) {
            if (!asset.userId().equals(user.id())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "ASSET_FORBIDDEN", "包含不属于当前用户的素材");
            }
            if (!template.id().equals(asset.templateId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ASSET_TEMPLATE_MISMATCH", "素材与模板不匹配");
            }
            if (!template.acceptedMimeTypes().contains(asset.mimeType())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TYPE",
                        "素材 " + asset.originalName() + " 类型不符合模板要求");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String taskId = java.util.UUID.randomUUID().toString();
        taskMapper.insert(new Task(taskId, user.id(), template.id(), TaskStatus.WAITING,
                null, null, null, null, now, null, null));
        stepMapper.insertBatch(taskId, List.of(
                new TaskStepRow(0, taskId, 0, Stage.MATERIAL_CHECK, StepStatus.PENDING, null, null, null),
                new TaskStepRow(0, taskId, 1, Stage.SIMULATE_GENERATE, StepStatus.PENDING, null, null, null),
                new TaskStepRow(0, taskId, 2, Stage.RESULT_FINALIZE, StepStatus.PENDING, null, null, null)));
        taskAssetMapper.insertBatch(taskId, assets);

        worker.submit(taskId);
        return new CreateResult(taskId);
    }

    /** 只读查询：不驱动任务推进，不改变状态。 */
    public Task getOwnedTask(User user, String taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null || !task.userId().equals(user.id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "任务不存在");
        }
        return task;
    }

    public record TaskDetail(Task task, Template template, List<TaskStepRow> steps, List<Asset> assets) {
    }

    public TaskDetail detail(User user, String taskId) {
        Task task = getOwnedTask(user, taskId);
        return new TaskDetail(task, templateService.get(task.templateId()),
                stepMapper.findByTaskId(taskId), taskAssetMapper.findAssetsByTaskId(taskId));
    }

    public List<TaskListItem> listMine(User user) {
        return taskMapper.findListByUser(user.id());
    }
}
