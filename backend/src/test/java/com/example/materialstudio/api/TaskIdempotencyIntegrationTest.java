package com.example.materialstudio.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.materialstudio.support.Api;
import com.example.materialstudio.support.BaseIntegrationTest;
import com.example.materialstudio.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 受理幂等（R4 新增）：携带相同 Idempotency-Key 的重复提交返回已受理的 taskId。 */
class TaskIdempotencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearIdempotencyTable() {
        // 测试库跨运行持久化：清掉历史幂等键，避免与上轮运行（旧随机用户）的记录冲突
        jdbc.update("DELETE FROM task_idempotency");
    }

    private String postWithKey(String session, String key) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content("{\"templateId\":\"pet-avatar\",\"assetIds\":[\""
                                + uploadedAssetId(session) + "\"]}")
                        .cookie(new Cookie("MS_SESSION", session)))
                .andReturn();
        return Api.json().readTree(result.getResponse().getContentAsString()).path("taskId").asText();
    }

    private String uploadedAssetId(String session) throws Exception {
        JsonNode asset = Api.upload(mockMvc, session, "pet-avatar", TestImages.PNG_1PX, "pet.png", "image/png");
        return asset.path("id").asText();
    }

    @Test
    @DisplayName("相同 Idempotency-Key 的重试提交返回同一 taskId，不产生重复任务")
    void sameKeyReturnsSameTask() throws Exception {
        String session = Api.session(mockMvc);
        String t1 = postWithKey(session, "retry-key-001");
        assertThat(t1).isNotBlank();

        // 模拟网络重试：同 key 再次提交
        String t2 = postWithKey(session, "retry-key-001");
        assertThat(t2).isEqualTo(t1);

        // 历史中该 key 只对应一个任务
        JsonNode list = Api.taskList(mockMvc, session);
        long count = 0;
        for (JsonNode item : list) {
            if (t1.equals(item.path("id").asText())) count++;
        }
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("不同 Idempotency-Key 视为不同提交")
    void differentKeysCreateDifferentTasks() throws Exception {
        String session = Api.session(mockMvc);
        String t1 = postWithKey(session, "key-A");
        String t2 = postWithKey(session, "key-B");
        assertThat(t2).isNotEqualTo(t1);
    }
}
