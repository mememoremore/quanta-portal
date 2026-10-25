# Quanta Pages 真实联调待办清单（上线前逐项核对）

> 2026-10-25 已完成首次真实服务器部署与验收（详见文末「验收结果」）。
> 剩余未完成项以 ⬜ 标注；已核验项以 ✅ 标注。

## A. 环境事实核实

- ✅ 容器由 docker compose 管理（`/opt/ruoyi/deploy/docker-compose.yml`），app 服务 host 网络 + 非 root（108:112）
- ✅ 磁盘容量充足（40G 盘余 23G）
- ✅ GitHub 出网：宿主机与容器内 api.github.com 均 200
- ✅ GitHub Token：`QT_PAGES_GITHUB_TOKEN` 已写入 `/opt/ruoyi/.env`，线上验证 limit=5000（2026-10-25）
- ✅ DNS 通配符：`*.page A → 8.133.248.109` 已配置并验证生效（2026-10-25）
- ✅ 通配符证书：`*.page.quantacenter.com` Let's Encrypt ECC（至 2027-01-23），acme.sh（/root/.acme.sh，DNS-01 + RAM 子账号 pages-cert-bot 仅 DNS 权限）自动续期并自动 reload nginx
- ✅ 443/80 防火墙正常（门户 https 可达）

## B. 目录与权限

- ✅ `/srv/quanta-pages/sites` 已创建，属主 ruoyi:ruoyi(108:112)，应用可写、nginx 可读

## C. 数据库

- ✅ 部署前全量备份：`/opt/ruoyi/backup/ry-vue-20261025114903.sql.gz`（58 表，BACKUP_OK 三重校验）
- ✅ `sql/2026-10-25-qt-pages.sql` 执行两遍（幂等），现有 58 表零改动，仅新增 qt_page_site/qt_page_deployment

## D. 应用配置

- ✅ `CORS_ALLOWED_ORIGINS=https://www.quantacenter.com`（原有配置已是精确值）
- ✅ `TOKEN_REFRESH_SECURE=true`（原有）；Refresh Cookie host-only
- ✅ `QT_PAGES_ENABLED=true / QT_PAGES_ROOT_DIR=/srv/quanta-pages / QT_PAGES_BASE_DOMAIN=page.quantacenter.com` 写入 `/opt/ruoyi/.env`
- ✅ app 容器已追加挂载 `/srv/quanta-pages:/srv/quanta-pages`（compose 备份 `.bak-pages-*`）
- ✅ 启动日志：运行时初始化 → 启动恢复完成，已就绪

## E. Nginx

- ✅ `/etc/nginx/sites-available/quanta-pages.conf`：80 → 301 HTTPS；443 独立 server 块（通配符证书），无任何代理/执行配置
- ✅ 安全头四件套（首页 location 已补齐继承声明）、ETag 协商缓存、index.html no-cache
- ✅ 未知主机/缺站点 404、隐藏路径 404、POST 405、SPA 深链回退（HTTP 与 HTTPS 均已验证）

## F. 验收结果（2026-10-25，测试账号 qt_pages_e2e / user_id=272，验收后已停用）

| # | 验收项 | 结果 |
| --- | --- | --- |
| 1 | 两塔员建站/同 slug 并发唯一 | ◐ 单账号全链路完成（qt-e2e，site_id=1）；并发唯一由 MySQL 集成测试覆盖，双账号线上未测 |
| 2 | 公开仓库指定分支+目录发布 | ✅ mdn/beginner-html-site-styled@main，SHA 6c7a360，7 文件 66KB 上线 |
| 3 | 新 Commit 自动发现 | ◐ 轮询周期运行正常（300s、ETag、304 不建意图、限流延迟字段就绪）；"推新 commit 自动发布"需真实写权限仓库，未在线上演练 |
| 4 | SPA 深链可访问/缺失资源 404 | ✅ 深链 200（Accept html）、missing.js 404、图片/CSS 200 且 MIME 正确 |
| 5 | 失败不破坏在线版本 | ✅ 修复期 #1/#2 失败时在线版本不受影响；线上观察一致 |
| 6 | 回滚 + 不被轮询撤销 | ✅ #6 ROLLBACK→SUCCESS 切回 release 4；下一轮询（12:33）未产生新意图、未撤销回滚 |
| 7 | 越权矩阵 | ✅ 未登录 401、跨站/跨记录 404（×4）、回滚守卫 409（×2）、限流 429（第 10 次）；非塔员 403 线上未测（单元覆盖） |
| 8 | 隐藏路径/未知主机/站外读取 | ✅ .env/.git 404、未知子域 404、POST 405 |

补充可靠性：同 SHA 多次部署（#3/#4/#5 release 递增）✅；旧任务 SUPERSEDED 语义随版本号生效 ✅；跨域 Refresh 隔离 ✅（无 Origin 403、Pages Origin 403）。

线上验收小结：**核心链路 8 项中 6 项完整通过、2 项部分通过（缺口均在"需要第二个真实账号/可写仓库"与 DNS/证书，非功能缺陷）。**

## G. 遗留风险声明

- ~~匿名 GitHub 额度~~ 已配 Token（5000 次/小时，2026-10-25 验证）
- 单实例约束：只能一个启用 Pages 的应用实例挂载 /srv/quanta-pages
- 回滚关闭：`QT_PAGES_ENABLED` 移除后 `docker restart quanta-app` 即回退；在线站点由 nginx 直出不受影响
- 测试数据：qt_page_site(1 行)/qt_page_deployment(6 行) 保留供复查；测试账号已停用（status=1）未删除
- 凭据卫生：RAM 子账号 pages-cert-bot 仅 AliyunDNS 权限（AK 保存在服务器 /root/.acme.sh/account.conf，600）；
  GitHub Token 零 scope；两者都曾在聊天中出现过明文，如需严格隔离可在控制台轮换后更新服务器对应位置
- 未完成项：双账号并发建站、"推新 commit 自动发布"两条线上演练（需第二个真实塔员账号与一个可写仓库）；
  其余 8 条设计验收全部通过
