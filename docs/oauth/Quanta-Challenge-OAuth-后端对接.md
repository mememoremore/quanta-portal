# Quanta → Challenge：官网后端 OAuth/OIDC 对接契约

更新：2026-10-18。本文为本轮前端及 Challenge 接入代码的接口契约，**官网 OAuth 服务端尚需后端实现；目前不能直接上线真实跨站登录**。本次 OAuth 改动不修改官网 Java 登录/权限业务代码，未执行真实数据库迁移。官网提交仅包含官网侧源码、测试和本文；Challenge 代码独立保留在其本地仓库，未随本次官网提交推送。

## 一、最终确认的产品行为

- Challenge 原 QQ 按钮替换为“Quanta 登录”，与生物认证并排，无新增行、无按钮下提示。
- 只有有效塔员账号可以使用此登录方式。原 Challenge 邮箱、通行密钥和 CLI 登录不因此关闭。
- 在 Quanta 官网完成塔员登录及授权，**完成页仍在官网**，显示“登录成功，欢迎回来 / Quanta 身份验证及 Challenge 授权已完成”。
- 用户点击“返回 Challenge”后，官网服务端才签发短期授权码并回跳 Challenge 回调。Challenge 完成验签、换码、建立自己的登录态后进入 `/app/dashboard`，不回原题目、不回登录页。
- 第一次未绑定时先选择创建新账号或绑定原账号；绑定不迁移原做题数据。首期绑定通过原 Challenge 邮箱密码验证，并再次明确确认双方姓名；不根据同名或相同邮箱自动合并。只有通行密钥而无邮箱密码的特殊旧账号，需要维护者另行确认接入方案，不能跳过所有权校验。
- 官网完成页只说明 Quanta 身份验证及授权已经完成，**不声称此时已建立 Challenge Cookie**。

## 二、两端责任

| 官网后端 | 官网前端（本轮已编写） | Challenge（本轮已编写） |
| --- | --- | --- |
| 标准 OIDC 提供方、客户端登记、PKCE、签名公钥、单次授权码、浏览器绑定交互、塔员准入 | `/oauth/authorize?interaction=…`、原塔员登录能力、身份确认、拒绝/同意、留在官网的完成页、点击安全续接 | Nitro 服务端 state/nonce/PKCE、真实签名校验、身份绑定、本站 Token/CSRF 初始化、固定首页 |

不能把现有 `/login`、`/getInfo` 和业务 JWT 直接当成 OAuth 服务。建议用与现有 Java 17 / Spring Boot 4.0.6 / Spring Security 版本兼容的标准 OIDC 服务端组件，独立安全链，不手写一个仅“能跑通”的授权协议。

## 三、标准协议和固定配置

生产建议：

```text
issuer: https://www.quantacenter.com
client_id: quanta-challenge
client authentication: client_secret_basic
response_type: code
grant_type: authorization_code
redirect_uri: https://challenge.quantacenter.com/api/auth/quanta/callback
scope: openid profile
code_challenge_method: S256（强制）
ID Token signing algorithm: RS256
```

客户端 secret 至少 32 字符高熵随机值，仅双方后端运行环境持有，不进入前端、public runtimeConfig、Git 或构建镜像。签名私钥与原业务 JWT 密钥隔离。正式/测试客户端和回调分别登记，不使用通配域名，不接受用户传入的客户端名称、图标、回调作为可信数据。

`issuer` 必须与发现文档、ID Token 的 iss 完全一致。Challenge 目前要求授权、换码、JWKS 端点与 issuer 同源且 HTTPS。若计划另设 auth 子域，请先同步改环境配置及双方契约，不要让前端临时放宽白名单。

提供 `/.well-known/openid-configuration`，包含 issuer、authorization_endpoint、token_endpoint、jwks_uri、支持 code、RS256、S256、client_secret_basic 的元数据。接口部署可以带网关前缀，发现文档给出准确的绝对地址。标准协议接口必须按 OAuth/OIDC 格式响应，**不能套业务接口 `{code:200,data:…}`**。

