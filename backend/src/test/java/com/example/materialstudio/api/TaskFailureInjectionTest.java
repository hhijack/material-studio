package com.example.materialstudio.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.materialstudio.support.Api;
import com.example.materialstudio.support.BaseIntegrationTest;
import com.example.materialstudio.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** 失败注入：可重复触发指定阶段失败，失败即停（R-06）。 */
class TaskFailureInjectionTest extends BaseIntegrationTest {

    private void setSimConfig(String session, String failStage) throws Exception {
        String body = "{\"materialCheckMs\":30,\"simulateGenerateMs\":30,\"resultFinalizeMs\":30,\"failStage\":"
                + (failStage == null ? "null" : "\"" + failStage + "\"") + "}";
        MvcResult result = mockMvc.perform(put("/api/dev/sim-config")
                        .contentType("application/json")
                        .content(body)
                        .cookie(new Cookie("MS_SESSION", session)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new IllegalStateException("设置模拟配置失败：" + result.getResponse().getContentAsString());
        }
    }

    @Test
    @DisplayName("注入 SIMULATE_GENERATE 失败：该阶段 FAILED、后续阶段 PENDING、任务 FAILED 且带原因")
    void failAtSimulateGenerate() throws Exception {
        String session = Api.session(mockMvc);
        setSimConfig(session, "SIMULATE_GENERATE");

        JsonNode a1 = Api.upload(mockMvc, session, "art-sketch", TestImages.PNG_1PX, "a.png", "image/png");
        String taskId = Api.createTask(mockMvc, session, "art-sketch",
                java.util.List.of(a1.path("id").asText()));

        JsonNode detail = Api.taskDetail(mockMvc, session, taskId);
        assertThat(detail.path("task").path("status").asText()).isEqualTo("FAILED");
        assertThat(detail.path("task").path("failStage").asText()).isEqualTo("SIMULATE_GENERATE");
        assertThat(detail.path("task").path("failReason").asText())
                .contains("模拟生成", "模拟故障注入");
        assertThat(detail.path("task").path("resultUrl").isNull()).isTrue();

        JsonNode steps = detail.path("steps");
        assertThat(steps.get(0).path("status").asText()).isEqualTo("PASSED");
        assertThat(steps.get(1).path("status").asText()).isEqualTo("FAILED");
        assertThat(steps.get(2).path("status").asText()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("重置注入后新任务可正常成功（可重复触发/可恢复）")
    void resetAndSucceedAgain() throws Exception {
        String session = Api.session(mockMvc);
        setSimConfig(session, "MATERIAL_CHECK");
        JsonNode a1 = Api.upload(mockMvc, session, "art-sketch", TestImages.PNG_1PX, "a.png", "image/png");
        String failedId = Api.createTask(mockMvc, session, "art-sketch", java.util.List.of(a1.path("id").asText()));
        assertThat(Api.taskDetail(mockMvc, session, failedId).path("task").path("failStage").asText())
                .isEqualTo("MATERIAL_CHECK");

        setSimConfig(session, null);
        String okId = Api.createTask(mockMvc, session, "art-sketch", java.util.List.of(a1.path("id").asText()));
        JsonNode detail = Api.taskDetail(mockMvc, session, okId);
        assertThat(detail.path("task").path("status").asText()).isEqualTo("SUCCESS");
        // 失败的任务保持失败，不因后续查询或新任务而改变
        assertThat(Api.taskDetail(mockMvc, session, failedId).path("task").path("status").asText())
                .isEqualTo("FAILED");
    }

    @Test
    @DisplayName("非法模拟配置返回 400")
    void invalidSimConfigRejected() throws Exception {
        String session = Api.session(mockMvc);
        mockMvc.perform(put("/api/dev/sim-config")
                        .contentType("application/json")
                        .content("{\"materialCheckMs\":-1,\"simulateGenerateMs\":30,\"resultFinalizeMs\":30}")
                        .cookie(new Cookie("MS_SESSION", session)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }
}
