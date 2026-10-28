# Material Studio —— 素材制作网站（小影科技 2027 校招 · 全栈开发笔试题）

考生：黄俊恺。项目实现「浏览模板 → 上传图片 → 提交制作任务 → 后台多步骤异步处理 → 查看结果与历史」的完整业务闭环。制作不调用真实 AI，使用**可调耗时、可注入指定阶段失败**的模拟生成器；重点是 Java 后端的多步骤异步处理与前端异步状态管理。

- **验收 commit**：见仓库 tag `acceptance-r3`（`git rev-parse acceptance-r3`；首轮 `acceptance`、二轮 `acceptance-r2` 保留备查）。
- **过程文档**：`docs/process/`（计划/决策/进度/验证/问题/需求追溯六份，全程边做边记）。

---

## 1. 技术版本

| 层 | 技术 | 版本 |
|---|---|---|
| 前端 | Next.js（App Router）+ React + TypeScript + CSS Modules + zustand | Next 15.5.28 / React 19.1.0 / TS 5.x / zustand 5.x |
| 后端 | Java + Spring Boot + MyBatis（Maven 构建） | Java 17 / Spring Boot 3.5.5 / mybatis-spring-boot-starter 3.0.5（MyBatis 3.5.19）/ mysql-connector-j（随 Boot 管理） |
| 数据库 | MySQL | 8.4（compose 用 `mysql:8.4` 官方镜像；开发验证用 MySQL 8.4.11，见 §8） |
| CI | GitHub Actions | 前端 lint/typecheck/test/build + 后端 mvn test/package（mysql:8.4 service 容器） |
| 交付 | Docker Compose | frontend + backend + mysql 一键启动 |

## 2. 项目结构

```
material-studio/
├── frontend/                     # Next.js App Router（静态资源 + /api 反向代理 + 全部页面）
│   ├── src/app/                  #   列表(/) 详情(/templates/[id]) 历史(/history)
│   ├── src/components/           #   TopBar / AccountDialog / UploadSlot / TaskStatusCard / AppBoot
│   ├── src/lib/                  #   api.ts(请求封装) taskStore.ts(任务仓库) taskTracker.ts(轮询生命周期) taskSync.ts(接线) types.ts
│   ├── src/test/                 #   vitest 测试（14 例）
│   ├── scripts/gen-covers.mjs    #   模板封面 SVG 生成脚本（素材来源，可复现）
│   ├── public/demo/covers/       #   6 张本地模板封面（无外链依赖）
│   └── Dockerfile                #   多阶段构建（node:22-alpine，standalone 运行）
├── backend/                      # Spring Boot
│   ├── src/main/java/com/example/materialstudio/
│   │   ├── controller/           #   Session/Template/Upload/Task/File/DevSim/Health
│   │   ├── service/              #   Task/Asset/Template/Session/SimConfig/TaskProgress/TaskWorker/Storage
│   │   ├── task/                 #   TaskEngine（纯 Java 可单测）+ 三步骤实现 + ResultComposer
│   │   ├── mapper/ + resources/mapper/   # MyBatis 接口与 XML
│   │   ├── model/                #   领域模型（record）
│   │   ├── web/                  #   @CurrentUser 解析器、异常体系
│   │   └── config/               #   配置属性、CORS/WebMvc
│   ├── src/main/resources/application.yml
│   ├── src/test/java/            #   JUnit5：引擎单测 + MockMvc 集成测试（17 例）
│   └── Dockerfile                #   多阶段构建（maven 构建 → temurin 17 JRE 运行）
├── db/init/                      # 01-schema.sql + 02-seed.sql（幂等；compose 初始化与后端启动共用同一份）
├── docker-compose.yml            # mysql + backend + frontend 一键启动
├── .github/workflows/ci.yml      # CI
└── docs/process/                 # 过程文档（验收核心）
```

## 3. 构建与运行命令

### 3.1 一键启动（推荐，需 Docker）

```bash
docker compose up --build
# → 前端 http://localhost:3000   后端 http://localhost:8080   MySQL 3306（root/root，库 material_studio）
```

