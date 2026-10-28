package com.example.materialstudio.task;

import com.example.materialstudio.model.Stage;

/** 引擎事件回调：由持久化层实现，把每个执行边界写入 task / task_step。 */
public interface EngineListener {

    void onStageStart(Stage stage);

    void onStagePassed(Stage stage, String message);

    void onStageFailed(Stage stage, String reason);

    void onTaskSucceeded(String resultUrl);
}
