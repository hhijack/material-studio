package com.example.materialstudio.task;

/** 步骤失败：message 为可理解的失败原因，会落库到 task.fail_reason 与 task_step.message。 */
public class TaskStepException extends Exception {

    public TaskStepException(String message) {
        super(message);
    }
}
