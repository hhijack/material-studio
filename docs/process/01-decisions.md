# 01 技术决策记录

> 约定：每个决策先写「选了什么 / 为什么 / 放弃了什么」再实施。编号 D-xx，随进度追加。

## 环境基线（2026-10-28 23:30 探测）

- 可用：Java 17.0.18、Maven 3.9.16（阿里云镜像）、Node 22.23.2 + npm 10.9.8、git 2.53；网络可达 npm registry 与 Maven 仓库。
- 不可用：**Docker**（无命令、无 podman/containerd）、**sudo**（无法 apt 安装）、**本机无 MySQL/MariaDB**、无 curl。
- 结论：交付物仍按题目要求提供 docker-compose（R-14），但本机无法 `compose up` 验证 → 04 文档记录，README 给出验收环境运行说明。本地真实验证方案见 D-8。
- MySQL 本地方案：官方 cdn.mysql.com 与阿里云/清华镜像均 404（minimal tarball 路径不对），改用 MariaDB 官方归档 `mariadb-11.4.5-linux-systemd-x86_64.tar.gz`（HTTP 200 已确认，后台下载中）。MariaDB 与 MySQL 协议/SQL 高度兼容，用于跑真实 SQL 与 MyBatis 集成验证；compose/CI 中仍用 `mysql:8.4` 官方镜像。风险与局限记录于 04。

## 决策列表

### D-1 后端技术栈：Spring Boot 3.5.x + Java 17 + Maven + MyBatis（2026-10-28 23:35）
- **选了什么**：Spring Boot 3.5.x（Java 17 基线）、mybatis-spring-boot-starter 3.0.x、`com.mysql:mysql-connector-j`、Maven 构建。
- **为什么**：本机 JDK 17，Spring Boot 3.x 要求 ≥17，兼容；MyBatis 是题目指定；Maven 配合阿里云镜像下载快、CI 配置简单。Spring Boot 3.5 为当前维护中的稳定小版本系列。
- **放弃了什么**：Spring Boot 2.7（Java 8 时代，过旧）；Gradle（本机无 gradle 命令，且团队阅读成本高）。

### D-2 前端技术栈：Next.js 15 App Router + React 19 + TypeScript + CSS Modules + Zustand（2026-10-28 23:35）
- **选了什么**：Next.js 15（App Router）、React 19、TS strict；样式用**全局 CSS 变量 + CSS Modules**（零额外依赖）；客户端任务状态用 **zustand**。
- **为什么**：题目只限定框架，样式/状态管理自选。CSS Modules 无工具链风险，暗色主题用 CSS 变量好维护；zustand 很小、适合"任务跟踪 store + 轮询管理器"这种非 React 绑定的逻辑，比纯 useReducer 更容易做单测（store 纯逻辑可脱离组件测试）。
- **放弃了什么**：Tailwind（构建链多一层 PostCSS 依赖，收益主要是书写速度，本题页面数量少）；Redux Toolkit（太重）；纯 useReducer/useSyncExternalStore 手写（可行，但轮询订阅/去重逻辑自己造轮子容易错）。

### D-3 前后端连接：Next.js rewrites 把 `/api/*` 反代到 Java，同源 Cookie 身份（2026-10-28 23:35）
- **选了什么**：前端一律请求同源 `/api/...`，`next.config.ts` 用 rewrites 按 `BACKEND_ORIGIN` 环境变量转发到 Java（compose 内为 `http://backend:8080`）。业务逻辑全部在 Java；Next 只做静态资源与反向代理（题目明确允许「接口中间层」）。
- **为什么**：身份用 Cookie（见 D-5），同源反代免去跨域凭据（CORS + SameSite）配置和浏览器兼容坑；compose 里前端容器直连后端服务名，无需对外暴露后端端口也可工作。
- **放弃了什么**：前端直连 `http://localhost:8080`（要配 CORS 和 SameSite=None，容器化后还涉及地址暴露，复杂度高）。

### D-4 目录结构（2026-10-28 23:35）
- **选了什么**：
  - `backend/src/main/java/com/example/materialstudio/`：按 `controller / service / task(引擎) / mapper / model / config / support` 分包，单 Maven 模块。
  - `frontend/src/app/`：`page.tsx`(列表)、`templates/[id]/page.tsx`(详情)、`history/page.tsx`(历史)；`src/lib/`（api client、task store、poller）、`src/components/`。
  - `db/init/`：`01-schema.sql`、`02-seed.sql`（compose 挂到 `/docker-entrypoint-initdb.d`）。
- **为什么**：单模块对笔试规模足够；分包让「任务引擎」独立成包便于单测（不依赖 Spring 上下文）。
- **放弃了什么**：多模块 Maven（过度设计）；前后端 monorepo 工具（pnpm workspace 等，没必要）。

### D-5 身份方案：匿名会话 Cookie + users 表（2026-10-28 23:35）
- **选了什么**：首次访问 `GET /api/session` 时后端创建用户（默认昵称"访客xxxx"）并下发 `HttpOnly` Cookie `MS_SESSION`（值为用户 UUID），有效期 180 天；素材、任务、历史全部按该用户隔离。提供 `PATCH /api/session`（改昵称）与 `DELETE /api/session`（切换身份=重置 Cookie）。
- **为什么**：题目要求「自行设计身份与个人记录方案」，不要求注册登录体系；匿名 Cookie 零摩擦、可演示隔离（切换身份后历史清空）、实现诚实简单。R10 截图的账号设置用「昵称编辑 + 切换身份」对话框呼应。
- **放弃了什么**：用户名/密码注册登录（超出题目必要范围，且要处理加密、表单等无关链路）；JWT（对匿名演示场景收益为零）。

