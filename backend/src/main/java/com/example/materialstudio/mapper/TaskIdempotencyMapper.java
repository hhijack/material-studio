package com.example.materialstudio.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskIdempotencyMapper {

    /** 按幂等键查已受理的 taskId；不存在返回 null。 */
    String findTaskIdByKey(@Param("key") String key);

    /** 注册幂等键（key 唯一约束；并发冲突抛 DuplicateKeyException）。 */
    void insert(@Param("key") String key, @Param("userId") String userId, @Param("taskId") String taskId);
}