授权入口校验：response_type、client_id、精确 redirect_uri、scope、state、nonce、S256 challenge 均有效。非法回调必须本地报错，不跳转到未登记地址。保存所有原始参数到服务端短期事务，再跳转官网前端 `/oauth/authorize?interaction=<opaque-id>`。

## 四、浏览器登录桥接：不破坏现有主站登录

现有 Access Token 在 localStorage，跳转 GET 不会携带 Bearer。官网前端会恢复原 Refresh Cookie 会话，并通过 Bearer 调 `/getInfo`，但 **OAuth 服务端不能信任前端传来的姓名、member 标记或用户 ID**。

交互需有独立、HttpOnly、Secure、Host-only、SameSite=Lax 的浏览器绑定 Cookie；interaction ID 至少 32 字符、至多 128 字符的随机 base64url。事务绑定客户端、原始请求、浏览器、状态和有效期，不是可单凭 URL 领取身份的票据。

现有 SecurityConfig 全局 csrf.disable()，新增交互的 Cookie 认证操作需要独立 CSRF/Origin 校验，不能照搬。决策接口验证 Bearer 与浏览器绑定，续接接口验证已审批账号和仍有效的主站认证上下文；退出/切换账号后旧审批不得用于新账号。不得扩大原 Refresh Cookie 的 Domain 或把它发给 Challenge。

## 五、官网前端实际调用的业务接口

这些是交互 UI 的私有业务 API，不是标准 OAuth 端点，沿用 RuoYi envelope。前端通过 `VITE_API_BASE_URL` 请求，强制携带 credentials；不走 Mock handler，不把 401 重定向成管理员登录。

### 1. GET /oauth2/interaction/{interactionId}

依赖浏览器绑定 Cookie，允许未登录用户读取最小展示信息。禁止跨域任意读取；no-store。不要返回原始 state、PKCE verifier、code 或 Token。

```json
{
  "code": 200,
  "data": {
    "interactionId": "43-character-random-base64url-id-required-here",
    "status": "pending",
    "expiresAt": "2026-10-18T08:10:00Z",
    "csrfToken": "32-to-128-character-random-base64url-token",
    "consentRequired": true,
    "scopes": ["openid", "profile"],
    "client": {
      "clientId": "quanta-challenge",
      "name": "Quanta Challenge",
      "origin": "https://challenge.quantacenter.com",
      "redirectUri": "https://challenge.quantacenter.com/api/auth/quanta/callback"
    }
  }
}
```

ID 和 csrfToken 示例是格式说明，不是生产值。实际均须随机生成。status 支持 pending/approved；approved 还必须带字符串 subjectUserId（仅当前绑定浏览器且已认证本人可读），用于刷新完成页时与 `/getInfo.user.userId` 核对，不能恢复其他账号的审批。过期返回 410。

### 2. POST /oauth2/interaction/{interactionId}/decision

```text
Authorization: Bearer <主站现有 Access Token>
X-CSRF-Token: <GET 返回的 csrfToken>
Content-Type: application/json
Cookie: <浏览器绑定 Cookie>
```

批准请求：`{"action":"approve"}`。

服务端重新读取数据库：账号未删除、未禁用且 `isQuantaMember == "1"`，验证事务归属、Origin、CSRF、登录会话、客户端/范围。不能因为 CEO/admin/部门职位或前端 localStorage audience 而豁免。重复审批同一合法事务可幂等，冲突账号必须拒绝。

批准响应：

```json
{
  "code": 200,
  "data": {
    "status": "approved",
    "continuePath": "/oauth2/interaction/<相同interactionId>/continue",
    "expiresAt": "2026-10-18T08:10:00Z"
  }
}
```

**此时只保存审批，不提前签发授权码。** 前端验证 continuePath 精确匹配当前事务后，在官网显示完成页；避免用户停留时把 60 秒授权码放过期。

拒绝请求：`{"action":"deny"}`。允许根据有效事务 Cookie/CSRF 拒绝，不应要求未登录用户先登录才能取消。

