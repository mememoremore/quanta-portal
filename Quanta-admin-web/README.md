# Quanta 统一 Web 门户

Vue 3 + Vite 单站点，包含三类入口：

- `/login/freshman`：新生门户，招新、面试进度、活动和账号安全。
- `/login/member`：塔员门户，通讯录、社区论坛、学习资料、云端个人档案和自助服务。
- `/admin/login`：原管理后台，成员、招新、活动、资料及运营管理。
- `/login`：兼容旧地址，自动跳转到管理端登录。

生产站点从根路径 `/` 发布，三个身份共用同一份 `index.html` 和 `/assets/*` 资源。

## 本地运行

```bash
npm install
npm run dev
```

环境变量示例：

```dotenv
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://127.0.0.1:8080
VITE_APP_TITLE=Quanta 统一门户
```

开发和生产默认都应连接真实后端。完整 Mock 只用于独立 UI 开发；局部 Mock 仅限 `src/config/mock-api.js` 中的显式白名单。个人档案不再读取或写入浏览器本地名片；论坛与报名草稿仍按各自功能保存。品牌内容使用静态资源。

接口失败不会自动回退为 Mock 成功。写接口、文件下载及身份授权必须使用真实后端结果。

## 身份与权限

- 新生登录发送 `loginType: "0"`。
- 塔员和管理登录发送 `loginType: "1"`。
- 门户身份以 `/getInfo.user.isQuantaMember` 为准；本地 audience 仅用于记住登录入口。
- 管理动态路由统一位于 `/admin/*`，由服务端角色、权限和 `/getRouters` 决定。
- 后端对通讯录、资料、图书、工位和塔服接口再次校验塔员身份，前端路由不代替服务端授权。

## 真实接口范围

- 认证：`/login`、`/logout`、`/captchaImage`、`/getInfo`、`/getRouters`。
- 新生：`/qt/interview/*`、`/qt/activity/list`、`/qt/signup/*`。
- 塔员内容：`/qt/member/list`、`/system/notice/*`、`/qt/materials/*`。
- 塔员服务：`/qt/book/*`、`/qt/borrow/*`、`/qt/workstation/*`、`/qt/reservation/*`、`/qt/item/*`、`/qt/order/*`。
- 个人网站（Pages）：`/qt/pages/sites`（创建/本人站点/更新/部署/历史/详情/回滚），页面在 `/member/pages`；站点静态内容由服务器 Nginx 提供，不经前端请求。
- 管理端：`/dashboard/*`、`/qt/*`、`/system/*` 及文件上传下载接口。

塔服缺少可信价格时只保存 `DRAFT`。头像复用 `POST /system/user/profile/avatar`（`avatarfile`）；云端档案使用 `GET/PUT /qt/member/profile`，成员主页使用 `GET /qt/member/{userId}/profile` 与 `/qt/member/{userId}/posts`。后面三个新增契约需要后端实现；真实接口未接通时明确显示错误，不伪装本地保存成功。

## 质量检查

```bash
npm run test:run
npm run lint
npm run build
```

### Playwright 浏览器冒烟测试

首次在本机或 CI 安装依赖与 Chromium：

```bash
npm ci
npx playwright install chromium
```

运行 Web 端到端测试：

```bash
npm run test:e2e
```

测试配置在 `playwright.config.js`，用例在 `e2e/`。运行时会构建并预览本地 Web 页面（`127.0.0.1:5177`），API 指向不会启动服务的测试地址，由 Playwright 拦截并返回固定数据；未声明的 API 或外部请求会让测试失败。当前覆盖入口和移动端滚动、三种身份的登录跳转、注册校验、新生报名、塔员论坛及管理端招新权限。测试账号是夹具里的虚构账号，不是线上账号，也不会发送真实邮件或提交真实报名。

失败截图与 trace 位于 `test-results/`；CI 的 HTML 报告位于 `playwright-report/`，这两个目录都不提交 Git。此套测试验证前端交互和请求形状，不代替真实后端联调；Vitest 仍负责组件、API 适配和工具函数测试。

## 目录

```text
src/
  api/portal/       新生与塔员真实接口适配（含 pages.js 个人网站接口）
  layout/           管理布局和门户布局
  router/           三身份路由与管理动态路由
  views/freshman/   新生 Web 页面
  views/member/     塔员 Web 页面（含 pages/ 个人网站控制台）
  views/login/      管理端登录
  views/portal-login/
  views/portal-security/
```