- 首次启动顺序：MySQL 初始化（自动执行 `db/init/*.sql` 建表+种子）→ 健康检查通过 → backend 启动（再次幂等执行同一份 SQL 并初始化模拟配置）→ frontend 启动。
- 注意：前端反代后端地址 `BACKEND_ORIGIN` 是**构建期参数**（Next.js standalone 的 rewrites 目标在构建时烘焙，运行期环境变量不生效），compose 已通过 `build.args` 注入 `http://backend:8080`；如需更改后端地址，修改 compose 中 `frontend.build.args.BACKEND_ORIGIN` 后重新 `--build` 即可（见 04-I-1 / V-010）。
- 数据卷：`mysql-data`（数据库）、`app-data`（上传素材与生成结果，容器内 `/app/data`）。
- 停止/重置：`docker compose down`；`docker compose down -v`（连数据一起清掉）。

### 3.2 本地分组件运行（无 Docker 时）

```bash
# 1) 准备 MySQL 8（本机 3306，root/root），库可自动创建于后端启动时（需先手动建库）：
#    CREATE DATABASE material_studio CHARACTER SET utf8mb4;
# 2) 后端（默认连 jdbc:mysql://localhost:3306/material_studio）
cd backend && mvn spring-boot:run
# 3) 前端（dev 模式，rewrites 把 /api/* 代理到 127.0.0.1:8080）
cd frontend && npm run dev   # http://localhost:3000
```

生产构建：

```bash
cd backend && mvn -q package && java -jar target/material-studio-backend-1.0.0.jar
cd frontend && npm ci && npm run build && npm start
```

### 3.3 测试

```bash
# 后端（无 DB 时自动跳过集成测试，仅跑引擎单测；有 MySQL 时全量）
cd backend && mvn test
# 指定测试库（CI 中由 service 容器提供）
TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/material_studio_test?useSSL=false&allowPublicKeyRetrieval=true' \
TEST_MYSQL_USERNAME=root TEST_MYSQL_PASSWORD=root mvn test

# 前端
cd frontend && npm ci && npm run lint && npm run typecheck && npm test -- --run
```

CI（`.github/workflows/ci.yml`）：push/PR 时并行跑前端四连 + 后端（mysql:8.4 service 容器跑 `mvn test` 后 `mvn package`）。

## 4. 配置项（无真实凭据，均为示例值）

后端 `backend/src/main/resources/application.yml`（全部可用环境变量覆盖，compose 中即用环境变量注入）：

| 配置 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/material_studio?...` | 数据库连接（compose 内为 `jdbc:mysql://mysql:3306/...`） |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | `root` / `root` | 示例凭据（演示项目，无真实凭据） |
| `DATA_DIR` | `./data` | 上传素材与结果目录（compose 内 `/app/data` 卷） |
| `SIM_MATERIAL_CHECK_MS` / `SIM_SIMULATE_GENERATE_MS` / `SIM_RESULT_FINALIZE_MS` | 2000 / 4000 / 2500 | 三阶段模拟耗时（ms），首次启动写入 `sim_config` 表 |
| `SIM_FAIL_STAGE` | 空 | 启动默认注入失败阶段（`MATERIAL_CHECK`/`SIMULATE_GENERATE`/`RESULT_FINALIZE`，空=不注入） |
| `TASK_EXECUTION_MODE` | `async` | `async`=单线程队列后台执行；`sync`=同线程执行（自动化测试用） |
| `app.sim.dev-api-enabled` | `true` | 是否开放 `/api/dev/sim-config` 开发接口 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | 前端直连 Java 时放开跨域（经 Next 代理时不需要） |

数据库初始化：`db/init/01-schema.sql`（7 张表：`user/template/asset/task/task_asset/task_step/sim_config`）与 `02-seed.sql`（6 个模板、4 个分类，幂等 upsert）。**同一份脚本**既挂载给 MySQL 容器首启执行，也在后端每次启动时以 `spring.sql.init` 幂等重放（构建期由 maven-resources 插件拷入 classpath，单一来源无副本）。

模拟数据与素材：模板数据在 `02-seed.sql`；模板封面为本地 SVG（由 `frontend/scripts/gen-covers.mjs` 生成，零外链）；上传图与生成结果存本地数据目录（compose 挂卷），不依赖任何付费/内网服务。

## 5. 主要接口与任务模型

### 5.1 接口一览（全部经 Next 同源代理 `/api/*`，也可 `CORS_ALLOWED_ORIGINS` 放开后直连 8080）

