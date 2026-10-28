package com.example.materialstudio.task;

import com.example.materialstudio.model.Stage;
import java.util.List;

/**
 * 任务执行引擎：按 steps 顺序执行；每步开始先回调 onStageStart（含模拟耗时休眠），
 * 抛出 TaskStepException 视为该阶段失败——记录失败阶段与原因并终止后续步骤；
 * 其余异常视为内部错误同样终止；全部通过后回调 onTaskSucceeded。
 * 纯 Java 无 Spring 依赖，便于对「步骤顺序与失败中断」做单元测试。
 */
public class TaskEngine {

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final Sleeper sleeper;

    public TaskEngine(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    /** 生产用：真实休眠模拟耗时。 */
    public static TaskEngine withRealSleep() {
        return new TaskEngine(millis -> {
            if (millis > 0) {
                Thread.sleep(millis);
            }
        });
    }

    public void run(TaskContext ctx, List<Step> steps, EngineListener listener) throws InterruptedException {
        for (Step step : steps) {
            Stage stage = step.stage();
            listener.onStageStart(stage);
            sleeper.sleep(ctx.sim().durationOf(stage));
            try {
                step.execute(ctx);
            } catch (TaskStepException e) {
                listener.onStageFailed(stage, e.getMessage());
                return;
            } catch (Exception e) {
                listener.onStageFailed(stage, "内部错误：" + e.getMessage());
                return;
            }
            listener.onStagePassed(stage, null);
        }
        listener.onTaskSucceeded(ctx.resultUrl());
    }
}