拒绝响应：

```json
{ "code": 200, "data": { "redirectUrl": "https://challenge.quantacenter.com/api/auth/quanta/callback?error=access_denied&state=<原始state>" } }
```

前端只允许精确登记的回调，参数仅 error/error_description/state/iss，拒绝 code、重复参数、片段及外链。

### 3. GET /oauth2/interaction/{interactionId}/continue

由完成页“返回 Challenge”触发浏览器导航。验证浏览器 Cookie、已审批状态、原认证账号、最新塔员资格、事务有效期，原子单次消费后签发新鲜授权码，再 302/303 跳转到登记回调：

```text
https://challenge.quantacenter.com/api/auth/quanta/callback?code=<一次性授权码>&state=<原state>
```

此 GET 不会自动携带 localStorage Bearer，必须使用前一步服务端保存且仍有效的浏览器认证/审批上下文，不得要求把 Access Token 加入 URL。不能接收前端指定的 returnTo 或任意 redirect。

用户切换/退出账号、客户端停用、资格变更、过期/重复续接，都拒绝本次请求并显示明确错误；不能返回业务 JSON 让浏览器停在白屏。完成页只在用户点击后导航，不自动回登录页。

## 六、Token 与身份声明

换码接受标准 form-urlencoded、客户端 Basic 鉴别、精确 redirect_uri、code_verifier。授权码建议 60 秒有效，校验成功后原子消费，绑定 client/redirect/user/scope/challenge，不得接受遗漏 PKCE 或 plain 降级。

ID Token 必须包含：

```json
{
  "iss": "https://www.quantacenter.com",
  "sub": "不可变且不可复用的用户身份字符串",
  "aud": "quanta-challenge",
  "iat": 1792300800,
  "exp": 1792301100,
  "nonce": "原授权请求nonce",
  "name": "实名显示字段的名称",
  "quanta_member": true
}
```

上面时间戳仅展示字段类型；实际按签发时刻生成。sub 不能用姓名、昵称、邮箱、学号，Long 应转换字符串避免 JS 精度问题。name 沿用现有实名显示逻辑（user.nickName，缺省 userName），不取 communityNickname。

`quanta_member` 必须是真正的 JSON boolean true，只有服务端核验有效塔员后签发。Challenge 会校验标准签名、iss/aud/exp/nonce 和该声明，缺失/false 均拒绝。

不提供主站通用 Refresh Token、权限全集、手机号、密码；客户端不申请 offline_access。Challenge 当前不持久化主站 Access/Refresh/ID Token，只存 issuer+sub 身份键。头像声明如提供必须受允许来源约束；首期 Challenge 不下载外站头像，不将 URL 当 Image 外键，保留旧头像，新用户默认首字头像。

JWKS 提供 kid 对应 RS256 公钥，私钥不公开；轮换保留验证窗口。OAuth Token 不能被原业务 API 当全权限主站 Token 使用，须有独立 audience/用途隔离。

## 七、错误、日志、跨域与生命周期

- 业务 API：401 未登录/过期、403 非塔员或 CSRF、409 已处理/账号冲突、410 事务过期、429 限流、503 未配置/不可用。保持 `{code,msg}`，不泄露凭据或存在性信息。
- 标准端点按 OAuth error 返回；无效 redirect_uri 不跳外站，其他已验证回调保留原 state。
- 交互建议 10 分钟，批准不无限延长；授权码 60 秒。多标签页按独立 interaction/state 存储，不能一个全局 state 覆盖其他标签。
- Redis 原子条件更新/消费，不能 GET 后 DEL 的竞态写法；失败时不消费他人浏览器的事务。
- 授权、续接、回调、Token 响应 no-store/no-referrer，授权页禁止嵌入；反向代理、应用日志、APM 不能记录完整 query、Cookie、Authorization、csrfToken、密码、响应 Token。
- 若官网 API 跨源，只允许实际官网前端 origin+credentials+必要 headers，不能 `Access-Control-Allow-Origin:*` 搭配凭据。推荐同源网关，确保顶层授权与交互请求使用同一组浏览器 Cookie。
- 首期退出只退出当前站点。主站禁用后新的 OAuth 登录会拒绝，但已有 Challenge 会话不会自动立即退出；若要求实时联动需另设计撤销/通知，不应误称已实现。