| 方法/路径 | 说明 | 请求 → 响应（主要字段） |
|---|---|---|
| `GET /api/session` | 取/建匿名会话（首访下发 `MS_SESSION` HttpOnly Cookie） | → `{id, nickname, created}` |
| `PATCH /api/session` | 改昵称 | `{nickname}` → `{id, nickname, created}` |
| `DELETE /api/session` | 切换身份（新建用户并重写 Cookie） | → 同上 |
| `GET /api/templates` | 模板列表 | → `[{id,title,category,coverUrl,description,requiredImageCount,acceptedMimeTypes[],maxFileSizeBytes,sortOrder}]` |
| `GET /api/templates/{id}` | 模板详情（不存在 → 404 `{code:"TEMPLATE_NOT_FOUND"}`） | → 单个模板对象 |
| `GET /api/categories` | 分类及数量 | → `[{category, templateCount}]` |
| `POST /api/uploads` | 上传素材（multipart：`templateId` + `file`）；服务端校验类型/大小/魔数 | → `{id, url, originalName, mimeType, sizeBytes}` |
| `GET /api/files/uploads/{id}` | 读素材文件（仅本人） | → 图片字节 |
| `POST /api/tasks` | **受理任务**：校验+落库后立即返回，不等待制作 | `{templateId, assetIds[]}` → `{taskId}` |
| `GET /api/tasks/{id}` | 任务详情（**只读**，含步骤边界） | → `{task{status,currentStage,failStage,failReason,resultUrl,createdAt,...}, template, steps[{stage,status,startedAt,finishedAt}], assets[]}` |
| `GET /api/tasks/mine` | 个人历史（创建时间倒序，含模板标题/封面） | → `[{id, templateTitle, templateCoverUrl, status, currentStage, failStage, failReason, resultUrl, createdAt, finishedAt}]` |
| `GET /api/files/results/{taskId}.png` | 成功结果图（仅本人） | → PNG |
| `GET/PUT /api/dev/sim-config` | 查询/修改模拟配置（开发演示用） | `{materialCheckMs, simulateGenerateMs, resultFinalizeMs, failStage}` → 同左 |
| `GET /api/health` | 健康检查 | → `{"status":"UP"}` |

错误响应统一 `{code, message}`，如 401 `NO_SESSION`、400 `ASSET_COUNT_MISMATCH`/`UNSUPPORTED_TYPE`/`FILE_TOO_LARGE`、403 `ASSET_FORBIDDEN`、404 `TASK_NOT_FOUND`。

### 5.2 任务状态与阶段设计

```
task.status:        WAITING ──► PROCESSING ──► SUCCESS
                                       └──► FAILED        （终态不因查询回退）
task.current_stage: null ─► MATERIAL_CHECK ─► SIMULATE_GENERATE ─► RESULT_FINALIZE（与 status 分别表达）
task_step:          每阶段一行 PENDING→RUNNING→PASSED/FAILED + started_at/finished_at（独立可观察的执行边界）
失败语义:           task.fail_stage + fail_reason（可理解原因）+ 失败步骤 message
```

### 5.3 后台执行方式（Java）

- 受理：校验（模板存在、素材数量恰好等于 `requiredImageCount`、素材归属本用户且属于该模板、类型合规）→ 事务内插入 task(WAITING) + task_step(PENDING×3) + task_asset → **事务提交后**把 taskId 交给工作线程 → 接口立即返回。
- 执行：单线程 `ExecutorService`（多任务先进先出排队）；每个阶段：置 RUNNING（task.current_stage 同步）→ 按 `sim_config` 休眠模拟耗时 → 执行真实业务校验/产物合成 → 置 PASSED；任一步抛出失败即记录失败阶段与原因、任务置 FAILED，**后续步骤不再执行**。
- 成功：「结果整理」用 java.awt 合成一张结果卡 PNG（渐变底 + 模板标题 + 用户上传图，文案绘制失败自动降级）写入数据目录，`task.result_url` 指向它。
- 查询接口纯只读，不驱动任务推进；已完成任务不会被任何查询改回处理中。
- 优雅停机：`@PreDestroy` 等待工作线程 10s（正在执行的任务大概率跑完落库）；强杀则任务停留在 PROCESSING（见 §8 已知限制）。

## 6. 演示指引

启动后打开 **http://localhost:3000**。

### 6.1 成功主流程
1. 首页浏览模板，点击分类 chip（如「节日祝福」）筛选，点击「旅行明信片」进入详情。
2. 两个上传位分别选择本地图片（JPG/PNG ≤5MB）——上传中有进度条，可预览/替换/移除；选错类型会立即提示。
3. 点击「开始制作」：按钮立即恢复（不再停留「提交中」），右侧出现任务卡片「处理中·素材检查」→「模拟生成」→「成功」，卡片内可直接看到结果预览图（点击看大图）。

