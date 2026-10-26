# Quanta Pages 前端对接与维护说明

> 状态：**已对接、已上线**（2026-10-25）。本文档面向后续维护者：接口契约、状态语义、轮询约定与测试注意事项。
> 后端实现在 `ruoyi-qt`（`QtPageSiteController` 等），本文只覆盖前端视角。

## 功能入口

- 路由：`/member/pages`（name=`MemberPages`，meta `{ title: '个人网站', portalAudience: 'member' }`，在 `src/router/portal-routes.js`）
- 入口：塔员首页快捷入口「个人网站」、「我的服务」页头「个人网站 →」链接
- 视图：`src/views/member/pages/index.vue` + `components/{SiteConfigForm,DeploymentHistory,DeploymentLogDialog}.vue`
- API 适配：`src/api/portal/pages.js`（全部走既有 `request`，前端不传 userId）

## API 契约（前缀 `/qt/pages/sites`，全部要求塔员登录）

| 方法 | 路径 | 请求 | 成功响应 | 失败状态码 |
| --- | --- | --- | --- | --- |
| GET | `/me` | — | `{code:200, data: site\|null}`；**data 键不存在 = 尚未创建（正常空状态）** | 401/403/503 |
| POST | `/` | `{slug?, repoUrl, branch?, publishDir?}`（slug 留空服务端随机分配） | `{code:200, data:{site, deployment}}` | 400 校验 / 409 slug 冲突或一人一站 / 429 |
| PUT | `/{siteId}` | `{repoUrl?, branch?, publishDir?}`（slug 不可改） | `{code:200, data:{site, deployment}}`（同时产生新 CONFIG 部署意图） | 400/404/429 |
| POST | `/{siteId}/deploy` | — | `{code:200, data: deployment}`（MANUAL 意图，快速返回） | 404/429/503 |
| GET | `/{siteId}/deployments` | `pageNum`、`pageSize`（默认 20，>100 钳到 100） | `{code:200, rows:[deployment], total}` | 404 |
| GET | `/{siteId}/deployments/{deploymentId}` | — | `{code:200, data: deployment}`（含 `logText`，≤32KiB 纯文本） | 404 |
| POST | `/{siteId}/rollback/{deploymentId}` | — | `{code:200, data: deployment}`（ROLLBACK 意图，不重新下载） | 404/409/429 |

- **错误处理约定**：Pages 控制器有专属异常映射，返回**真实 HTTP 状态码** + `AjaxResult.code`：
  401 未登录、403 非有效塔员、404 站点/记录不存在或非本人、409 业务冲突、429 限流（手动入口每账号 60 秒 10 次）、503 功能关闭或恢复中。
  `utils/request.js` 对 `code!==200` 抛错（`error.message`=后端 `msg`，`error.code`=数字码），页面直接展示 `error.message`，**不做任何 Mock 回退**。

## 关键字段语义（前端判断逻辑）

- `site`：`slug`（站点标识，创建后不可改）、`currentSha`（空 = 从未上线）、`currentDeploymentId`/`currentReleaseId`（当前在线版本）、
  `lastCheckedTime`/`lastCheckError`（最近一次分支检查，检查失败不影响在线版本）、`status`（ENABLED/SUSPENDED）。
- 站点地址固定拼接：`https://{slug}.page.quantacenter.com`（后端不返回完整域名）。
- `deployment.status` 状态机：`PENDING → RESOLVING → DOWNLOADING → VALIDATING → PUBLISHING → SUCCESS | FAILED`，
  任何非终态任务都可能被更新的意图置为 `SUPERSEDED`（如改配置、手动部署、回滚插队）。
  前端判断非终态统一用 `isDeploymentActive()`（`pages.js` 内维护，与后端集合一致）。
- `deployment.releaseAvailable`：实体文件是否仍存在。**可回滚 = `status==='SUCCESS' && releaseAvailable && deploymentId!==site.currentDeploymentId`**；
  已回收/失败/当前在线的版本点回滚会得到 409（后端兜底，前端按钮照常置灰）。
- `triggerType`：`CREATE`（首次创建）/`CONFIG`（改配置）/`POLL`（5 分钟自动检查发现新提交）/`MANUAL`/`ROLLBACK`。

## 轮询约定（index.vue 已实现，改动需保持）

- 仅当 **页面可见（`visibilityState==='visible'`）且存在非终态部署** 时，每 5 秒静默刷新 `getMyPageSite` + `getPageDeployments`；
  `visibilitychange` 与组件卸载时清理定时器；无执行中任务时不启动轮询。
