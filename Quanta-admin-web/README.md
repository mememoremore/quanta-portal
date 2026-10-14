# Quanta 统一 Web 门户

Vue 3 + Vite 单站点，包含三类入口：

- `/login/freshman`：新生门户，招新、面试进度、活动和账号安全。
- `/login/member`：塔员门户，通讯录、社区论坛、学习资料、个人名片和自助服务。
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

开发和生产默认都应连接真实后端。完整 Mock 只用于独立 UI 开发；局部 Mock 仅限 `src/config/mock-api.js` 中的显式白名单。

接口失败不会自动回退为 Mock 成功。写接口、文件下载及身份授权必须使用真实后端结果。

## 身份与权限

- 新生登录发送 `loginType: "0"`。
- 塔员和管理登录发送 `loginType: "1"`。
- 门户身份以 `/getInfo.user.isQuantaMember` 为准；本地 audience 仅用于记住登录入口。
- 管理动态路由统一位于 `/admin/*`，由服务端角色、权限和 `/getRouters` 决定。
- 后端对通讯录、论坛、资料、图书和塔服接口再次校验塔员身份，前端路由不代替服务端授权。

## 真实接口范围

- 认证：`/login`、`/logout`、`/captchaImage`、`/getInfo`、`/getRouters`。
- 新生：`/qt/interview/*`、`/qt/activity/list`、`/qt/signup/*`。
- 塔员内容：`/qt/member/list`、`/system/notice/*`、`/qt/materials/*`、`/qt/forum/*`。
- 塔员服务：`/qt/book/*`、`/qt/borrow/*`、`/qt/item/*`、`/qt/order/*`。
- 管理端：`/dashboard/*`、`/qt/*`、`/system/*` 及文件上传下载接口。

塔服缺少可信价格时只保存 `DRAFT`；电子名片只保存在当前浏览器。论坛和资料分类等新页面需要后端提供对应接口，前端 Mock 不代表后端已经上线。

## 质量检查

```bash
npm run test:run
npm run lint
npm run build
```

## 目录

```text
src/
  api/portal/       新生与塔员真实接口适配
  layout/           管理布局和门户布局
  router/           三身份路由与管理动态路由
  views/freshman/   新生 Web 页面
  views/member/     塔员 Web 页面
  views/login/      管理端登录
  views/portal-login/
  views/portal-security/
```
