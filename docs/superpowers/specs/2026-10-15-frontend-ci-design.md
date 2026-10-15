# 前端 CI 设计

## 目标与范围

在 GitHub Actions 中新增一个前端工作流，分别检查 `Quanta-admin-web` 和 `Quanta-uniapp`。它只验证代码，不部署、不调用生产接口、不要求服务器密钥。后端 CI 由后端同学在同一仓库的另一个工作流维护。

## 工作流

文件为仓库根目录 `.github/workflows/frontend-ci.yml`。向 `main` 推送、针对 `main` 的 Pull Request，或手动触发时运行；自动触发仅限两个前端目录或工作流文件自身发生变化。权限仅为读取仓库内容。一个工作流中放两个相互独立的任务，某个任务失败不阻止另一个任务给出结果。

两个任务均使用 GitHub 托管的 Ubuntu Runner、Node.js 24、各自目录中的 `package-lock.json` 以及 `npm ci`。缓存仅针对各目录的 npm 下载缓存，不缓存 `node_modules`。

## Web 任务

进入 `Quanta-admin-web`，依次运行 ESLint、Vitest 全量测试、Vite 生产构建。再安装 Playwright 的 Chromium 及系统依赖，运行现有 E2E 测试。E2E 测试通过仓库现有 fixture 拦截 API 请求，不连接真实后端；fixture 需支持双 Token 未登录时的刷新失败响应，以及跨域凭据请求的 CORS 响应头。失败时上传 Playwright 报告及跟踪材料供排查。

## UniApp 任务

进入 `Quanta-uniapp`，运行现有 Vitest 测试及 H5 构建。首版不构建小程序或 App，也不加入该项目当前没有的 lint 脚本。

## 成功标准

两个任务均通过，工作流才显示成功。任何测试、构建或 E2E 失败都会让相应任务失败；不使用 `continue-on-error` 掩盖失败。CI 不上传或发布生产构建产物，不包含 CD 步骤。首版仅新增工作流并适配 E2E 测试夹具，不修改业务代码。
