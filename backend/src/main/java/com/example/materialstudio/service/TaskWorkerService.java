package com.example.materialstudio.service;

import com.example.materialstudio.config.TaskProperties;
import com.example.materialstudio.mapper.TaskAssetMapper;
import com.example.materialstudio.mapper.TaskMapper;
import com.example.materialstudio.model.Asset;
import com.example.materialstudio.model.SimConfig;
import com.example.materialstudio.model.Stage;
import com.example.materialstudio.model.Task;
import com.example.materialstudio.model.TaskStatus;
import com.example.materialstudio.model.Template;
import com.example.materialstudio.task.EngineListener;
import com.example.materialstudio.task.MaterialCheckStep;
import com.example.materialstudio.task.ResultFinalizeStep;
import com.example.materialstudio.task.SimulateGenerateStep;
import com.example.materialstudio.task.TaskContext;
import com.example.materialstudio.task.TaskEngine;
import com.example.materialstudio.task.Step;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 任务工作线程：受理后从这里异步执行。
 * async 模式为单线程队列（多任务排队、互不阻塞受理）；
 * sync 模式在提交线程内同步执行，供自动化测试/CI 获得确定性结果。
 */
@Service
public class TaskWorkerService {

    private static final Logger log = LoggerFactory.getLogger(TaskWorkerService.class);

    private final TaskMapper taskMapper;
    private final TaskAssetMapper taskAssetMapper;
    private final TemplateService templateService;
    private final SimConfigService simConfigService;
    private final TaskProgressService progress;
    private final MaterialCheckStep materialCheckStep;
    private final SimulateGenerateStep simulateGenerateStep;
    private final ResultFinalizeStep resultFinalizeStep;
    private final boolean syncMode;

    private final ExecutorService worker;
    private final TaskEngine engine = TaskEngine.withRealSleep();

    public TaskWorkerService(TaskMapper taskMapper, TaskAssetMapper taskAssetMapper,
                             TemplateService templateService, SimConfigService simConfigService,
                             TaskProgressService progress, MaterialCheckStep materialCheckStep,
                             SimulateGenerateStep simulateGenerateStep, ResultFinalizeStep resultFinalizeStep,
                             TaskProperties taskProperties) {
        this.taskMapper = taskMapper;
        this.taskAssetMapper = taskAssetMapper;
        this.templateService = templateService;
        this.simConfigService = simConfigService;
        this.progress = progress;
        this.materialCheckStep = materialCheckStep;
        this.simulateGenerateStep = simulateGenerateStep;
        this.resultFinalizeStep = resultFinalizeStep;
        this.syncMode = taskProperties.execution().isSync();
        if (syncMode) {
            this.worker = null;
        } else {
            this.worker = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "task-worker");
                t.setDaemon(true);
                return t;
            });
        }
    }

    /** 受理入口：入队（async）或同线程执行（sync），均立即返回。 */
    public void submit(String taskId) {
        if (syncMode) {
            process(taskId);
        } else {
            worker.execute(() -> process(taskId));
        }
    }

    void process(String taskId) {
        try {
            doProcess(taskId);
        } catch (Throwable t) {
            // 兜底：任何未预期异常都不能让任务悬挂在 PROCESSING
            log.error("任务 {} 执行出现未预期异常", taskId, t);
            try {
                progress.failTask(taskId, "服务器内部错误：" + t.getMessage());
            } catch (Exception swallow) {
                log.error("任务 {} 兜底落库失败", taskId, swallow);
            }
        }
    }

    private void doProcess(String taskId) throws InterruptedException {
        Task task = taskMapper.findById(taskId);
        if (task == null || task.status() != TaskStatus.WAITING) {
            // 已被处理过或不存在：查询只读、引擎不重复执行
            return;
        }
        Template template = templateService.get(task.templateId());
        List<Asset> assets = taskAssetMapper.findAssetsByTaskId(taskId);
        SimConfig sim = simConfigService.current();

        progress.markProcessing(taskId);
        TaskContext ctx = new TaskContext(taskId, task.userId(), template, assets, sim);
        List<Step> steps = List.of(materialCheckStep, simulateGenerateStep, resultFinalizeStep);

        engine.run(ctx, steps, new EngineListener() {
            @Override
            public void onStageStart(Stage stage) {
                progress.beginStage(taskId, stage);
            }

            @Override
            public void onStagePassed(Stage stage, String message) {
                progress.completeStage(taskId, stage, message);
            }

            @Override
            public void onStageFailed(Stage stage, String reason) {
                log.info("任务 {} 在阶段 {} 失败：{}", taskId, stage, reason);
                progress.failStageAndTask(taskId, stage, reason);
            }

            @Override
            public void onTaskSucceeded(String resultUrl) {
                progress.completeTask(taskId, resultUrl);
            }
        });
    }

    @PreDestroy
    void shutdown() {
        if (worker == null) {
            return;
        }
        worker.shutdown();
        try {
            if (!worker.awaitTermination(10, TimeUnit.SECONDS)) {
                worker.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            worker.shutdownNow();
        }
    }
}
