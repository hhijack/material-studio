package com.example.materialstudio.service;

import com.example.materialstudio.config.SimProperties;
import com.example.materialstudio.mapper.SimConfigMapper;
import com.example.materialstudio.model.SimConfig;
import com.example.materialstudio.model.Stage;
import com.example.materialstudio.web.ApiException;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 模拟配置：properties 提供初始值，sim_config 表为运行期唯一真源。 */
@Service
public class SimConfigService {

    private final SimConfigMapper mapper;
    private final SimProperties props;

    public SimConfigService(SimConfigMapper mapper, SimProperties props) {
        this.mapper = mapper;
        this.props = props;
    }

    /** 启动时用 properties 初始化（幂等，不覆盖运行期修改）。 */
    @PostConstruct
    void initIfAbsent() {
        if (mapper.find() == null) {
            mapper.upsert(props.materialCheckMs(), props.simulateGenerateMs(), props.resultFinalizeMs(),
                    Stage.parse(props.failStage()), LocalDateTime.now());
        }
    }

    public SimConfig current() {
        SimConfig config = mapper.find();
        if (config != null) {
            return config;
        }
        return new SimConfig(props.materialCheckMs(), props.simulateGenerateMs(), props.resultFinalizeMs(),
                Stage.parse(props.failStage()));
    }

    public SimConfig update(long materialCheckMs, long simulateGenerateMs, long resultFinalizeMs, Stage failStage) {
        validateMs(materialCheckMs, "materialCheckMs");
        validateMs(simulateGenerateMs, "simulateGenerateMs");
        validateMs(resultFinalizeMs, "resultFinalizeMs");
        mapper.upsert(materialCheckMs, simulateGenerateMs, resultFinalizeMs, failStage, LocalDateTime.now());
        return current();
    }

    private void validateMs(long value, String field) {
        if (value < SimConfig.MIN_MS || value > SimConfig.MAX_MS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SIM_CONFIG",
                    field + " 必须在 " + SimConfig.MIN_MS + " ~ " + SimConfig.MAX_MS + " 毫秒之间");
        }
    }
}
