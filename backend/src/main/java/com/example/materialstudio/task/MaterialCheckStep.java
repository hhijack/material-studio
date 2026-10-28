package com.example.materialstudio.task;

import com.example.materialstudio.model.Asset;
import com.example.materialstudio.model.Stage;
import com.example.materialstudio.model.Template;
import com.example.materialstudio.service.StorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/** 第 1 步：素材检查——复核文件仍存在、大小一致、类型符合模板要求。 */
@Component
public class MaterialCheckStep implements Step {

    private final StorageService storage;

    public MaterialCheckStep(StorageService storage) {
        this.storage = storage;
    }

    @Override
    public Stage stage() {
        return Stage.MATERIAL_CHECK;
    }

    @Override
    public void execute(TaskContext ctx) throws TaskStepException {
        Template template = ctx.template();
        for (Asset asset : ctx.assets()) {
            if (!template.acceptedMimeTypes().contains(asset.mimeType())) {
                throw new TaskStepException("素材检查失败：" + asset.originalName()
                        + " 的类型 " + asset.mimeType() + " 不在模板支持的类型列表内");
            }
            Path path = storage.resolveUpload(asset.storedName());
            if (!Files.isRegularFile(path)) {
                throw new TaskStepException("素材检查失败：文件 " + asset.originalName() + " 在服务器上已不存在");
            }
            try {
                if (Files.size(path) != asset.sizeBytes()) {
                    throw new TaskStepException("素材检查失败：文件 " + asset.originalName()
                            + " 的大小与上传记录不一致");
                }
            } catch (IOException e) {
                throw new TaskStepException("素材检查失败：无法读取文件 " + asset.originalName());
            }
        }
        ctx.requireNoInjectedFailure(stage());
    }
}
