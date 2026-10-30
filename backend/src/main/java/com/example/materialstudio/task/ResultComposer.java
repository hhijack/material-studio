package com.example.materialstudio.task;

import com.example.materialstudio.model.Asset;
import com.example.materialstudio.model.Template;
import com.example.materialstudio.service.StorageService;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * 用 java.awt 合成一张「结果卡」PNG：渐变底 + 用户上传图 + 模板标题。
 * 纯本地模拟，不依赖外部服务；文案绘制失败（容器缺字体等）只降级不失败。
 */
@Component
public class ResultComposer {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StorageService storage;

    public ResultComposer(StorageService storage) {
        this.storage = storage;
    }

    public String compose(String taskId, Template template, List<Asset> assets) throws TaskStepException {
        try {
            int w = 960;
            int h = 600;
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            int hue = Math.abs(template.id().hashCode()) % 360;
            g.setPaint(new java.awt.GradientPaint(0, 0,
                    java.awt.Color.getHSBColor(hue / 360f, 0.55f, 0.35f), w, h,
                    java.awt.Color.getHSBColor((hue + 40f) / 360f, 0.45f, 0.18f)));
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // 粘贴第一张上传图（读不出来则只画占位框，不影响结果产出）
            int photoW = 520;
            int photoH = 380;
            int photoX = (w - photoW) / 2;
            int photoY = 90;
            BufferedImage photo = readFirstPhoto(assets);
            if (photo != null) {
                g.drawImage(scaleCover(photo, photoW, photoH), photoX, photoY, null);
            } else {
                g.setColor(new java.awt.Color(255, 255, 255, 40));
                g.fill(new RoundRectangle2D.Float(photoX, photoY, photoW, photoH, 16, 16));
            }
            g.setColor(new java.awt.Color(255, 255, 255, 220));
            g.draw(new RoundRectangle2D.Float(photoX - 4, photoY - 4, photoW + 8, photoH + 8, 18, 18));

            try {
                Font titleFont = resolveTitleFont();
                g.setColor(java.awt.Color.WHITE);
                g.setFont(g.getFont().deriveFont(java.awt.Font.PLAIN, 18f));
                g.setColor(new java.awt.Color(255, 255, 255, 200));
                g.drawString("Material Studio simulated result", photoX, photoY + photoH + 86);
                g.drawString(TS.format(LocalDateTime.now()), photoX + photoW - 180, photoY + photoH + 56);
                if (titleFont != null) {
                    g.setFont(titleFont.deriveFont(java.awt.Font.BOLD, 34f));
                    g.setColor(java.awt.Color.WHITE);
                    g.drawString(template.title(), photoX, photoY + photoH + 56);
                }
            } catch (Exception fontError) {
                // 容器缺字体时跳过文案，保证结果图仍能产出
            }
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            storage.saveResult(taskId + ".png", out.toByteArray());
            return "/api/files/results/" + taskId + ".png";
        } catch (Exception e) {
            throw new TaskStepException("结果整理失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private BufferedImage readFirstPhoto(List<Asset> assets) {
        for (Asset asset : assets) {
            try (InputStream in = new ByteArrayInputStream(readAllBytes(storage.resolveUpload(asset.storedName())))) {
                BufferedImage photo = ImageIO.read(in);
                if (photo != null) {
                    return photo;
                }
            } catch (Exception ignored) {
                // 尝试下一张
            }
        }
        return null;
    }

    private byte[] readAllBytes(java.nio.file.Path path) throws IOException {
        return java.nio.file.Files.readAllBytes(path);
    }

    /**
     * 选择能渲染中文的字体：优先常见 CJK 字体；全部缺失时返回 null（调用方改用英文标题），
     * 避免容器缺 CJK 字体时中文画成方框。容器镜像已内置 fonts-noto-cjk，正常返回非 null。
     */
    private Font resolveTitleFont() {
        String sample = "中文渲染测试";
        String[] preferred = {"Noto Sans CJK SC", "Noto Sans CJK JP", "Noto Sans CJK",
                "WenQuanYi Zen Hei", "Source Han Sans SC", "Microsoft YaHei", "PingFang SC"};
        for (String name : preferred) {
            Font candidate = new Font(name, Font.PLAIN, 12);
            if (candidate.canDisplayUpTo(sample) == -1) {
                return candidate;
            }
        }
        Font logical = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        return logical.canDisplayUpTo(sample) == -1 ? logical : null;
    }

    /** 等比裁剪为 cover 填充目标尺寸。 */
    private BufferedImage scaleCover(BufferedImage src, int w, int h) {
        double scale = Math.max(w / (double) src.getWidth(), h / (double) src.getHeight());
        int sw = (int) Math.round(src.getWidth() * scale);
        int sh = (int) Math.round(src.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, sw, sh, null);
        g.dispose();
        int x = Math.max(0, (sw - w) / 2);
        int y = Math.max(0, (sh - h) / 2);
        return scaled.getSubimage(Math.min(x, sw - w), Math.min(y, sh - h), w, h);
    }
}
