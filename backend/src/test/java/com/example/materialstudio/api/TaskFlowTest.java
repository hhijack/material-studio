package com.example.materialstudio.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.materialstudio.support.Api;
import com.example.materialstudio.support.BaseIntegrationTest;
import com.example.materialstudio.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 任务受理 → 三步执行 → 成功结果（R-04、R-05 主链路）。
 * 测试环境为 sync 执行：受理返回时任务已完成，可直接断言终态；
 * 步骤顺序与失败中断的更细粒度行为由 TaskEngineTest（纯单测）覆盖。
 */
class TaskFlowTest extends BaseIntegrationTest {

    @Test
    @DisplayName("受理后立即返回 taskId；完成后三阶段全部 PASSED、状态 SUCCESS 且结果可预览")
    void submitAndComplete() throws Exception {
        String session = Api.session(mockMvc);
        JsonNode a1 = Api.upload(mockMvc, session, "travel-postcard", TestImages.PNG_1PX, "a.png", "image/png");
        JsonNode a2 = Api.upload(mockMvc, session, "travel-postcard", TestImages.PNG_1PX, "b.png", "image/png");
        assertThat(a1.path("url").asText()).isEqualTo("/api/files/uploads/" + a1.path("id").asText());

        String taskId = Api.createTask(mockMvc, session, "travel-postcard",
                java.util.List.of(a1.path("id").asText(), a2.path("id").asText()));
        assertThat(taskId).isNotBlank();

        JsonNode detail = Api.taskDetail(mockMvc, session, taskId);
        assertThat(detail.path("task").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(detail.path("task").path("failStage").isNull()).isTrue();

        JsonNode steps = detail.path("steps");
        assertThat(steps).hasSize(3);
        assertThat(steps.get(0).path("stage").asText()).isEqualTo("MATERIAL_CHECK");
        assertThat(steps.get(0).path("status").asText()).isEqualTo("PASSED");
        assertThat(steps.get(1).path("stage").asText()).isEqualTo("SIMULATE_GENERATE");
        assertThat(steps.get(1).path("status").asText()).isEqualTo("PASSED");
        assertThat(steps.get(2).path("stage").asText()).isEqualTo("RESULT_FINALIZE");
        assertThat(steps.get(2).path("status").asText()).isEqualTo("PASSED");
        // 步骤时间边界完整：started_at / finished_at 齐备
        assertThat(steps.get(0).path("startedAt").isNull()).isFalse();
        assertThat(steps.get(0).path("finishedAt").isNull()).isFalse();

        String resultUrl = detail.path("task").path("resultUrl").asText();
        assertThat(resultUrl).isEqualTo("/api/files/results/" + taskId + ".png");

        // 结果可预览（本人访问 200；返回 PNG）
        mockMvc.perform(get(resultUrl).cookie(new Cookie("MS_SESSION", session)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        // 历史列表包含该任务（带模板标题与封面）
        JsonNode mine = Api.taskList(mockMvc, session);
        JsonNode match = null;
        for (JsonNode item : mine) {
            if (taskId.equals(item.path("id").asText())) {
                match = item;
            }
        }
        assertThat(match).as("历史列表应包含刚完成的任务").isNotNull();
        assertThat(match.path("templateTitle").asText()).isEqualTo("旅行明信片");
        assertThat(match.path("templateCoverUrl").asText()).isNotEmpty();
        assertThat(match.path("createdAt").isNull()).isFalse();
    }
}
