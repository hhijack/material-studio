package com.example.materialstudio.task;

import com.example.materialstudio.model.Stage;
import org.springframework.stereotype.Component;

/** 第 3 步：结果整理——合成可预览的结果图并登记结果地址。 */
@Component
public class ResultFinalizeStep implements Step {

    private final ResultComposer composer;

    public ResultFinalizeStep(ResultComposer composer) {
        this.composer = composer;
    }

    @Override
    public Stage stage() {
        return Stage.RESULT_FINALIZE;
    }

    @Override
    public void execute(TaskContext ctx) throws TaskStepException {
        ctx.requireNoInjectedFailure(stage());
        String url = composer.compose(ctx.taskId(), ctx.template(), ctx.assets());
        ctx.setResultUrl(url);
    }
}
