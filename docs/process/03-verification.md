# 03 验证记录

> 约定：每条验证写【命令 / 预期 / 实际 / 结论】。失败→修复→复验留痕。跑不了的写「未验证 + 原因」，禁止编造。

## V-001 环境基线探测（2026-10-28 23:30）
- 命令：`java -version && mvn -version && node -v && npm -v && docker --version && git --version`、`sudo -n true`、`wget --spider` 探测 npm registry / Maven central / MySQL CDN / MariaDB 归档
- 预期：确认可用工具链与网络
- 实际：Java 17.0.18、Maven 3.9.16、Node 22.23.2、npm 10.9.8、git 2.53.0 可用；**docker 命令不存在**；`sudo -n` 需要交互密码（不可用）；npm registry 与 Maven Central（阿里云镜像）HTTP 可达；cdn.mysql.com 与阿里云/清华 MySQL 镜像对 minimal tarball 均 404；archive.mariadb.org 的 mariadb-11.4.5 bintar HTTP 200。
- 结论：本机无 Docker/MySQL 是硬限制；采用 D-8 的验证策略（本地 MariaDB 真实验证 + CI service 容器 + compose 交付但本地无法实跑，记入 04）。

## V-002 后端编译与单元测试（2026-10-29 00:05）
- 命令：`cd backend && mvn -q compile && mvn -q test`
- 预期：编译通过；TaskEngineTest 3 例全过；集成测试在无 DB 时跳过不失败
- 实际：编译通过（修复缺 import：SimConfigService/Stage、MaterialCheckStep/Template、FileController/CurrentUser）；TaskEngineTest 3/3 通过；集成测试 4 个类因无 DB 被跳过（BaseIntegrationTest 静态 @BeforeAll 探测 DB，不可用即 Assumptions.abort，Spring 上下文不启动）
- 结论：✅ 后端可构建、引擎单测全绿。中途修复：JsonNode.asString→asText、缺 status() static import、误删括号（均有编译错误→修复→复验留痕）

## V-003 前端四连验证（2026-10-29 00:13~00:15）
- 命令：`cd frontend && npm run lint && npm run typecheck && npm test -- --run && npm run build`
- 预期：全部通过
- 实际：lint ✅；tsc ✅；vitest 首跑 10/14（@ 别名未配、RTL 无自动 cleanup、tracker 测试脚本与常驻 interval 语义不符）→ 修复 vitest.config alias、setup.ts 手动 cleanup、重写 harness 跟踪 active 调度 → 复验 14/14 ✅；build ✅（standalone 输出，3 路由）
- 结论：✅ 前端四项全绿

## V-004 模板封面素材生成（2026-10-29 00:18）
- 命令：`node frontend/scripts/gen-covers.mjs && ls frontend/public/demo/covers/`
- 预期：生成 6 个与种子 cover_url 一致的本地 SVG
- 实际：travel-postcard / art-sketch / festival-greeting / love-mosaic / pet-avatar / trip-collage 6 张 SVG 生成成功
- 结论：✅ 素材零外部依赖（来源与复现脚本在仓库内）

## V-005 集成测试（真实 MySQL 8.4.11，2026-10-29 00:49~01:00）
- 前置：本地 MySQL 8.4.11（Ubuntu mysql-server-core deb 解压运行，端口 33061，库 material_studio_test），启动脚本 `~/local-mysql/start-mysqld.sh`（非仓库文件，仅开发环境）
- 命令：`cd backend && TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:33061/material_studio_test?...' mvn test`
- 预期：17 例全部通过（3 单测 + 14 集成）
- 实际：**首跑 6 例失败** —— 根因：sync 执行模式下 `TaskService.create()` 的 `@Transactional` 事务尚未提交就执行了引擎，`TaskProgressService` 的 REQUIRES_NEW 更新 `task` 行被外层事务行锁阻塞（Lock wait timeout 50s）。**修复**：把 `worker.submit` 注册到 `TransactionSynchronization.afterCommit()`（提交后才触发执行，async/sync 语义都正确）。复验中又暴露 2 个**测试自身错误**（PNG 字节标 jpeg 魔数不符；session 测试未带第二次 Cookie 且比较了会变化的 created 标志）→ 修正后：**Tests run: 17, Failures: 0, Errors: 0，BUILD SUCCESS**
- 结论：✅ 步骤顺序/失败中断/查询只读/完成不回退/身份隔离/上传与提交校验 在真实 MySQL 上全部验证通过；schema+seed 建表脚本同源执行成功

