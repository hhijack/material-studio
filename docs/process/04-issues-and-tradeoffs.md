# 04 问题、未完成项、已知限制与取舍

> 随进度追加。每项标注状态：OPEN / MITIGATED / CLOSED / ACCEPTED。

## 环境

### I-1 本机无 Docker，`docker compose up --build` 无法在本机实跑（CLOSED @ R4，2026-10-31）
- 历史状态：OPEN——compose 本地不可验证，以"标准模式 + 等效命令验证 + 对抗性审查修复（V-010/V-011）"缓解（保留在 04-I-1 更新条目）。
- **R4 关闭**：CI 新增 `compose-e2e` job（V-013），在 GitHub runner 的真实 Docker 环境中 `docker compose up --build` 启动三容器栈，端到端验证成功路径与失败注入——compose 编排从"配置留痕"升级为"每次 push 实跑验证"。交付限制不再存在。

### I-2 本机无 MySQL，无法直装（无 sudo）（CLOSED，方案=本地 MariaDB）
- 影响：集成测试与 SQL 验证需要真实 DB。
- 处置：下载 MariaDB 11.4.5 官方 bintar 到用户目录运行（无 root 依赖），schema SQL 以 MySQL 8 语法书写并在 MariaDB 上验证；CI 中用 mysql:8 service 容器跑同一套 SQL 与集成测试。已知差异与风险见 I-3。

### I-3 MariaDB 验证 ≠ MySQL 验证（ACCEPTED）
- 影响：极少数 MySQL 专有语法/行为无法在本机复现。
- 缓解：schema 仅用双方一致的特性（BIGINT AUTO_INCREMENT、VARCHAR、DATETIME(3)、TEXT、普通索引），不使用 CTE 特性、窗口函数等方言敏感语法于关键路径；CI 在真 MySQL 8 上跑全量集成测试兜底。

## R4 收尾增强轮（2026-10-31，验收质询"这些真的解决不了？"驱动）

### I-10 R4 实现与取舍清单
- **启动恢复（原 I-5）**：`app.recovery.on-startup`（默认 true），启动时 WAITING/PROCESSING 重置重跑。三步全程可重入（检查只读/生成无产物/结果覆盖写），无需步骤级断点。集成测试 2 例。生产可关。
- **受理幂等**：`POST /api/tasks` 支持 `Idempotency-Key` 头 + `task_idempotency` 表（key 唯一）。同 key 重试返回原 taskId；**并发窗口取舍**：前置查询 + 提交后注册，同毫秒双发不去重（严格串行化需唯一约束+捕获重试），前端已防重复点击、重试场景为主，README §8 说明。
- **Cookie HMAC 签名**：`MS_SESSION` = userId.HMAC-SHA256；伪造/错签名 401（集成测试）。secret 未配置时随机生成（重启失效），compose 给出示例值。
- **CJK 字体**：backend 镜像内置 fonts-noto-cjk + ResultComposer.canDisplay 回退英文标题——原"方框"限制消除。
- **compose e2e**：CI `compose-e2e` job 实跑 `docker compose up --build` + 端到端冒烟（成功路径 + 失败注入），04-I-1 关闭。
- **保持的取舍**：单线程执行器（题目允许）；上传深度解码校验（webp 需额外解码器，魔数校验已拦声明不一致）。

## 待补问题

### I-4 上传进度条实现（CLOSED）
- 结论：XMLHttpRequest `upload.onprogress` 实现（fetch 不支持上传进度），前端上传位显示百分比进度条；实测正常。

### I-5 服务重启后任务恢复（题目明确范围外）（CLOSED @ R4——超出题目要求的加分实现）
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

## R2 验收轮发现的问题（2026-10-29）

### I-9 用户验收提出的问题清单与处置
- **P1-1 受理成功但列表刷新失败 → 误报「提交失败」且新任务失联**（CLOSED，V-009）
  - 修复：抽出 `createAndTrackTask`（taskSync.ts），createTask 成功即受理成功（启动 tracker、清上传位）；列表刷新失败走 connectionError 通道；仅受理失败才显示提交错误并保留上传位。新增组件测试 2 例。
- **P2-1 请求无超时 + 轮询重叠堆积**（CLOSED，V-009）
  - 修复：`api.request` 加 8s `AbortSignal.timeout`（上传 XHR 除外）；`taskTracker.pollOnce` 加 in-flight 互斥。新增 tracker 用例。
- **P2-2 bootError 无渲染入口**（CLOSED，V-009）
  - 修复：TopBar 增加「连接服务失败 + 重试」横幅（重试=重新 bootstrapSession，可重入）。新增 TopBar 测试 2 例。
- **P2-3 详情页悬挂上传 setState**（CLOSED，V-009）
  - 修复：上传位 AbortController，组件卸载统一 abort；ABORTED 分支复位上传位。
- **P3-1 markProcessing 无 WHERE 防御**（CLOSED，V-009）：加 `AND status='WAITING'` + 受影响行数检查；记录于 01-D6。
- **P3-2 会话 Cookie 为未签名 UUID、可伪造身份**（ACCEPTED）：匿名轻量方案的有意取舍，已在 README §8 已知限制中写明与收紧路径。

### I-1 更新（R3 对抗性审查，2026-10-29 02:10）
- 对抗性审查发现 compose 的确定性缺陷：前端 `BACKEND_ORIGIN` 原设计为运行期环境变量，但 Next.js standalone 的 rewrites 目标在构建期烘焙，验收环境 `docker compose up --build` 必然出现「前端容器内 500、连不上后端」。已改为构建参数注入（frontend/Dockerfile ARG + compose build.args），本地完整模拟验证通过（V-010）。
- 对抗性审查第二项发现（V-011）：backend 构建上下文 `./backend` 拿不到仓库根 `db/init`，maven-resources 对缺失目录静默跳过 → 容器内 jar 缺建表 SQL → 后端启动即失败。已修复：pom `dbinit.dir` 参数化 + Dockerfile 改仓库根上下文 + compose `context: .` + 根 .dockerignore；注入路径与默认路径均验证通过。
- 残留风险（缩小后不变项）：compose 编排本身（容器网络、healthcheck 时序）仍无法在本机实跑；构建/SQL/反代等确定性环节均已等效验证。状态：ACCEPTED（环境硬限制）。
