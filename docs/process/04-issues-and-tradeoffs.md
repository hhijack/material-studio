# 04 问题、未完成项、已知限制与取舍

> 随进度追加。每项标注状态：OPEN / MITIGATED / CLOSED / ACCEPTED。

## 环境

### I-1 本机无 Docker，`docker compose up --build` 无法在本机实跑（OPEN）
- 影响：R-14 的 compose 配置本机无法端到端验证。
- 缓解：compose 使用最常规的模式（mysql:8.4 官方镜像 + 标准初始化挂载 + 两个多阶段 Dockerfile），Dockerfile 内的构建步骤（maven package / npm build）在本地单独验证等效命令；db/init SQL 在本地 MariaDB 实跑验证；README 给出验收环境的一键运行与逐组件运行两种说明。
- 残留：compose 网络编排本身未实跑验证，以 CI + 本地等效验证为准。状态：ACCEPTED（环境硬限制，题目允许记录）。

### I-2 本机无 MySQL，无法直装（无 sudo）（CLOSED，方案=本地 MariaDB）
- 影响：集成测试与 SQL 验证需要真实 DB。
- 处置：下载 MariaDB 11.4.5 官方 bintar 到用户目录运行（无 root 依赖），schema SQL 以 MySQL 8 语法书写并在 MariaDB 上验证；CI 中用 mysql:8 service 容器跑同一套 SQL 与集成测试。已知差异与风险见 I-3。

### I-3 MariaDB 验证 ≠ MySQL 验证（ACCEPTED）
- 影响：极少数 MySQL 专有语法/行为无法在本机复现。
- 缓解：schema 仅用双方一致的特性（BIGINT AUTO_INCREMENT、VARCHAR、DATETIME(3)、TEXT、普通索引），不使用 CTE 特性、窗口函数等方言敏感语法于关键路径；CI 在真 MySQL 8 上跑全量集成测试兜底。

## 待补问题

### I-4 （M2 上传进度条：fetch 不支持上传进度，需 XHR）（OPEN）
- 计划：XMLHttpRequest 实现 upload.onprogress；若时间紧可退化为「上传中」状态展示（仍满足 R-07 状态区分）。

### I-5 服务重启后任务恢复（题目明确范围外）
- 说明：进程重启时 WAITING/PROCESSING 任务不会自动续跑（重启前卡在 PROCESSING）。按题目范围外处理，README 说明；DB 中状态保持真实（PROCESSING 记录仍可见，不会自动变成功/失败）。
