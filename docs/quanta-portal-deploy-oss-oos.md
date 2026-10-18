# Quanta Portal 生产发布（OSS + 云助手）

> **适用环境**：上海 ECS `i-uf6f9dq5t2g0nv3wj7nd`（`8.133.248.109`），站点 `https://www.quantacenter.com`  
> **不依赖**阿里云 MCP 的 `OOS_CodeDeploy`（该接口可能误报 OSS 未开通）。使用**已有 OSS 桶** + **OOS 云助手 RunCommand**。

---

## 1. 一次性准备

### 1.1 本机

| 项 | 说明 |
|----|------|
| Java 17、Maven、Node.js | 构建后端与 `Quanta-admin-web` |
| Python 3 + `oss2` | `pip install oss2`（上传发布包） |
| 阿里云 AccessKey | 建议 RAM 子账号，权限含目标桶 `PutObject`、`GetObject` |
| 凭证文件 | 默认读取 `%USERPROFILE%\.aliyun\credentials`（`[default]` 段） |

复制配置模板并改名（勿提交 Git）：

```powershell
copy .code_deploy\deploy.env.example .code_deploy\deploy.env.local
# 编辑 deploy.env.local，填写 OSS_BUCKET 等
```

### 1.2 服务器（仅需做一次）

将仓库内 [`.code_deploy/remote-deploy.sh`](../.code_deploy/remote-deploy.sh) 放到 ECS：

```text
/opt/ruoyi/remote-deploy.sh
chmod +x /opt/ruoyi/remote-deploy.sh
```

可通过云助手执行（示例）：

```bash
# 在 ECS 上创建脚本后粘贴 remote-deploy.sh 内容，或 scp 上传
chmod +x /opt/ruoyi/remote-deploy.sh
```

目录约定（已存在，勿改）：

| 路径 | 作用 |
|------|------|
| `/opt/ruoyi/ruoyi-admin.jar` | 当前后端 |
| `/opt/ruoyi/admin/` | 当前前端静态资源 |
| `/opt/ruoyi/releases/<发布名>/` | 每次发布的解压目录 |
| `quanta-app` / `quanta-mysql` | Docker 容器 |

---

## 2. 日常发布（推荐一条命令）

在项目根目录 PowerShell：

```powershell
.\.code_deploy\publish.ps1
```

脚本会依次：

1. 调用 `build-release.ps1`：`mvn package`（跳过测试）+ 前端 `npm run build`，生成  
   `.code_deploy/release/quanta-portal.tar.gz`
2. 上传到 OSS：`deploy/quanta-portal-<时间戳>.tar.gz`（桶名见 `deploy.env.local`）
3. 在终端打印 **云助手一键命令**（含签名下载 URL，约 1 小时有效）

在 [ECS 云助手 / OOS 批量执行命令](https://oos.console.aliyun.com/) 中选择实例 `i-uf6f9dq5t2g0nv3wj7nd`，粘贴打印的 **整段 shell** 执行。

成功标志：

- 输出含 `DEPLOY_OK <发布名>`
- `docker ps` 中 `quanta-app` 为 `healthy`
- `https://www.quantacenter.com` 返回 200

---

## 3. 仅打包、不上传

```powershell
.\.code_deploy\build-release.ps1
```

产物：`.code_deploy/release/quanta-portal.tar.gz`（内含 `ruoyi-admin.jar`、`admin-dist.tgz`、`sql/*.sql`）。

---

## 4. 云助手执行的脚本在做什么

`publish.ps1` 生成的远程命令等价于：

1. `curl` 从 OSS 下载 tar 到 `/opt/ruoyi/releases/<发布名>/`
2. 解压后执行：`bash /opt/ruoyi/remote-deploy.sh <发布名>`
3. `remote-deploy.sh`：替换 JAR、同步前端、按文件名顺序执行 `sql/*.sql`（已执行过的语句若报错会忽略）、`docker restart quanta-app`、reload nginx

**新增库表/数据变更**：把新的 `.sql` 放进仓库 `sql/` 目录，下次打包会自动打进 tar。

---

## 5. 使用 Cursor / MCP 代发（可选）

对助手说明：

- 实例：`i-uf6f9dq5t2g0nv3wj7nd`，地域 `cn-shanghai`
- 先本地 `publish.ps1` 或手动上传 OSS，再调用 **`OOS_RunCommand`**，**不要**使用 `OOS_CodeDeploy`
- 远程命令**不要**以 `#!/bin/bash` 开头（云助手会当成命令名导致 exit 127）

---

## 6. 常见问题

| 现象 | 处理 |
|------|------|
| MCP `OSS_SERVICE_NOT_ACTIVATED` | 控制台 OSS 已开通即可忽略；用本文 OSS 上传 + RunCommand |
| `source .env` 报 `not found` | 已改为只读取 `MYSQL_ROOT_PASSWORD` 一行，勿在云助手脚本里 `source` 整个 `.env` |
| SQL 重复执行报错 | 脚本对每条 SQL 使用 `|| true`；仍失败请检查 SQL 是否幂等 |
| 签名 URL 过期 | 重新运行 `publish.ps1` 生成新链接 |

---

## 7. 回滚（手动）

```bash
REL=<上一次发布目录名>
BASE=/opt/ruoyi/releases/${REL}
install -m 644 "${BASE}/ruoyi-admin.jar.prev" /opt/ruoyi/ruoyi-admin.jar
# 若有 admin 备份再 rsync；否则从更早的 release 目录取
docker restart quanta-app
```

建议保留 `/opt/ruoyi/releases/*` 下最近几次发布目录。
