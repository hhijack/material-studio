package com.example.materialstudio.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.materialstudio.support.Api;
import com.example.materialstudio.support.BaseIntegrationTest;
import com.example.materialstudio.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/** 查询只读性、完成不回退、身份隔离与提交前校验（R-05、R-07、R-10 服务端部分）。 */
class TaskQueryAndValidationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("已完成的任务被多次查询状态不回退（查询只读，不驱动推进）")
    void completedTaskDoesNotRegress() throws Exception {
        String session = Api.session(mockMvc);
        JsonNode a1 = Api.upload(mockMvc, session, "pet-avatar", TestImages.PNG_1PX, "pet.png", "image/png");
        String taskId = Api.createTask(mockMvc, session, "pet-avatar", java.util.List.of(a1.path("id").asText()));
        for (int i = 0; i < 5; i++) {
            JsonNode detail = Api.taskDetail(mockMvc, session, taskId);
            assertThat(detail.path("task").path("status").asText()).isEqualTo("SUCCESS");
            assertThat(detail.path("task").path("resultUrl").asText()).isNotBlank();
        }
    }

    @Test
    @DisplayName("他人任务不可见：详情 404，历史列表互不串扰")
    void userIsolation() throws Exception {
        String alice = Api.session(mockMvc);
        JsonNode a1 = Api.upload(mockMvc, alice, "pet-avatar", TestImages.PNG_1PX, "pet.png", "image/png");
        String taskId = Api.createTask(mockMvc, alice, "pet-avatar", java.util.List.of(a1.path("id").asText()));

        String bob = mockMvc.perform(get("/api/session")).andExpect(status().isOk())
                .andReturn().getResponse().getCookie("MS_SESSION").getValue();

        mockMvc.perform(get("/api/tasks/" + taskId).cookie(new Cookie("MS_SESSION", bob)))
                .andExpect(status().isNotFound());

        JsonNode bobList = Api.taskList(mockMvc, bob);
        for (JsonNode item : bobList) {
            assertThat(item.path("id").asText()).isNotEqualTo(taskId);
        }
    }

    @Test
    @DisplayName("未建立会话访问任务接口返回 401")
    void requiresSession() throws Exception {
        mockMvc.perform(get("/api/tasks/mine"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content("{\"templateId\":\"pet-avatar\",\"assetIds\":[\"x\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("上传校验：类型不符 / 内容与类型不一致 / 超过大小限制")
    void uploadValidation() throws Exception {
        String session = Api.session(mockMvc);
        Cookie cookie = new Cookie("MS_SESSION", session);

        // 不支持的类型
        MvcResult wrongType = mockMvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("file", "doc.txt", "text/plain", "hello".getBytes()))
                        .param("templateId", "pet-avatar").cookie(cookie))
                .andReturn();
        assertThat(wrongType.getResponse().getStatus()).isEqualTo(400);

        // 声明 image/png 但内容不是图片（魔数不符）
        MvcResult mismatch = mockMvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("file", "fake.png", "image/png", "not-a-png".getBytes()))
                        .param("templateId", "pet-avatar").cookie(cookie))
                .andReturn();
        assertThat(mismatch.getResponse().getStatus()).isEqualTo(400);

        // 超过模板大小限制（pet-avatar 上限 5MB → 上传 6MB）
        MvcResult tooLarge = mockMvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("file", "big.png", "image/png", TestImages.oversizedPng(6 * 1024 * 1024)))
                        .param("templateId", "pet-avatar").cookie(cookie))
                .andReturn();
        assertThat(tooLarge.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("提交校验：数量不符 400、含他人素材 403、模板不存在 404")
    void submitValidation() throws Exception {
        String alice = Api.session(mockMvc);
        String bob = Api.session(mockMvc);
        JsonNode a1 = Api.upload(mockMvc, alice, "travel-postcard", TestImages.PNG_1PX, "a.png", "image/png");
        JsonNode a2 = Api.upload(mockMvc, alice, "travel-postcard", TestImages.PNG_1PX, "b.png", "image/png");
        String asset1 = a1.path("id").asText();
        String asset2 = a2.path("id").asText();

        // 数量不符（模板需要 2 张）
        MvcResult countMismatch = mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content("{\"templateId\":\"travel-postcard\",\"assetIds\":[\"" + asset1 + "\"]}")
                        .cookie(new Cookie("MS_SESSION", alice)))
                .andReturn();
        assertThat(countMismatch.getResponse().getStatus()).isEqualTo(400);

        // 模板不存在
        MvcResult unknownTemplate = mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content("{\"templateId\":\"no-such\",\"assetIds\":[\"" + asset1 + "\"]}")
                        .cookie(new Cookie("MS_SESSION", alice)))
                .andReturn();
        assertThat(unknownTemplate.getResponse().getStatus()).isEqualTo(404);

        // 他人素材
        MvcResult foreign = mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content("{\"templateId\":\"travel-postcard\",\"assetIds\":[\"" + asset1 + "\",\"" + asset2 + "\"]}")
                        .cookie(new Cookie("MS_SESSION", bob)))
                .andReturn();
        assertThat(foreign.getResponse().getStatus()).isEqualTo(403);
    }
}
