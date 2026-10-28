package com.example.materialstudio.mapper;

import com.example.materialstudio.model.Asset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssetMapper {

    void insert(Asset asset);

    Asset findById(@Param("id") String id);

    List<Asset> findByIds(@Param("ids") List<String> ids);
}