## V-006 后端运行冒烟（2026-10-29 01:02）
- 前置：本地 MySQL 8.4.11（33061/material_studio）+ `mvn spring-boot:run`（8080，async 模式，DATA_DIR=/tmp/ms-data）+ `npm run dev`（3000，rewrites 代理 /api → 8080）
- 命令：Node 脚本走代理全链路（session → 上传×2 → 提交 → 轮询 → 结果 → 不回退 → 历史）
- 预期：受理立即返回；WAITING/PROCESSING 可观察；三步 PASSED；结果 PNG 可访问；完成不回退
- 实际：
  - `createTask 耗时 93ms`（远小于 8.5s 模拟总耗时）→ **受理不等待** ✅
  - 提交后首次轮询即读到 `PROCESSING / MATERIAL_CHECK` ✅
  - 终态 `SUCCESS`，steps `MATERIAL_CHECK:PASSED SIMULATE_GENERATE:PASSED RESULT_FINALIZE:PASSED` ✅
  - 结果 `/api/files/results/{taskId}.png` 200，38931 字节 PNG ✅
  - 连续 3 次查询状态无回退 ✅
- 失败注入 + 多任务（第二段脚本）：
  - `PUT /api/dev/sim-config {failStage:"SIMULATE_GENERATE"}` → 提交 → `FAILED`，`fail_stage=SIMULATE_GENERATE`，原因「模拟故障注入：本次任务被配置为在「模拟生成」阶段失败」，steps 前一阶段 PASSED、当前 FAILED、后续 PENDING ✅
  - 重置 `failStage:null` → 新任务成功（可重复触发/可恢复）✅
  - 同时提交 2 个任务：快照显示 `PROCESSING(MATERIAL_CHECK) / WAITING(-)`（单线程排队），最终双双 SUCCESS ✅
- 结论：✅ R-04/05/06 服务端行为全部实机验证通过

## V-007 前端浏览器走查（Chromium 1280×800 + 390×760，2026-10-29 01:05~01:20）
- 方式：ZCode 内置浏览器实际操作 + 截图（截图存于会话 artifacts，对应描述如下）
- 列表页（UI-01 布局）：分类 chips + 6 卡片网格 + 暗色主题 ✅；点击「节日祝福」chips 后只剩 2 卡片 ✅
- 详情页（UI-02 布局）：左制作/右历史；页内注入 2 个真实 PNG 文件 → 上传中→预览（替换/移除可用）→ 「开始制作」激活 ✅
- 提交后：任务卡片即时出现「处理中·素材检查」→「模拟生成」→「成功 + 结果预览图」✅（R-07 状态区分）
- 指定阶段失败演示：注入 RESULT_FINALIZE 失败 → 卡片红框显示「失败阶段：结果整理」+ 可理解原因，与旁边成功卡片并存 ✅
- **查询异常 ≠ 任务失败（R-07 核心）**：任务处理中强杀后端进程 → 顶栏横幅「状态查询暂时不可用…任务信息已保留，正在自动重试」+ 面板提示 + 任务卡片**保持「处理中·模拟生成」未变失败**；历史三条原样 ✅
- 恢复：重启后端 → 轮询自动恢复、横幅自动消失（**此处发现并修复 bug**：恢复成功后 connectionError 未清除，见 04-I-6）✅
- 刷新恢复（R-09）：页面 reload 后历史由后端恢复、进行中任务继续跟踪 ✅
- 身份（R-10）：账号设置对话框（昵称保存同步顶栏）；「切换身份」后历史清空、新身份为全新访客 ✅
- 上传校验（R-03）：注入 text/plain 文件 → 「不支持的图片类型：text/plain」+「重新选择」✅
- 小屏（390×760）：详情页单列布局、按钮/上传位可操作 ✅
- 结论：✅ 前端核心行为全部实机验证；发现 1 个 bug 已修复并复验

