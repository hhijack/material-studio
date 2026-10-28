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

### I-4 上传进度条实现（CLOSED）
- 结论：XMLHttpRequest `upload.onprogress` 实现（fetch 不支持上传进度），前端上传位显示百分比进度条；实测正常。

### I-5 服务重启后任务恢复（题目明确范围外）（ACCEPTED，已实机验证其表现）
- 说明：进程被强杀时 WAITING/PROCESSING 任务停在原状态（实测强杀后任务保持「处理中·模拟生成」，不自动续跑也不会误标失败）；优雅关闭（SIGTERM）时 @PreDestroy 等待工作线程 10s，正在执行的任务大概率能跑完（实测一次优雅重启中任务正常完成落库）。
- 领域扩展讨论（README 亦有）：可用「启动时把 PROCESSING 扫回 WAITING 重新入队」实现恢复，但中断步骤的幂等性（结果文件可能已写）需要引入步骤级断点，未实现。

### I-6 查询恢复后 connectionError 未清除（CLOSED，2026-10-29 01:10）
- 发现：V-007 浏览器走查中断连→恢复后，任务已恢复查询但顶栏「查询异常」横幅不消失。
- 根因：taskSync 的 apply 回调只合并数据未清错误标志。
- 修复：apply 成功路径加 `setConnectionError(null)`；复验横幅自动消失。taskStore 单测对「恢复清除」行为已有覆盖。

### I-7 同步执行模式下引擎在外层事务内运行（CLOSED，2026-10-29 00:55）
- 发现：V-005 集成测试首跑 Lock wait timeout——sync 模式在 `create()` 未提交事务内执行引擎，REQUIRES_NEW 的进度更新被行锁阻塞。
- 修复：`worker.submit` 注册到 `TransactionSynchronization.afterCommit()`，任务行先提交再执行；async 模式语义不变。

### I-8 CI 无法在本机触发（ACCEPTED）
- 说明：无 GitHub 远端仓库（交付为源码包/本地目录），Actions 不会在本机运行。
- 处置：CI 工作流按官方 actions（checkout@v4 / setup-node@v4 / setup-java@v4）+ 服务容器标准写法编写并人工复核；其每一步命令（npm ci/lint/typecheck/test/build、mvn test with MySQL）均已在本机以等效方式跑通（V-003、V-005）。若验收方 push 到 GitHub，工作流应直接可用。