### 6.2 指定阶段失败（可重复触发）
```bash
# 注入「模拟生成」阶段失败（也可注入 MATERIAL_CHECK / RESULT_FINALIZE），耗时调短便于演示
curl -X PUT http://localhost:3000/api/dev/sim-config -H 'Content-Type: application/json' \
  -d '{"materialCheckMs":1000,"simulateGenerateMs":1000,"resultFinalizeMs":1000,"failStage":"SIMULATE_GENERATE"}'
# 提交一个任务 → 卡片显示「失败」+「失败阶段：模拟生成」+ 原因文案
# 恢复不注入（之后的新任务正常成功；已失败任务保持失败）
curl -X PUT http://localhost:3000/api/dev/sim-config -H 'Content-Type: application/json' \
  -d '{"materialCheckMs":2000,"simulateGenerateMs":4000,"resultFinalizeMs":2500,"failStage":null}'
```
也可在环境变量 `SIM_FAIL_STAGE` 中设置启动默认注入。

### 6.3 调整处理耗时
同上接口，改三个 `*Ms` 字段（0~60000ms）。调大（如 5000）可清晰观察「等待处理」（排队）与「处理中·当前阶段」；调小可加速演示。

### 6.4 多任务
快速在多个模板页连续提交任务：各任务卡片状态/阶段/结果互不串扰；处理能力为单线程队列，可见 `WAITING（等待处理）`与 `PROCESSING` 并存。任务全部结束后前端**停止轮询**（DevTools Network 可验证不再有 `/api/tasks/mine` 请求）。

### 6.5 刷新恢复
任务处理中刷新页面：历史记录从 Java 后端恢复，进行中的任务继续被跟踪直到结束。

### 6.6 查询异常 ≠ 任务失败（推荐演示）
任务处理中 `docker compose stop backend`（或杀掉后端进程）：页面出现「状态查询暂时不可用…任务信息已保留，正在自动重试」横幅，任务卡片**保持「处理中」原样**（不会误标失败），可点击 ⟳ 手动恢复；再 `start backend` 后轮询自动恢复、横幅自动消失。

### 6.7 用户身份与个人记录
- 方案：**匿名会话 Cookie**。首次访问 `GET /api/session` 时后端创建用户（昵称「访客XXXX」）并下发 `MS_SESSION`（HttpOnly，180 天）；素材、任务、历史全部按该用户隔离，无需注册登录。
- 演示：顶栏「账号设置」→ 修改昵称（同步顶栏）→「切换身份」→ 历史清空（全新用户）；用另一浏览器/无痕窗口可同时演示两身份互相隔离。
- 说明：这是有意选择的轻量方案（题目不要求注册登录/第三方账号）；不涉及真实密码与凭据。

## 7. 部署与回滚

- **部署**：任意装 Docker 的主机 `docker compose up -d --build`；变更后 `docker compose up -d --build` 重建 backend/frontend 镜像（MySQL 数据在卷中保留）。
- **回滚**：按 tag/commit 重新构建即可（`git checkout <上一验收tag> && docker compose up -d --build`）；数据库结构变更时，由于 `01-schema.sql` 只增不改（CREATE IF NOT EXISTS），破坏性变更需手动迁移或 `docker compose down -v` 重置数据卷后重启。
- 不要求生产级发布平台；未做镜像仓库推送、域名/HTTPS 等（题目范围外）。

## 8. 已完成 / 未完成项、已知限制与实际投入

### 已完成（均可在 `docs/process/03-verification.md` 找到命令与结果）
- R-01~R-14 全部要求的主体功能与技术栈约束；后端 17 例测试（真实 MySQL 8.4）与前端 14 例测试全绿；前端 lint/tsc/build 全过；`docker compose up --build` 配置齐备。
- 关键行为实机验证：受理即返回（93ms）、三步顺序边界落库、指定阶段失败/恢复、耗时调整、多任务排队互不串扰、轮询生命周期（结束即停/切换用户清理）、查询异常与任务失败严格区分、刷新恢复、身份切换隔离、上传校验（类型/大小/魔数）、小屏可用。

