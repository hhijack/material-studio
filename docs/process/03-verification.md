# 03 验证记录

> 约定：每条验证写【命令 / 预期 / 实际 / 结论】。失败→修复→复验留痕。跑不了的写「未验证 + 原因」，禁止编造。

## V-001 环境基线探测（2026-10-28 23:30）
- 命令：`java -version && mvn -version && node -v && npm -v && docker --version && git --version`、`sudo -n true`、`wget --spider` 探测 npm registry / Maven central / MySQL CDN / MariaDB 归档
- 预期：确认可用工具链与网络
- 实际：Java 17.0.18、Maven 3.9.16、Node 22.23.2、npm 10.9.8、git 2.53.0 可用；**docker 命令不存在**；`sudo -n` 需要交互密码（不可用）；npm registry 与 Maven Central（阿里云镜像）HTTP 可达；cdn.mysql.com 与阿里云/清华 MySQL 镜像对 minimal tarball 均 404；archive.mariadb.org 的 mariadb-11.4.5 bintar HTTP 200。
- 结论：本机无 Docker/MySQL 是硬限制；采用 D-8 的验证策略（本地 MariaDB 真实验证 + CI service 容器 + compose 交付但本地无法实跑，记入 04）。

## V-002 （待 M0 脚手架后追加）
