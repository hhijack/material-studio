package com.example.materialstudio.mapper;

import com.example.materialstudio.model.Stage;
import com.example.materialstudio.model.TaskStepRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskStepMapper {

    void insertBatch(@Param("taskId") String taskId, @Param("steps") List<TaskStepRow> steps);

    List<TaskStepRow> findByTaskId(@Param("taskId") String taskId);

    void markRunning(@Param("taskId") String taskId, @Param("stage") Stage stage,
                     @Param("startedAt") LocalDateTime startedAt);

    void markPassed(@Param("taskId") String taskId, @Param("stage") Stage stage,
                    @Param("message") String message, @Param("finishedAt") LocalDateTime finishedAt);

    void markFailed(@Param("taskId") String taskId, @Param("stage") Stage stage,
                    @Param("message") String message, @Param("finishedAt") LocalDateTime finishedAt);
}
