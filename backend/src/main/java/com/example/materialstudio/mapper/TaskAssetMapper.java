package com.example.materialstudio.mapper;

import com.example.materialstudio.model.Asset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskAssetMapper {

    void insertBatch(@Param("taskId") String taskId, @Param("assets") List<Asset> assets);

    /** 任务关联素材（按 sort_order 排序）。 */
    List<Asset> findAssetsByTaskId(@Param("taskId") String taskId);
}
