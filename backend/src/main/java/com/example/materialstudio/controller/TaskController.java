package com.example.materialstudio.controller;

import com.example.materialstudio.model.TaskListItem;
import com.example.materialstudio.model.User;
import com.example.materialstudio.service.TaskService;
import com.example.materialstudio.web.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 任务受理（立即返回 taskId）与只读状态/历史查询。 */
@RestController
@RequestMapping("/api")
public class TaskController {

    public record CreateTaskRequest(@NotBlank String templateId, @NotEmpty List<String> assetIds) {
    }

    public record CreateTaskResponse(String taskId) {
    }

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/tasks")
    public CreateTaskResponse create(@CurrentUser User user, @Valid @RequestBody CreateTaskRequest request,
                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return new CreateTaskResponse(taskService.create(user, request.templateId(), request.assetIds(), idempotencyKey).taskId());
    }

    @GetMapping("/tasks/mine")
    public List<TaskListItem> mine(@CurrentUser User user) {
        return taskService.listMine(user);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskService.TaskDetail detail(@CurrentUser User user, @PathVariable String taskId) {
        return taskService.detail(user, taskId);
    }
}
