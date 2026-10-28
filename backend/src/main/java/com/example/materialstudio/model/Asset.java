package com.example.materialstudio.model;

import java.time.LocalDateTime;

/** 已上传的素材文件记录。 */
public record Asset(
        String id,
        String userId,
        String templateId,
        String originalName,
        String storedName,
        String mimeType,
        long sizeBytes,
        LocalDateTime createdAt) {

    public String accessUrl() {
        return "/api/files/uploads/" + id;
    }
}
