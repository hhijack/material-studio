package com.example.materialstudio.task;

import com.example.materialstudio.model.Stage;
import org.springframework.stereotype.Component;

/**
 * 第 2 步：模拟生成——不调用真实 AI，耗时已由引擎按 sim 配置休眠模拟。
 * 仅做故障注入检查；生成的产物在结果整理阶段合成。
 */
@Component
public class SimulateGenerateStep implements Step {

    @Override
    public Stage stage() {
        return Stage.SIMULATE_GENERATE;
    }

    @Override
    public void execute(TaskContext ctx) throws TaskStepException {
        ctx.requireNoInjectedFailure(stage());
    }
}
