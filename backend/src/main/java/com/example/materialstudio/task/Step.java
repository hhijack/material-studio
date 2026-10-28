package com.example.materialstudio.task;

import com.example.materialstudio.model.Stage;

/** 单个制作步骤：实现者只关心业务校验/产物，顺序与中断由 TaskEngine 控制。 */
public interface Step {

    Stage stage();

    void execute(TaskContext ctx) throws TaskStepException;
}
