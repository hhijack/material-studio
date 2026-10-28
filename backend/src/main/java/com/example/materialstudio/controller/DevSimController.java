package com.example.materialstudio.controller;

import com.example.materialstudio.config.SimProperties;
import com.example.materialstudio.model.SimConfig;
import com.example.materialstudio.model.Stage;
import com.example.materialstudio.service.SimConfigService;
import com.example.materialstudio.web.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发/演示用：查询与调整模拟配置（各阶段耗时、注入失败阶段）。
 * failStage 传 null 表示不注入失败。对之后创建的任务生效。
 */
@RestController
@RequestMapping("/api/dev")
public class DevSimController {

    public record UpdateSimConfigRequest(
            @NotNull @Min(0) @Max(60000) Long materialCheckMs,
            @NotNull @Min(0) @Max(60000) Long simulateGenerateMs,
            @NotNull @Min(0) @Max(60000) Long resultFinalizeMs,
            Stage failStage) {
    }

    private final SimConfigService simConfigService;
    private final boolean devApiEnabled;

    public DevSimController(SimConfigService simConfigService, SimProperties props) {
        this.simConfigService = simConfigService;
        this.devApiEnabled = props.devApiEnabled();
    }

    @GetMapping("/sim-config")
    public SimConfig get() {
        requireEnabled();
        return simConfigService.current();
    }

    @PutMapping("/sim-config")
    public SimConfig update(@Valid @RequestBody UpdateSimConfigRequest request) {
        requireEnabled();
        return simConfigService.update(request.materialCheckMs(), request.simulateGenerateMs(),
                request.resultFinalizeMs(), request.failStage());
    }

    private void requireEnabled() {
        if (!devApiEnabled) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DEV_API_DISABLED", "开发接口未启用");
        }
    }
}
