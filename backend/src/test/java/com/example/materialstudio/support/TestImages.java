package com.example.materialstudio.support;

import java.util.Base64;

/** 测试用图片数据。 */
public final class TestImages {

    private TestImages() {
    }

    /** 1x1 合法 PNG。 */
    public static final byte[] PNG_1PX = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    /** 带合法 PNG 头的填充数据（用于超大文件校验）。 */
    public static byte[] oversizedPng(int bytes) {
        byte[] data = new byte[bytes];
        System.arraycopy(PNG_1PX, 0, data, 0, Math.min(PNG_1PX.length, data.length));
        return data;
    }
}
