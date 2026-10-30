package com.example.materialstudio.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.materialstudio.service.TaskRecoveryService;
import com.example.materialstudio.support.Api;
import com.example.materialstudio.support.BaseIntegrationTest;
import com.example.materialstudio.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 启动恢复（R4 新增）：重启后未完成任务重置重跑。测试环境为 sync 模式，恢复调用返回时任务已完成。 */
class TaskRecoveryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TaskRecoveryService recoveryService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("任务停在 PROCESSING（模拟崩溃）→ 恢复重置 WAITING → 重跑至 SUCCESS")
    void unfinishedTaskRecoveredAndRerun() throws Exception {
        String session = Api.session(mockMvc);
        JsonNode a1 = Api.upload(mockMvc, session, "pet-avatar", TestImages.PNG_1PX, "pet.png", "image/png");
        String taskId = Api.createTask(mockMvc, session, "pet-avatar", java.util.List.of(a1.path("id").asText()));

        // 模拟"进程曾在此任务运行中崩溃"：任务卡在 PROCESSING，步骤卡在 RUNNING
        jdbc.update("UPDATE task SET status='PROCESSING', current_stage='SIMULATE_GENERATE' WHERE id=?", taskId);
        jdbc.update("UPDATE task_step SET status='RUNNING', started_at=NOW(3) WHERE task_id=? AND stage='SIMULATE_GENERATE'", taskId);

        recoveryService.recoverOnStartup(); // sync 模式：调用返回时任务已重跑完成

        JsonNode detail = Api.taskDetail(mockMvc, session, taskId);
        assertThat(detail.path("task").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(detail.path("task").path("resultUrl").asText()).isNotBlank();
        assertThat(detail.path("steps").get(1).path("status").asText()).isEqualTo("PASSED");
        // 恢复前的中间状态痕迹被清干净
        assertThat(detail.path("steps").get(0).path("status").asText()).isEqualTo("PASSED");
    }

    @Test
    @DisplayName("无未完成任务时恢复为无操作")
    void recoveryIsNoopWhenNothingStuck() {
        recoveryService.recoverOnStartup(); // 不抛异常即可
    }
}
