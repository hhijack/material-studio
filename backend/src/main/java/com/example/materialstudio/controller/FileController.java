package com.example.materialstudio.controller;

import com.example.materialstudio.model.Task;
import com.example.materialstudio.model.User;
import com.example.materialstudio.service.StorageService;
import com.example.materialstudio.service.TaskService;
import com.example.materialstudio.web.ApiException;
import com.example.materialstudio.web.CurrentUser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 生成结果文件读取（校验任务归属）。 */
@RestController
@RequestMapping("/api/files/results")
public class FileController {

    private final TaskService taskService;
    private final StorageService storage;

    public FileController(TaskService taskService, StorageService storage) {
        this.taskService = taskService;
        this.storage = storage;
    }

    @GetMapping("/{taskId}.png")
    public ResponseEntity<FileSystemResource> result(@CurrentUser User user, @PathVariable String taskId) {
        Task task = taskService.getOwnedTask(user, taskId);
        if (task.resultUrl() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RESULT_NOT_READY", "结果尚未生成");
        }
        Path path = storage.resolveResult(taskId + ".png");
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_MISSING", "结果文件不存在");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/png")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(new FileSystemResource(path));
    }
}