### D-6 任务执行模型：受理即返回 + 单线程排队执行器（2026-10-28 23:35；R2 补充防御条件）
- **选了什么**：`POST /api/tasks` 校验+落库（status=WAITING）后把 taskId 提交到**单线程 ExecutorService**，立即返回。工作线程按 素材检查→模拟生成→结果整理 顺序执行；每步开始前把 task.current_stage 置为该阶段、task.status=PROCESSING，并插入 `task_step` 记录（RUNNING），结束后更新为 PASSED/FAILED。失败即停，后续步骤不再执行。
- **R2 补充（2026-10-29，验收建议）**：`markProcessing` SQL 增加 `AND status = 'WAITING'` 并检查受影响行数（0 则中止执行）——防御异常重复投递导致重复执行；当前单线程引擎下本无实际竞态，属纵深防御。
- **为什么**：单线程队列天然满足「多任务可排队」，且容易观察 WAITING→PROCESSING 的状态迁移；题目不要求真并行。每步独立落库给出「可观察的执行边界」，前端/接口读到的永远是 DB 状态而非内存猜测。
- **放弃了什么**：`@Async` 注解（隐藏了线程池语义，不好解释边界）；MQ/Redis（题目不强制且环境无 Docker 起不了额外中间件）； Scheduled 轮询扫表驱动（"查询驱动推进"的反模式，还引入延迟）。

### D-7 模拟控制：sim_config 表 + 开发接口 + properties 默认值（2026-10-28 23:35）
- **选了什么**：单行配置表 `sim_config`（三个阶段各自耗时 ms + fail_stage 枚举），启动时用 properties `sim.*` 初始化；运行期可用 `GET/PUT /api/dev/sim-config` 修改，**对之后创建的任务生效**（正在跑的任务不受影响），实现「可重复触发指定阶段失败」和「可调耗时」。
- **为什么**：放 DB 里可跨请求、跨实例重启保留，比纯 properties 灵活（README 里两条路径都写清楚）；开发接口加在 `/api/dev/**` 并在 README 说明这是演示用配置面。
- **放弃了什么**：随机失败（题目明确不要求）；每任务失败参数（用全局配置更接近"故障开关"的心智模型，够用）。

### D-8 测试与本地 DB 验证策略（2026-10-28 23:36）
- **选了什么**：
  - 后端：JUnit5。任务引擎（步骤顺序、失败中断、失败原因）写成**纯单测**（内存步骤记录器，不依赖 DB）；另加 Spring Boot 集成测试（MockMvc）覆盖接口行为——集成测试需要 DB，用 `Assumptions` 探测：`MYSQL_TEST_URL` 环境变量存在或默认 localhost 可连才跑，否则**跳过并记日志**（避免无 DB 环境假失败；CI 里配置 mysql:8 service 必跑）。
  - 前端：vitest + @testing-library/react。重点测 task store：多任务互不串扰、轮询异常不清状态、结束停止轮询。
  - CI：GitHub Actions，前端 job（lint/tsc/build/test）+ 后端 job（带 mysql:8 service 容器跑 test + package）。
- **为什么**：核心行为（顺序、中断）不依赖 DB 可以秒级跑；DB 行为（隔离、持久化、只读查询）用真 MySQL 才有意义。GitHub 托管 runner 自带 Docker，service 容器是标准做法。
- **放弃了什么**：Testcontainers（本机无 Docker 无法先验证，选 service 容器更稳）；H2 MySQL 模式（方言差异会让测试结果失真，宁可跳过也不造假）。

### D-9 结果产物：AWT 合成预览图，失败则复制兜底（2026-10-28 23:35）
- **选了什么**：「结果整理」阶段用 java.awt（headless）把用户上传图缩放拼到一张带模板标题的结果卡 PNG 上，写入数据卷 `results/{taskId}.png`，`task.result_url` 指向 `/api/files/results/{taskId}.png`；AWT 异常时降级为复制一张预置结果图。
- **为什么**：让"成功结果可预览"有真实可见的产物，又完全不依赖外部服务；降级路径保证演示不脆。
- **放弃了什么**：真实视频/图片处理（题目明确不做）；只回一个字符串 URL（预览体验差）。

### D-10 文件存储：本地数据卷目录 + Spring 静态资源映射（2026-10-28 23:35）
- **选了什么**：上传文件存 `{workdir}/data/uploads/{assetId}.{ext}`，结果存 `data/results/`；通过 Spring `ResourceHandlerRegistry` 以 `/api/files/**` 暴露（结果文件校验归属用户，上传文件按 UUID 文件名防猜测）。compose 中挂为 volume。
- **为什么**：题目允许「后端本地目录或容器数据卷」；UUID 文件名 + 归属校验满足基本安全。
- **放弃了什么**：对象存储（无外部依赖要求）；base64 入库（大文件不合适）。

## 待定决策
- （M2 时定）上传进度条实现：XMLHttpRequest `upload.onprogress`（fetch 不支持上传进度）——暂定 XHR，记入 02。
