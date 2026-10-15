# Quanta Web 双 Token 鉴权对接说明

> **读者**：`Quanta-admin-web`（新生门户 / 塔员中心 / 管理后台，同一套 SPA）  
> **版本**：2026-10-15  
> **状态**：后端与 Web 前端已按本文实现。UniApp 不在范围内。生产环境需配置 `TOKEN_REFRESH_SECURE=true` 后重新部署才会生效。

---

## 1. 行为

| 项目 | 约定 |
|------|------|
| Access | 响应字段名仍是 `token`。Redis TTL **15 分钟**，请求过程中**不再滑动续期** |
| 请求头 | `Authorization: Bearer <token>` |
| Refresh | Cookie 名 `quanta_refresh`，`HttpOnly`、`Path=/`、`SameSite=Lax`。前端 JS 读不到 |
| Refresh 有效期 | 服务端角色 `admin` / `ceo` / `qt_mgmt` / `qt_manager`，或权限 `*:*:*`：**1 天**；其余用户：**7 天**。与从哪个入口登录无关 |
| 刷新 | `POST /auth/refresh`，浏览器自动带 Cookie |
| 失败形态 | 与若依一致：HTTP 200，JSON `code: 401`。权限不足是 `code: 403`，**不刷新** |
| 轮换 | 每次刷新作废旧 Refresh。再次出示旧 Cookie 会撤销该用户全部会话 |

Access 仍放在 `localStorage` 键 `quanta_admin_token`，这样刷新页面在 15 分钟内不必先打刷新接口。Cookie 只负责过期后续期。

---

## 2. 接口

### 2.1 登录 `POST /login`

请求体不变（`username`、`password`、`code`、`uuid`、`loginType`）。

成功时响应体仍包含 `token`，并增加：

```json
{
  "code": 200,
  "token": "<access>",
  "expiresIn": 900
}
```

同时：

```http
Set-Cookie: quanta_refresh=<opaque>; Path=/; HttpOnly; SameSite=Lax; Max-Age=604800
```

生产 HTTPS 还会带 `Secure`（`TOKEN_REFRESH_SECURE=true`）。管理角色的 `Max-Age` 为 86400。

`expiresIn` 固定为 900 秒。登录页继续只读 `token`。

### 2.2 刷新 `POST /auth/refresh`

- 不需要 `Authorization`。
- 必须 `withCredentials: true`，否则 Cookie 不会发出。
- 请求体为空。

成功：

```json
{
  "code": 200,
  "token": "<new-access>",
  "expiresIn": 900
}
```

并 `Set-Cookie` 轮换 Refresh。

失败：

```json
{
  "code": 401,
  "msg": "登录状态已过期，请重新登录"
}
```

同时清除 Cookie。

### 2.3 退出 `POST /logout`

撤销**当前这一对** Access + Refresh，并清除 Cookie。同一账号的其他设备不受影响。

### 2.4 会撤销该用户全部会话的操作

- 个人修改密码、邮件重置密码、管理员重置密码
- 账号停用
- 新生接受录用成功

这些场景下旧 Refresh 不能再换票。接受录用后前端应进入塔员登录页。

---

## 3. Web 端已经做的事

| 文件 | 行为 |
|------|------|
| `src/utils/request.js` | `withCredentials: true`。业务码 401 时全页面只发一次 `POST /auth/refresh`，成功后重试原请求一次；刷新自身失败或重试仍 401 才走 `notifyUnauthorized`。403 不刷新 |
| `src/api/auth.js` | 导出 `refreshSession()` |
| `src/stores/user.js` | `restoreSession()` 用 Cookie 换 Access |
| `src/router/guard.js` | 本地没有 Access 时先尝试恢复，失败再去对应登录页 |
| `src/config/mock-api.js` | `/auth/refresh` 属于真实鉴权路径。全量 Mock 时默认返回 401 |

本地 Vite（`127.0.0.1:5173`）请求 `127.0.0.1:8080` 属于同站跨端口，`SameSite=Lax` 可以带上 Cookie。后端 CORS 已 `allowCredentials`。生产若改成跨站，`CORS_ALLOWED_ORIGINS` 必须写成具体源，不能用 `*`。

---

## 4. 联调检查

- [ ] 登录响应有 `token`、`expiresIn: 900`，并且有 `quanta_refresh` Cookie（HttpOnly）
- [ ] 等 Access 过期后，业务请求触发一次刷新并重试成功
- [ ] 并发多个 401 只打一次 `/auth/refresh`
- [ ] 旧 Refresh 再提交会得到 401，且该用户其他会话一并失效
- [ ] 退出后 Cookie 被清除
- [ ] 403 不会触发刷新
- [ ] 改密、停用、接受录用之后，`/auth/refresh` 返回 401
