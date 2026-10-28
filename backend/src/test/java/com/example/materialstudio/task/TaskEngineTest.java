package com.example.materialstudio.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.materialstudio.model.SimConfig;
import com.example.materialstudio.model.Stage;
import com.example.materialstudio.model.Template;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 任务引擎核心行为：步骤顺序、模拟耗时归属、失败即中断。不依赖 Spring 与数据库。 */
class TaskEngineTest {

    private static Template template() {
        return new Template("travel-postcard", "旅行明信片", "创意图片", "/demo/cover.svg", "desc",
                2, List.of("image/png"), 1024, 1);
    }

    /** 构造三个假步骤：记录执行顺序；模拟生成阶段可注入异常。 */
    private static List<Step> steps(List<String> log, RuntimeException boom) {
        Step materialCheck = new Step() {
            @Override
            public Stage stage() {
                return Stage.MATERIAL_CHECK;
            }

            @Override
            public void execute(TaskContext c) {
                log.add("exec:MATERIAL_CHECK");
            }
        };
        Step simulateGenerate = new Step() {
            @Override
            public Stage stage() {
                return Stage.SIMULATE_GENERATE;
            }

            @Override
            public void execute(TaskContext c) throws TaskStepException {
                log.add("exec:SIMULATE_GENERATE");
                c.requireNoInjectedFailure(stage());
                if (boom != null) {
                    throw boom;
                }
            }
        };
        Step resultFinalize = new Step() {
            @Override
            public Stage stage() {
                return Stage.RESULT_FINALIZE;
            }

            @Override
            public void execute(TaskContext c) {
                log.add("exec:RESULT_FINALIZE");
                c.setResultUrl("/api/files/results/task-1.png");
            }
        };
        return List.of(materialCheck, simulateGenerate, resultFinalize);
    }

    private static TaskEngine engine(List<Long> slept, List<String> log) {
        return new TaskEngine(millis -> slept.add(millis));
    }

    private static EngineListener listener(List<String> log) {
        return new EngineListener() {
            @Override
            public void onStageStart(Stage stage) {
                log.add("start:" + stage);
            }

            @Override
            public void onStagePassed(Stage stage, String message) {
                log.add("pass:" + stage);
            }

            @Override
            public void onStageFailed(Stage stage, String reason) {
                log.add("fail:" + stage + ":" + reason);
            }

            @Override
            public void onTaskSucceeded(String resultUrl) {
                log.add("success:" + resultUrl);
            }
        };
    }

    @Test
    void 全部通过时按顺序执行三步并回调成功() throws Exception {
        List<String> log = new ArrayList<>();
        List<Long> slept = new ArrayList<>();
        String[] urlHolder = new String[1];
        TaskContext ctx = new TaskContext("task-1", "user-1", template(), List.of(),
                new SimConfig(10, 20, 30, null));
        new TaskEngine(millis -> slept.add(millis))
                .run(ctx, steps(log, null), listener(log));

        assertThat(log).containsExactly(
                "start:MATERIAL_CHECK", "exec:MATERIAL_CHECK", "pass:MATERIAL_CHECK",
                "start:SIMULATE_GENERATE", "exec:SIMULATE_GENERATE", "pass:SIMULATE_GENERATE",
                "start:RESULT_FINALIZE", "exec:RESULT_FINALIZE", "pass:RESULT_FINALIZE",
                "success:/api/files/results/task-1.png");
        // 每个阶段的模拟耗时来自 sim 配置
        assertThat(slept).containsExactly(10L, 20L, 30L);
    }

    @Test
    void 注入失败时停止后续步骤且不回调成功() throws Exception {
        List<String> log = new ArrayList<>();
        String[] urlHolder = new String[1];
        TaskContext ctx = new TaskContext("task-1", "user-1", template(), List.of(),
                new SimConfig(10, 20, 30, Stage.SIMULATE_GENERATE));
        new TaskEngine(millis -> {
        }).run(ctx, steps(log, null), listener(log));

        assertThat(log).containsExactly(
                "start:MATERIAL_CHECK", "exec:MATERIAL_CHECK", "pass:MATERIAL_CHECK",
                "start:SIMULATE_GENERATE", "exec:SIMULATE_GENERATE",
                "fail:SIMULATE_GENERATE:模拟故障注入：本次任务被配置为在「模拟生成」阶段失败");
        // 结果整理阶段完全未执行
        assertThat(log).noneMatch(s -> s.contains("RESULT_FINALIZE"));
        assertThat(log).noneMatch(s -> s.startsWith("success"));
    }

    @Test
    void 步骤抛出非预期异常时视为内部错误并中断() throws Exception {
        List<String> log = new ArrayList<>();
        TaskContext ctx = new TaskContext("task-1", "user-1", template(), List.of(),
                new SimConfig(0, 0, 0, null));
        new TaskEngine(millis -> {
        }).run(ctx, steps(log, new RuntimeException("boom")), listener(log));

        assertThat(log).containsExactly(
                "start:MATERIAL_CHECK", "exec:MATERIAL_CHECK", "pass:MATERIAL_CHECK",
                "start:SIMULATE_GENERATE", "exec:SIMULATE_GENERATE",
                "fail:SIMULATE_GENERATE:内部错误：boom");
    }
}
