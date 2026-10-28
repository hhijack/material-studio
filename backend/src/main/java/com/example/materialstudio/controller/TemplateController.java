package com.example.materialstudio.controller;

import com.example.materialstudio.model.CategoryInfo;
import com.example.materialstudio.model.Template;
import com.example.materialstudio.service.TemplateService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 模板查询。 */
@RestController
@RequestMapping("/api")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/templates")
    public List<Template> list() {
        return templateService.list();
    }

    @GetMapping("/templates/{id}")
    public Template detail(@PathVariable String id) {
        return templateService.get(id);
    }

    @GetMapping("/categories")
    public List<CategoryInfo> categories() {
        return templateService.categories();
    }
}
