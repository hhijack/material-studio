package com.example.materialstudio.model;

import java.util.List;

/** 制作模板（domain 对象，accepted_mime_types 已解析为列表）。 */
public record Template(
        String id,
        String title,
        String category,
        String coverUrl,
        String description,
        int requiredImageCount,
        List<String> acceptedMimeTypes,
        long maxFileSizeBytes,
        int sortOrder) {
}