### 未完成 / 已知限制（如实说明）
- **`docker compose up` 本机未实跑**：开发环境无 Docker（过程文档 04-I-1、V-008）。compose 采用标准模式，Dockerfile 内每个构建步骤（maven package、npm ci/build）与 db/init SQL 均已在本地用等效命令在真实 MySQL 8.4 上验证；若验收环境按 §3.1 启动遇到问题，可参考 §3.2 分组件运行。
- **重启自动恢复未实现**（题目明确范围外）：强杀进程后 PROCESSING/WAITING 任务保持原状，不自动续跑，也不会被误标为失败（已实测其表现）。扩展思路：启动时把 PROCESSING 扫回 WAITING 重入队，但需引入步骤级断点与产物幂等（结果文件可能已写入），故未做。
- 单线程执行器（题目允许排队式）：吞吐不高，但状态语义清晰、便于观察 WAITING。
- 任务受理幂等/断点续跑/自动重试未实现（题目允许在 README 讨论取舍，非扣分项）：幂等可基于客户端生成 `Idempotency-Key` 落库去重，本案未引入。
- AWT 生成结果图在缺 CJK 字体的容器里中文文案可能显示为方框（图片仍正常产出，代码已做降级不失败）。
- 上传文件的类型校验基于 Content-Type + 魔数头，未做逐字节深度校验（演示场景足够）。
- 会话 Cookie（`MS_SESSION`）为未签名的用户 UUID：知道他人 UUID 可伪造其身份。这是匿名轻量身份方案的有意取舍（题目允许自选身份策略，不含真实凭据）；若需收紧可改为服务端签名 Token（HMAC）或随机不透明 Token + 服务端映射表。

### 实际投入
- 2026-10-28 23:20 开始读题，2026-10-29 01:30 完成开发与全部验证，**约 2.2 小时**纯专注投入（含读题、前后端开发、环境受限下的本地 MySQL 攻坚、测试、浏览器实测与全部过程文档）；README 与最终整理另计约 0.5 小时。

## 9. AI 工作流说明（如实）

本项目**大量使用了 AI 编程工具**（ZCode CLI，Claude/GLM 系模型驱动），全部代码、文档由 AI 在本人目标与约束下生成并经我确认。工作流如下：

1. **拆解任务**：把题目 R-01~R-14 映射为里程碑 M0~M6（见 `docs/process/00`），每个里程碑有完成标准；先写六份过程文档再动代码。
2. **提供上下文**：把题目全文、参考截图、环境探测结果（无 Docker/sudo/MySQL、有网）作为约束输入，并要求 AI 先记录关键决策（`docs/process/01`）再实施。
3. **生成/修改代码**：按里程碑逐块生成（后端引擎→接口→前端页面→测试→CI/compose）；AI 对每次选型先写「选了什么/为什么/放弃了什么」。
4. **验证与修正**：AI 每完成一块立即跑编译/测试/构建并留痕（`docs/process/03` 有完整的失败→修复→复验链，例如 V-005 的行锁问题、V-007 的横幅未清除 bug，均为实跑发现、实跑修复）；UI 用浏览器自动化实测截图。
5. **具体例子**：集成测试首跑 6 例失败（Lock wait timeout）。AI 读取完整堆栈，定位为「sync 执行模式下引擎在 `create()` 未提交事务内运行，REQUIRES_NEW 的进度更新被外层事务行锁阻塞」，将 `worker.submit` 改为注册到 `TransactionSynchronization.afterCommit()`，复验 17/17 全绿，并把根因/修复记入 `04-I-7`。环境无 Docker/MySQL 时，AI 自行改用「Ubuntu mysql-server-core deb 解压 + 无 root 运行 MySQL 8.4.11」完成真实验证，全程记录于 02/03 文档。

## 10. 脚手架与开源素材复用来源

- 前端脚手架：`create-next-app@15`（TypeScript + ESLint + App Router 模板），其后所有页面/样式/逻辑为项目内实现。
- 后端依赖均为公开开源组件（Spring Boot / MyBatis / MySQL Connector/J），未复制任何业务代码模板。
- 素材：模板封面 SVG 由仓库内脚本 `frontend/scripts/gen-covers.mjs` 生成（算法绘制，无第三方版权素材）；测试用 1×1 PNG 为公共领域样式的最小 PNG（base64 内置于测试）；结果图由 java.awt 程序化合成。
- UI 参考：按题目截图（UI-01/02/03、R10）自行实现的暗色主题，未复用截图中的品牌素材。
- 本地开发验证所用 MySQL 8.4.11 来自 Ubuntu 官方仓库 deb（仅开发环境运行方式，不属于交付物）。