## V-008 compose 验证：未验证
- 原因：本机无 Docker（04-I-1）。compose 配置采用的均为标准模式（mysql:8.4 官方镜像 + docker-entrypoint-initdb.d 挂载 + healthcheck + depends_on condition；backend/frontend 多阶段构建 Dockerfile，其中 maven package / npm ci / npm run build 等步骤均已在本地等效验证）。
- 缓解：CI（GitHub Actions）含 mysql:8.4 service 的后端测试与前端四连检查，作为等效云端验证；验收方按 README「一键启动」执行 `docker compose up --build` 即可。

## V-009 R2 验收修复轮验证（2026-10-29 01:40~01:45）

> 背景：用户完成首轮验收（独立复跑确认 R-01~R-14 全部满足、无禁做项），提出 1 个 P1 + 3 个 P2 + 1 个 P3 建议，见 04-I-9。本轮为定点修复。

### 修复内容
| 项 | 修复 | 验证 |
|---|---|---|
| P1-1 受理成功但列表刷新失败误报「提交失败」、新任务失联 | 抽出 `lib/taskSync.ts#createAndTrackTask`：`createTask` 成功即受理成功（启动 tracker、清上传位）；随后的 myTasks 刷新失败只走 `setConnectionError` 通道；仅受理本身失败才显示提交错误且保留上传位 | 新增组件测试 `detailPageSubmit.test.tsx` 2 例（受理成功+刷新失败 / 受理失败），断言：无提交错误提示、tracker.start 已调用、connectionError 已设置、上传位已清空（0/1）/ 受理失败时上传位保留（1/1）且不误设 connectionError |
| P2-1a 请求无超时 | `api.request` 加 `AbortSignal.timeout(8000)`（上传 XHR 不受影响） | tsc + 既有测试回归 |
| P2-1b 轮询无 in-flight 互斥 | `taskTracker.pollOnce` 加 in-flight 互斥（上一轮未返回跳过本次 tick） | taskTracker 新增用例「上一轮未返回时跳过新 tick」：fetchMock 调用 1 次不堆积，响应后正常落地并停止 |
| P2-2 bootError 无展示 | TopBar 渲染「连接服务失败：{原因} + 重试」横幅，重试=重新执行 `bootstrapSession`（可重入） | 新增 `TopBar.test.tsx` 2 例：bootError 显示横幅→点击重试→会话建立、横幅消失；会话正常时不显示 |
| P2-3 悬挂上传 setState | 详情页为每个上传位持有 AbortController，卸载时全部 abort；ABORTED 分支复位上传位 | 代码路径 review + 回归测试（卸载中止为浏览器行为，jsdom 下以 ABORTED 复位逻辑覆盖） |
| P3 markProcessing 防御 | SQL 加 `AND status='WAITING'` 并检查受影响行数（0 则中止）；01-D6 补记；README §8 补 Cookie 未签名说明 | 后端 17/17 全绿（含 TaskFlowTest 等真实 DB 路径） |

### 命令与结果
- `cd frontend && npm run lint` → ✅ 0 错误
- `npm run typecheck` → ✅ 0 错误（首跑 1 个测试类型错误 MockInstance/Mock，已修）
- `npm test -- --run` → ✅ **Test Files 5 passed, Tests 19 passed**（14 + 新增 5：tracker 互斥 1、详情页受理路径 2、TopBar 2）
- `npm run build` → ✅ Compiled successfully
- `cd backend && TEST_MYSQL_URL=...33061/material_studio_test... mvn test` → ✅ **Tests run: 17, Failures: 0, Errors: 0, BUILD SUCCESS**（含 markProcessing WHERE 条件变更后的真实 DB 路径）
- 结论：✅ R2 全部修复项完成，复验门槛（前端四连全绿、后端 17/17）达成；tag `acceptance-r2`。
