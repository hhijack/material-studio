package com.example.materialstudio.service;

import com.example.materialstudio.config.AppProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/** 本地文件存储：uploads/（用户上传素材）与 results/（生成结果），位于数据目录下。 */
@Service
public class StorageService {

    private final Path dataDir;
    private final Path uploadsDir;
    private final Path resultsDir;

    public StorageService(AppProperties props) {
        this.dataDir = Path.of(props.dataDir()).toAbsolutePath().normalize();
        this.uploadsDir = dataDir.resolve("uploads");
        this.resultsDir = dataDir.resolve("results");
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(uploadsDir);
        Files.createDirectories(resultsDir);
    }

    public Path saveUpload(String storedName, InputStream in) throws IOException {
        Path target = uploadsDir.resolve(storedName);
        Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    public Path resolveUpload(String storedName) {
        return safeResolve(uploadsDir, storedName);
    }

    public Path saveResult(String fileName, byte[] bytes) throws IOException {
        Path target = safeResolve(resultsDir, fileName);
        Files.write(target, bytes);
        return target;
    }

    public Path resolveResult(String fileName) {
        return safeResolve(resultsDir, fileName);
    }

    /** 防路径穿越：解析后必须仍位于目标目录内。 */
    private Path safeResolve(Path dir, String name) {
        Path resolved = dir.resolve(name).normalize();
        if (!resolved.startsWith(dir)) {
            throw new IllegalArgumentException("非法文件名");
        }
        return resolved;
    }
}
