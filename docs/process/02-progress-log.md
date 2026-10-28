# 02 进度日志（追加式）

> 格式：`## [时间] 摘要`，记做了什么、产出在哪、下一步。只追加不改写。

## 2026-10-28 23:20~23:35 读题与环境探测
- 通读 `quvideo_exam_2027_fullstack/题目.md`（R-01~R-14）与 4 张参考截图（UI-01/02/03、R10 账号设置）。
- 环境探测：Java 17 / Maven 3.9 / Node 22 / git 可用；Docker、sudo、MySQL 不可用；网络可达 npm 与 Maven。
- 建立 git 仓库 `material-studio/`（分支 main，本地身份 黄俊恺）。
- 初始化六份过程文档（本次提交）。
- **下一步**：M0 脚手架（后端 Spring Boot 骨架、前端 Next.js 骨架、compose/CI 初版）；同时等待 MariaDB tarball 下载完成后搭本地 DB。

## 2026-10-28 23:35 ~ 2026-10-29 00:05 后端全量开发（M0/M1/M3/M5 服务端）
- Spring Boot 3.5.5 + MyBatis 3.0.5 + mysql-connector-j；分包 config/controller/service/mapper/model/task/web。
- 任务引擎 TaskEngine（纯 Java 可单测）：按 步骤顺序执行、每阶段开始回调 + 按模拟配置休眠、失败即中断；三个步骤实现 MaterialCheckStep / SimulateGenerateStep / ResultFinalizeStep；ResultComposer 用 AWT 合成结果 PNG。
- 数据层：7 张表 SQL（db/init/01-schema.sql）+ 6 模板种子（02-seed.sql，幂等 upsert）；构建期把 db/init 拷入 classpath 由 spring.sql.init 执行（单一来源）。
- 接口：/api/session（匿名会话 Cookie）、/api/templates*、/api/categories、/api/uploads、/api/files/uploads/{id}、/api/files/results/{taskId}.png、/api/tasks（受理即返回）、/api/tasks/mine、/api/tasks/{id}（只读）、/api/dev/sim-config（耗时+注入失败）。
- 编译通过（mvn compile），修复 3 处缺 import。

## 2026-10-29 00:06 ~ 00:15 前端全量开发 + 测试（M0/M2/M4）
- compose/Dockerfile×2/CI workflow 初版。
- Next.js 15 页面：列表（分类筛选+卡片）、详情（上传位×N + 制作按钮 + 本模板历史）、history 全量历史、账号设置对话框；暗色主题 CSS Modules。
- 状态层：zustand taskStore（增量合并、connectionError 与任务状态解耦）+ TaskTracker 轮询生命周期（结束即停、切换用户防串数据）。
- vitest 3.2.4 + RTL：14 例全过（含 RTL 手动 cleanup 修复、@ 别名修复）；lint/tsc/build 全过。

## 2026-10-29 00:18 ~ 00:30 素材与本地 DB 攻坚
- frontend/scripts/gen-covers.mjs 生成 6 张模板封面 SVG（本地素材，无外链）。
- MariaDB tarball 下载曲折：cdn.mysql.com 及国内 MySQL 镜像 404 → archive.mariadb.org 单流慢且两次截断 → tuna 镜像 30KB/s 且中途回 404 → 自写 Node 分片并行下载器（6 连接 Range），349MB 快速完成（sha256 待验证，官方校验值已取得）。
- libaio 缺失：无 sudo，下载 Ubuntu libaio1 deb 解压到 ~/.local/opt/lib（运行 mariadbd 时以 LD_LIBRARY_PATH 引入）。
- git 提交 2 次（79a3b70 过程文档初始化；1be1fb6 后端+前端+测试+CI 初版）。
- **下一步**：MariaDB sha256 校验 → 初始化实例（material_studio / material_studio_test）→ 后端集成测试（V-005）→ 前后端联调演示（V-006/007）→ README。

## 2026-10-29 00:30 ~ 01:00 本地真实 MySQL 攻坚 + 集成测试全绿
- MariaDB bintar 反复被网络截断/限速，改用 **Ubuntu 26.04 的 mysql-server-core 8.4.11 deb**（与 compose 的 mysql:8.4 同版本）本地解压运行：补齐 libaio1t64/libprotobuf-lite32t64/libtcmalloc（Debian google-perftools 包 + mininal→libtcmalloc.so.4 符号链接），`~/local-mysql/start-mysqld.sh` 无 root 启动于 127.0.0.1:33061。
- V-005 集成测试：首跑 6 失败 → 定位为 **sync 模式下引擎在外层事务内执行的行锁问题** → 修复（afterCommit 提交后执行，04-I-7）→ 修正 2 个测试自身错误 → **17/17 全绿（真实 MySQL 8.4）**。
- **下一步**：前后端联调 + 浏览器走查。

## 2026-10-29 01:00 ~ 01:25 实机联调与浏览器走查（V-006/V-007）
- 后端 8080（async 模式连真实库）+ 前端 3000（dev + rewrites 代理）。
- V-006 接口级冒烟：受理 93ms 即返、三步 PASSED、结果 PNG 38KB、不回退、失败注入/恢复、双任务排队 —— 全部通过。
- V-007 浏览器走查（截图留痕）：列表/筛选/详情/上传/提交/状态流转/失败卡片/断连横幅/恢复/刷新恢复/账号对话框/切换身份/校验提示/小屏 —— 全部通过。
- 过程中发现并修复 **I-6**（查询恢复后 connectionError 未清除，热修 + 复验）。
- 演示收尾：悬挂任务标记为演示清理失败、sim-config 恢复默认。
- **下一步**：README 全量撰写 → 追溯矩阵定稿 → tag acceptance。

## 2026-10-29 01:30 ~ 01:45 收尾交付（M6 完成）
- 最终复验：前端 lint/tsc/14 测试/build 全绿；后端 mvn test 17/17（真实 MySQL 8.4）+ 可执行 jar（28MB）构建成功；docker-compose.yml 与 ci.yml YAML 语法校验通过。
- README 撰写完成（R-13 全要点：技术版本/结构/命令/配置/接口与任务模型/六类演示/身份方案/已完成未完成+投入/部署回滚/AI 工作流/素材来源）。
- 追溯矩阵定稿（05 文档），全部里程碑 M0~M6 完成；tag `acceptance` 指向最终 commit。
- 项目交付状态：主链路 + 全部演示路径可运行可验证；compose 本机未实跑（无 Docker，已如实记录于 V-008/04-I-1）。