## 八、Challenge 接入配置与数据库

代码目录：`C:/Users/LXZ/Desktop/quanta-challenge-dev/quanta-challenge`，基于本轮拉取的 `6c5eafd07d78f29a736989b59c1ede8fb44f9645`，本地分支 `codex/quanta-oauth-login`。旧的 `quanta-challenge-dev/quanta-challenge-dev` 未改动。

服务端环境：QUANTA_OAUTH_ENABLED=false（默认）、QUANTA_OAUTH_ISSUER、QUANTA_OAUTH_APP_ORIGIN、QUANTA_OAUTH_CLIENT_ID、QUANTA_OAUTH_CLIENT_SECRET。需要把变量注入 Nitro 运行时容器，不是只写到镜像构建参数里。OAuth Cookie 始终 Secure，真实联调需要 HTTPS；不提供生产不安全回退。

已准备两份未执行的迁移：添加 QUANTA enum，再创建 QUANTA 用户侧部分唯一索引；原 `(provider,providerId)` 唯一约束保留。providerId 是 JSON 编码的 `[issuer,sub]`，双方唯一约束防止一对多和并发重复账号。迁移前检查当前环境迁移历史，本轮不自动操作任何真实数据库。

首次创建 User/Auth/UserStatistic 在同一事务；新角色固定 USER，绑定原账号保留原有 Challenge 角色。Challenge不会按主站 CEO/经理分配 ADMIN。上线前先执行迁移、生成客户端、配置双方凭据、联调，最后再开启开关。

## 九、联调验收

1. 主站未登录、已登录、Access 过期但 Refresh 有效、彻底过期均不误跳管理员登录。
2. 新生/普通管理员无塔员标记、停用账号、撤销塔员资格均不能签发有效凭证。
3. 主站完成页只在批准后显示，停留期间不产生即将过期的 code；点击返回后才续接且进入 Challenge 首页。
4. PKCE、state、nonce、issuer、audience、签名、过期、非塔员声明错误全部拒绝；code/state 重放及换浏览器复用失败。
5. 未绑定账号明确选择创建/绑定；同名邮箱不自动关联，冲突不覆写，重复点击不创建重复用户。
6. Challenge Cookie+CSRF 初始化后 getUser 和受保护操作均可用；URL 不包含登录 Token，正常浏览器多标签页可用。
7. HTTPS 桌面 Chrome、手机 Chrome/Safari/微信内置浏览器检查；无弹窗依赖，按钮不挤出。
8. 原邮箱、生物认证、CLI device flow 和主站新生/塔员/后台权限回归。

## 十、本轮验证与已知阻碍

官网前端全量单测 106 文件 / 411 项通过；build 通过；lint 0 错误、18 条既有警告。Challenge 独立 OAuth 测试已覆盖真实 OIDC 客户端的 RS256 验签、nonce/aud/iss/expiry/member 拒绝以及原子 state 测试。

最终针对性复测：官网 23 项测试和 4 项桌面/H5 浏览器用例通过；Challenge 独立 OAuth 17 项通过，10 个新增服务器文件的范围内语法/语义检查零诊断；两份修改/新增 Vue 页面可编译。浏览器截图使用受控 API 测试数据，不是线上真实登录成功。

Challenge 最新上游本身存在无关文件的注释残片语法错误，导致完整 server/app 类型检查失败，涉及 recalculate-achievements、static 文件路由、judge-complete、device-flow、daily、rank。本轮不混改这些业务文件，不能宣称完整 Challenge 构建成功。完整联调还需要官网后端实现本契约，并在测试数据库验证迁移与绑定事务。
