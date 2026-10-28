package com.example.materialstudio.service;

import com.example.materialstudio.mapper.TemplateMapper;
import com.example.materialstudio.model.CategoryInfo;
import com.example.materialstudio.model.Template;
import com.example.materialstudio.web.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

    private final TemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    public TemplateService(TemplateMapper templateMapper, ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.objectMapper = objectMapper;
    }

    public List<Template> list() {
        return templateMapper.findAll().stream().map(this::toDomain).toList();
    }

    public List<CategoryInfo> categories() {
        return templateMapper.findCategories();
    }

    public Template get(String id) {
        TemplateMapper.TemplateRow row = templateMapper.findById(id);
        if (row == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "模板不存在：" + id);
        }
        return toDomain(row);
    }

    private Template toDomain(TemplateMapper.TemplateRow row) {
        List<String> mimes = parseMimes(row.acceptedMimeTypesJson());
        return new Template(row.id(), row.title(), row.category(), row.coverUrl(), row.description(),
                row.requiredImageCount(), mimes, row.maxFileSizeBytes(), row.sortOrder());
    }

    private List<String> parseMimes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
