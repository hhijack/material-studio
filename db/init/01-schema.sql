-- Material Studio 建表脚本（MySQL 8 / 兼容 MariaDB 11）
-- 与 backend/src/main/resources/db/init/01-schema.sql 保持同一文件（Spring Boot 启动时也会执行，幂等）

CREATE TABLE IF NOT EXISTS `user` (
    `id`         CHAR(36)    NOT NULL,
    `nickname`   VARCHAR(64) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `template` (
    `id`                   VARCHAR(64)   NOT NULL COMMENT '模板标识（slug）',
    `title`                VARCHAR(128)  NOT NULL,
    `category`             VARCHAR(64)   NOT NULL,
    `cover_url`            VARCHAR(512)  NOT NULL,
    `description`          VARCHAR(1024) NOT NULL,
    `required_image_count` INT           NOT NULL COMMENT '要求的图片数量',
    `accepted_mime_types`  JSON          NOT NULL COMMENT '接受的 MIME 类型数组',
    `max_file_size_bytes`  BIGINT        NOT NULL,
    `sort_order`           INT           NOT NULL DEFAULT 0,
    `created_at`           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_template_category` (`category`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `asset` (
    `id`            CHAR(36)      NOT NULL,
    `user_id`       CHAR(36)      NOT NULL,
    `template_id`   VARCHAR(64)   NULL,
    `original_name` VARCHAR(255)  NOT NULL,
    `stored_name`   VARCHAR(128)  NOT NULL COMMENT '本地存储文件名（UUID）',
    `mime_type`     VARCHAR(128)  NOT NULL,
    `size_bytes`    BIGINT        NOT NULL,
    `created_at`    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_asset_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task` (
    `id`            CHAR(36)      NOT NULL,
    `user_id`       CHAR(36)      NOT NULL,
    `template_id`   VARCHAR(64)   NOT NULL,
    `status`        VARCHAR(16)   NOT NULL COMMENT 'WAITING/PROCESSING/SUCCESS/FAILED',
    `current_stage` VARCHAR(32)   NULL COMMENT '当前阶段 MATERIAL_CHECK/SIMULATE_GENERATE/RESULT_FINALIZE',
    `fail_stage`    VARCHAR(32)   NULL COMMENT '失败阶段',
    `fail_reason`   VARCHAR(1024) NULL,
    `result_url`    VARCHAR(512)  NULL COMMENT '成功结果可预览地址',
    `created_at`    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `started_at`    DATETIME(3)   NULL,
    `finished_at`   DATETIME(3)   NULL,
    PRIMARY KEY (`id`),
    KEY `idx_task_user_created` (`user_id`, `created_at`),
    KEY `idx_task_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_asset` (
    `task_id`    CHAR(36) NOT NULL,
    `asset_id`   CHAR(36) NOT NULL,
    `sort_order` INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`task_id`, `asset_id`),
    KEY `idx_task_asset_asset` (`asset_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_step` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `task_id`     CHAR(36)      NOT NULL,
    `step_index`  INT           NOT NULL,
    `stage`       VARCHAR(32)   NOT NULL COMMENT 'MATERIAL_CHECK/SIMULATE_GENERATE/RESULT_FINALIZE',
    `status`      VARCHAR(16)   NOT NULL COMMENT 'PENDING/RUNNING/PASSED/FAILED',
    `message`     VARCHAR(1024) NULL,
    `started_at`  DATETIME(3)   NULL,
    `finished_at` DATETIME(3)   NULL,
    PRIMARY KEY (`id`),
    KEY `idx_step_task` (`task_id`, `step_index`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `sim_config` (
    `id`                  TINYINT     NOT NULL,
    `material_check_ms`   INT         NOT NULL,
    `simulate_generate_ms` INT        NOT NULL,
    `result_finalize_ms`  INT         NOT NULL,
    `fail_stage`          VARCHAR(32) NULL COMMENT '注入失败阶段；NULL=不失败',
    `updated_at`          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
