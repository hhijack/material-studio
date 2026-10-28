package com.example.materialstudio.mapper;

import com.example.materialstudio.model.CategoryInfo;
import com.example.materialstudio.model.Template;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TemplateMapper {

    /** 返回按 sort_order 排序的全部模板；accepted_mime_types 以 JSON 字符串返回。 */
    List<TemplateRow> findAll();

    TemplateRow findById(@Param("id") String id);

    List<CategoryInfo> findCategories();

    /** DB 行对象：accepted_mime_types 仍为 JSON 字符串，由 service 解析。 */
    record TemplateRow(
            String id,
            String title,
            String category,
            String coverUrl,
            String description,
            int requiredImageCount,
            String acceptedMimeTypesJson,
            long maxFileSizeBytes,
            int sortOrder) {
    }
}
