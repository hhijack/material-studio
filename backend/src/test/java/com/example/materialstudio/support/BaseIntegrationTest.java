package com.example.materialstudio.support;

import com.example.materialstudio.service.SimConfigService;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试基类：需要真实 MySQL/MariaDB。
 * 在 Spring 上下文创建之前用 JDBC 探测数据库；不可用时整类跳过（而非失败），
 * 保证无 DB 环境下 `mvn test` 仍可执行单元测试。CI 中配置了 mysql:8 service，集成测试必然运行。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private SimConfigService simConfigService;

    @BeforeAll
    static void requireDatabaseOrSkip() {
        String url = System.getenv().getOrDefault("TEST_MYSQL_URL",
                "jdbc:mysql://127.0.0.1:3306/material_studio_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");
        String user = System.getenv().getOrDefault("TEST_MYSQL_USERNAME", "root");
        String password = System.getenv().getOrDefault("TEST_MYSQL_PASSWORD", "root");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Assumptions.assumeTrue(conn.isValid(3), "数据库连接无效");
        } catch (Exception e) {
            Assumptions.abort("未检测到可用的 MySQL/MariaDB（设置 TEST_MYSQL_URL/TEST_MYSQL_USERNAME/"
                    + "TEST_MYSQL_PASSWORD 或启动本地数据库后重跑集成测试）：" + e.getMessage());
        }
    }

    @BeforeEach
    void resetSimBaseline() {
        // 每个用例前的已知基线：短耗时、不注入失败（避免上次运行残留影响）
        simConfigService.update(30, 30, 30, null);
    }
}
