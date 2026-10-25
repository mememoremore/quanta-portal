# Quanta Pages 部署工件

成员静态网站托管（`{slug}.page.quantacenter.com`）的服务器侧配套。后端与前端实现见
`ruoyi-qt`（`qt.pages.*` 配置、`/qt/pages/sites` 接口）与 `Quanta-admin-web`（`/member/pages` 控制台）。

## 文件

| 文件 | 用途 |
| --- | --- |
| `nginx-pages.conf.example` | 宿主机 Nginx 独立 server 块模板（静态直出、隐藏路径 404、SPA 深链回退、未知主机 404） |
| `prepare-pages.sh` | 创建 Pages 根目录并设置应用 UID/GID 权限（幂等，不碰其他目录） |
| `container-mount.example.md` | quanta-app 容器追加挂载的重建步骤与核对命令 |
| `verify-pages.sh` | 对测试站点域名跑 Nginx 侧验收（首页/深链/缺失资源/隐藏路径/POST/响应头） |
| `ROLLOUT-CHECKLIST.md` | **上线前逐项核对的联调清单（含环境事实核实与 8 条验收）** |

## 发布链路（启动后）

```text
塔员推送 pages 分支
  → 应用每 300s 轮询分支头（ETag 条件请求）
  → 下载指定 SHA 的 tar.gz（≤100MiB，只走 api.github.com/codeload.github.com）
  → 安全解包校验（≤200MiB 展开、拒绝穿越/链接/隐藏路径）
  → releases/{deploymentId} 固化 + current 原子符号链接切换
  → Nginx 直接读 /srv/quanta-pages/sites/<slug>/current（无需 reload）
```

成员部署**不需要**任何服务器操作；服务器操作只在首次上线（见 ROLLOUT-CHECKLIST.md）与容器重建时发生。

## 边界

- 单机 Linux、本地文件系统、一个启用 Pages 的应用实例
- Pages 根目录独立于 `ruoyi.profile`，`/profile/**` 静态映射、门户发布脚本与用户上传均触不到
- `qt.pages.enabled=false`（默认）时不建目录、不联网；启用前置条件全部在 ROLLOUT-CHECKLIST.md
