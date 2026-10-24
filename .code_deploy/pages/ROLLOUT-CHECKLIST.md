# Quanta Pages 真实联调待办清单（上线前逐项核对）

> 本轮交付只完成代码与本地工件（Task 1–7 全部 + 本目录的模板/脚本）。
> 以下条目**均未验证**，必须在真实服务器上逐项完成并记录结果；任何一项未通过都不得开启 `QT_PAGES_ENABLED=true`。

## A. 环境事实核实（只读检查，先做）

- [ ] 服务器运行用户与 UID：`docker inspect quanta-app --format '{{.HostConfig.User}}'`；确定 Pages 专用 UID/GID
- [ ] 现有容器启动参数完整导出（Binds/Env/Network/RestartPolicy），重建时原样保留
- [ ] 磁盘容量：Pages 根目录所在卷剩余空间 ≥ 预期站点数 × 2 GiB 配额余量
- [ ] GitHub 出网：从容器内 `curl -sS -o /dev/null -w '%{http_code}' https://api.github.com` 返回 200/403（不是超时）
- [ ] GitHub Token 预算：有 Token 时 core 限额 5000/h；无 Token 仅 60/h（按出口 IP 共享），多人使用必须配 Token
- [ ] DNS：`dig +short alice-lab.page.quantacenter.com` 与 `*.page.quantacenter.com` A 记录指向本服务器
- [ ] 443 防火墙/安全组放行；80 仅做跳转
- [ ] 通配符证书：`certbot certonly --dns-* -d '*.page.quantacenter.com'`（DNS 验证），自动续期已启用且 `certbot renew --dry-run` 通过

## B. 目录与权限

- [ ] 执行 `prepare-pages.sh /srv/quanta-pages <UID> <GID>`，确认属主/权限输出
- [ ] Nginx 用户可遍历 `/srv/quanta-pages/sites`（脚本末尾自检命令）

## C. 数据库

- [ ] 备份：`server-backup.sh`（或 mysqldump ry-vue）成功且校验通过
- [ ] 执行 `sql/2026-10-25-qt-pages.sql`（幂等，可重复执行），确认两张表创建
- [ ] 再次执行一遍确认幂等，不报错

## D. 应用配置

- [ ] `CORS_ALLOWED_ORIGINS` 设为**精确门户来源**（如 `https://www.quantacenter.com`）——
      开启 Pages 后带 `*`/通配符会拒绝启动（TrustedPortalOriginPolicy）
- [ ] `TOKEN_REFRESH_SECURE=true`（生产 HTTPS），Refresh Cookie 保持 host-only（无 Domain）
- [ ] `QT_PAGES_GITHUB_TOKEN` 注入环境变量（不写入仓库/脚本）
- [ ] `QT_PAGES_ROOT_DIR=/srv/quanta-pages`、`QT_PAGES_BASE_DOMAIN=page.quantacenter.com`
- [ ] 重建容器挂载 Pages 根目录（container-mount.example.md），确认应用可写、Nginx 可读
- [ ] 启动日志确认：`Quanta Pages 运行时已初始化` → `启动恢复完成，已就绪`

## E. Nginx

- [ ] 按 `nginx-pages.conf.example` 落地独立 server 块（证书路径按实际调整）
- [ ] `nginx -t` 通过后才 reload；确认未引入任何 FastCGI/代理执行配置
- [ ] 之后**成员每次部署都不再 reload Nginx**

## F. 真实验收（两个测试塔员 + 一个公开静态产物仓库）

按设计第 7 节逐条验收并记录（域名、目标 SHA、HTTP 状态、时间）：

- [ ] 1. 两名塔员分别创建站点，域名不同；同 slug 并发创建只成功一次
- [ ] 2. pages 分支 + 指定 publishDir 发布成功，页面内容与提交 SHA 一致
- [ ] 3. 新 Commit 在下一个 5 分钟周期自动发布；控制台显示检查时间/限流延迟
- [ ] 4. SPA 深链刷新可用；缺失 JS/CSS 返回 404（verify-pages.sh 全绿）
- [ ] 5. 缺 index、恶意归档、超限、断网、磁盘写满均不破坏在线版本
- [ ] 6. 回滚到保留版本成功；分支头未变的下一次轮询不撤销回滚；新提交继续自动部署
- [ ] 7. 越权矩阵：未登录 401、非塔员 403、A 访问 B 的站点/详情/日志/回滚 404、伪造 userId 无效、管理员无本人接口旁路
- [ ] 8. 未知站点/非法 Host/隐藏路径/穿越路径读不到站外文件；错误站点不返回门户首页
- [ ] 补充：同 SHA 失败重试成功；改 publishDir 的同 SHA 可重新部署；旧任务晚完成不覆盖新目标；
      PUBLISHING 切换前后分别 `docker kill quanta-app` 重启可恢复（内容与 DB 对账）；
      容器重建后站点仍在；从 `*.page.quantacenter.com` 页面发起的 /auth/refresh 请求被 403 拒绝

## G. 遗留风险声明

- 无 Token 时 GitHub 匿名额度为 60 次/小时/出口 IP，不支撑多人规模；上线必须配 Token
- 多实例部署不在 MVP 范围：必须保证**只有一个**启用 Pages 的应用实例挂载该目录
- 本清单任何一项失败时：保持 `QT_PAGES_ENABLED=false` 回退，在线站点由 Nginx 直接提供不受影响