- 刷新失败置 `pollError` 展示「重试」按钮并停止轮询，绝不伪造成功状态。

## 后端行为要点（前端无需处理，但理解后不误判）

- 创建/手动部署对「同配置且仍在排队」的意图幂等去重（返回已有任务，不会重复排队）。
- 改配置、手动部署、回滚都会使旧的非终态任务 `SUPERSEDED`（历史里常见，属正常）。
- 分支轮询每 5 分钟由后端自动完成，前端无需触发；`304 Not Modified` 不产生部署。
- 回滚不重新下载，秒级完成；`lastObservedSha` 与 `currentSha` 分开维护，回滚不会被下一次轮询撤销。
- 所有错误消息已脱敏（不含上游响应正文与服务器路径），可直接展示给成员。

## 测试注意

- 单元测试：参考 `src/api/portal/__tests__/pages.test.js` 与 `src/views/member/pages/__tests__/pages.test.js`；
  组件测试需 `vi.mock('element-plus')`（测试模式不自动注册 EP 组件）与 `vi.mock('@/api/portal/pages')`。
- e2e：`e2e/member-pages.spec.js` 是**自包含 API 拦截**（不依赖共享 fixture）。共享 `e2e/support/fixture.js` 的
  `responseFor()` 目前**没有** `/qt/pages/*` 分支——若要把 Pages 并入共享 fixture 的用例，必须先在 `responseFor` 增加分支，
  否则请求落入 `state.unexpected` 会让**所有** e2e 用例失败。
- 本地开发：`VITE_USE_MOCK` 默认开启且 mock 层没有 pages handler，无后端调试本页会得到 404 stub；
  调试请设 `VITE_USE_MOCK=false` 并连本地后端。

## 变更检查单

- 改 API 字段：`pages.js` 的 map 函数 + 后端 domain/Mapper XML + 本文档同步改。
- 新增接口：`QtPageSiteController` + `pages.js` + 本文档。
- 改状态集合：`pages.js` 的 `PAGES_ACTIVE_STATUSES` 必须与后端 `QtPageDeployment` 状态机一致。

## 站点数据 Open API（供成员自己的静态站调用）

后端在 `ruoyi-qt`：`QtPagesOpenController`（`/open/pages`，类级 `@Anonymous`）+ `QtPagesOpenServiceImpl`；
控制台说明组件是 `components/SiteApiGuide.vue`。这些接口**匿名只读**，按站点 slug 解析站主，
仅返回站主本人「塔员可见(MEMBERS)」的文章与这些文章的留言，`MANAGERS` 文章永远 404。

| 方法 | 路径 | data |
| --- | --- | --- |
| GET | `/open/pages/{slug}/posts?pageNum&pageSize` | `{items:[{postId,title,category,excerpt,commentCount,createdAt}], total, pageNum, pageSize}` |
| GET | `/open/pages/{slug}/posts/{postId}` | `{postId,title,category,content(Markdown),commentCount,createdAt,authorNickname}` |
| GET | `/open/pages/{slug}/posts/{postId}/comments?pageNum&pageSize` | `{items:[{commentId,parentId,content,authorNickname,replyToNickname,createdAt}], total, pageNum, pageSize}` |

- 错误：未知 slug / 非法 slug / 文章不属于站主或为 MANAGERS → 真实 HTTP 404；参数错误 400；限流 429（每 IP 60 秒 120 次）；`qt.pages.enabled=false` → 404。
- CORS：后端 `ResourcesConfig` 仅为 `/open/pages/**` 注册 Pages 子域来源（`https://*.{qt.pages.base-domain}`），
  `allowCredentials=false`；**不要**把 Pages 子域加进 `ruoyi.cors.allowed-origins`（那会削弱门户凭据隔离）。
- 隐私：评论只返回社区昵称（空串表示未设置），不含真实姓名/部门/userId/头像；文章正文为 Markdown 原文，
  论坛上传的图片是登录保护资源，公网页面无法加载（文档与控制台均已提示）。
- 限流/分页：`pageSize` 默认 20、最大 50；不递增论坛浏览量。
- 测试：后端 `QtPagesOpenServiceTest` / `QtPagesOpenControllerTest` / `QtPagesOpenMapperMySqlTest`（`QUANTA_PAGES_MYSQL_TEST=true`）；
  前端 `pages.test.js` 的 `site api guide` 用例；契约或字段变更需同步 `SiteApiGuide.vue` 与本文档。
