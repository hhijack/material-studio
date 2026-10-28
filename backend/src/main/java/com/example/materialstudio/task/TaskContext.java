package com.example.materialstudio.task;

import com.example.materialstudio.model.Asset;
import com.example.materialstudio.model.SimConfig;
import com.example.materialstudio.model.Stage;
import com.example.materialstudio.model.Template;
import java.util.List;

/** 一次任务执行中传给各步骤的上下文。 */
public final class TaskContext {

    private final String taskId;
    private final String userId;
    private final Template template;
    private final List<Asset> assets;
    private final SimConfig sim;
    private String resultUrl;

    public TaskContext(String taskId, String userId, Template template, List<Asset> assets, SimConfig sim) {
        this.taskId = taskId;
        this.userId = userId;
        this.template = template;
        this.assets = List.copyOf(assets);
        this.sim = sim;
    }

    public String taskId() {
        return taskId;
    }

    public String userId() {
        return userId;
    }

    public Template template() {
        return template;
    }

    public List<Asset> assets() {
        return assets;
    }

    public SimConfig sim() {
        return sim;
    }

    public String resultUrl() {
        return resultUrl;
    }

    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }

    /** 模拟故障注入：该阶段被配置为失败时抛出。由各步骤在真实校验之后调用。 */
    public void requireNoInjectedFailure(Stage stage) throws TaskStepException {
        if (sim.failStage() == stage) {
            throw new TaskStepException(
                    "模拟故障注入：本次任务被配置为在「" + stage.label() + "」阶段失败");
        }
    }
}
