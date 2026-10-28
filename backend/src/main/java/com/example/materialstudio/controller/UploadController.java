package com.example.materialstudio.controller;

import com.example.materialstudio.model.Asset;
import com.example.materialstudio.model.User;
import com.example.materialstudio.service.AssetService;
import com.example.materialstudio.service.StorageService;
import com.example.materialstudio.web.ApiException;
import com.example.materialstudio.web.CurrentUser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 素材上传与素材文件读取（校验归属）。 */
@RestController
@RequestMapping("/api")
public class UploadController {

    private final AssetService assetService;
    private final StorageService storage;

    public UploadController(AssetService assetService, StorageService storage) {
        this.assetService = assetService;
        this.storage = storage;
    }

    @PostMapping("/uploads")
    public UploadedAsset upload(@CurrentUser User user,
                                @RequestParam("templateId") String templateId,
                                @RequestParam("file") MultipartFile file) {
        Asset asset = assetService.upload(user, templateId, file);
        return toResponse(asset);
    }

    @GetMapping("/files/uploads/{assetId}")
    public ResponseEntity<FileSystemResource> serveUpload(@CurrentUser User user, @PathVariable String assetId) {
        Asset asset = assetService.getOwned(user, assetId);
        Path path = storage.resolveUpload(asset.storedName());
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_MISSING", "文件不存在");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, asset.mimeType())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(new FileSystemResource(path));
    }

    public record UploadedAsset(String id, String url, String originalName, String mimeType, long sizeBytes) {
    }

    /** 供参考：把 domain Asset 转为响应（含 url 字段）。 */
    public static UploadedAsset toResponse(Asset asset) {
        return new UploadedAsset(asset.id(), asset.accessUrl(), asset.originalName(), asset.mimeType(), asset.sizeBytes());
    }
}
