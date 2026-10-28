package com.example.materialstudio.service;

import com.example.materialstudio.mapper.AssetMapper;
import com.example.materialstudio.model.Asset;
import com.example.materialstudio.model.Template;
import com.example.materialstudio.model.User;
import com.example.materialstudio.web.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 素材上传与读取：服务端校验类型/大小 + 魔数复核，存本地数据目录。 */
@Service
public class AssetService {

    /** 声明类型的魔数前缀（够用的轻量校验）。 */
    private static final Map<String, byte[]> MAGIC = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'},
            "image/gif", new byte[]{'G', 'I', 'F'},
            "image/webp", new byte[]{'R', 'I', 'F', 'F'});

    private static final Map<String, String> EXT = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/gif", ".gif", "image/webp", ".webp");

    private final AssetMapper assetMapper;
    private final TemplateService templateService;
    private final StorageService storage;

    public AssetService(AssetMapper assetMapper, TemplateService templateService, StorageService storage) {
        this.assetMapper = assetMapper;
        this.templateService = templateService;
        this.storage = storage;
    }

    public Asset upload(User user, String templateId, MultipartFile file) {
        Template template = templateService.get(templateId);
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "上传文件为空");
        }
        String mimeType = file.getContentType();
        if (mimeType == null || !template.acceptedMimeTypes().contains(mimeType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TYPE",
                    "不支持的图片类型 " + mimeType + "，模板仅接受：" + String.join(", ", template.acceptedMimeTypes()));
        }
        if (file.getSize() > template.maxFileSizeBytes()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE",
                    "文件超过大小限制 " + formatSize(template.maxFileSizeBytes()));
        }
        verifyMagicBytes(file, mimeType);

        String id = UUID.randomUUID().toString();
        String storedName = id + EXT.getOrDefault(mimeType, ".bin");
        try (InputStream in = file.getInputStream()) {
            storage.saveUpload(storedName, in);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORE_FAILED", "素材保存失败");
        }
        Asset asset = new Asset(id, user.id(), templateId, sanitize(file.getOriginalFilename()),
                storedName, mimeType, file.getSize(), LocalDateTime.now());
        assetMapper.insert(asset);
        return asset;
    }

    /** 供结果/预览读取：必须是本人的素材。 */
    public Asset getOwned(User user, String assetId) {
        Asset asset = assetMapper.findById(assetId);
        if (asset == null || !asset.userId().equals(user.id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND", "素材不存在");
        }
        return asset;
    }

    private void verifyMagicBytes(MultipartFile file, String mimeType) {
        byte[] prefix = MAGIC.get(mimeType);
        if (prefix == null) {
            return;
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(Math.max(prefix.length, 12));
            for (int i = 0; i < prefix.length; i++) {
                if (i >= head.length || head[i] != prefix[i]) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH",
                            "文件内容与声明的图片类型不一致");
                }
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "READ_FAILED", "无法读取上传文件");
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "未命名图片";
        }
        String cleaned = name.replaceAll("[\\r\\n\\t]", "");
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    private String formatSize(long bytes) {
        return bytes >= 1024 * 1024 ? (bytes / 1024 / 1024) + "MB" : (bytes / 1024) + "KB";
    }
}
