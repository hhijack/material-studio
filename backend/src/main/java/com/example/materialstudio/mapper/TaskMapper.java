package com.example.materialstudio.mapper;

import com.example.materialstudio.model.Stage;
import com.example.materialstudio.model.Task;
import com.example.materialstudio.model.TaskListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskMapper {

    void insert(Task task);

    Task findById(@Param("id") String id);

    /** 个人历史（按创建时间倒序），join template 带出标题与封面。 */
    List<TaskListItem> findListByUser(@Param("userId") String userId);

    /** 仅当任务仍处于 WAITING 时置为 PROCESSING；返回受影响行数（0=已被处理，调用方应中止执行）。 */
    int markProcessing(@Param("id") String id, @Param("startedAt") LocalDateTime startedAt);

    void setCurrentStage(@Param("id") String id, @Param("stage") Stage stage);

    void markSuccess(@Param("id") String id, @Param("resultUrl") String resultUrl,
                     @Param("finishedAt") LocalDateTime finishedAt);

    void markFailed(@Param("id") String id, @Param("failStage") Stage failStage,
                    @Param("failReason") String failReason, @Param("finishedAt") LocalDateTime finishedAt);
}
