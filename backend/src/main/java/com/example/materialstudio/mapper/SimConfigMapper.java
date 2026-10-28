package com.example.materialstudio.mapper;

import com.example.materialstudio.model.SimConfig;
import com.example.materialstudio.model.Stage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SimConfigMapper {

    SimConfig find();

    void upsert(@Param("materialCheckMs") long materialCheckMs,
                @Param("simulateGenerateMs") long simulateGenerateMs,
                @Param("resultFinalizeMs") long resultFinalizeMs,
                @Param("failStage") Stage failStage,
                @Param("updatedAt") LocalDateTime updatedAt);
}
