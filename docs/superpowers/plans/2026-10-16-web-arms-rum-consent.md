# Web ARMS RUM 用户授权接入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Quanta Web/H5 生产站点上，经用户同意后加载阿里云 RUM；拒绝不影响网站使用。

**Architecture:** 将生产站点识别、同意状态、SDK 延迟加载和遥测脱敏封装在独立模块。全局 Vue 组件展示不阻塞的首次提示及持续可见的设置入口。现有 Vue Router、Axios Trace ID 和后端保持不变。

**Tech Stack:** Vue 3、Vite、Vitest、阿里云 ARMS RUM CDN SDK。

**Spec:** `docs/superpowers/specs/2026-10-16-web-arms-rum-consent-design.md`

## Global Constraints

- 只改 `Quanta-admin-web`，不改 UniApp、后端、登录或 `X-Trace-Id`。
- 仅生产构建在 `www.quantacenter.com` 加载，`service_id=d08n852dkz@a575525305e4ddb5ce019`。
- `env: 'prod'`、`spaMode: 'history'`、更新版 collector 开关（含 `click.enable`、`click.trackUserInteractions`）、`tracing: false`、`replay: false`。
- 拒绝监控仍可正常使用；仅同意后下载 SDK；可随时撤回。
- 不向 RUM 主动添加学号、姓名、邮箱或其他业务身份；不上报请求/响应体。
- 不推送 GitHub；保存用户现有未提交改动。

---

## 文件结构

- `Quanta-admin-web/src/observability/rum.js`：站点门控、同意持久化、SDK 加载与 URL 脱敏。
- `Quanta-admin-web/src/observability/RumConsent.vue`：全局非阻塞提示和监控设置入口。
- `Quanta-admin-web/src/observability/rum.css`：响应式样式。
- `Quanta-admin-web/src/observability/__tests__/rum.test.js`：运行时单元测试。
- `Quanta-admin-web/src/observability/__tests__/RumConsent.test.js`：用户选择测试。
- `Quanta-admin-web/src/App.vue`、`src/main.js`：挂载提示和启动已有同意。
- `Quanta-admin-web/.env.production`、`.env.example`：公开的 RUM endpoint 配置。

### Task 1: SDK 门控与安全配置

**Files:** Create `src/observability/rum.js`、`src/observability/__tests__/rum.test.js`; modify `.env.production`、`.env.example`。

**Interfaces:** `isRumAvailable({ production, hostname, endpoint }) -> boolean`，`readRumConsent(storage) -> 'accepted' | 'declined' | null`，`saveRumConsent(choice, storage) -> void`，`loadRumSdk({ windowRef, documentRef, endpoint }) -> boolean`，`startRumIfConsented() -> boolean`。

- [x] Write tests first: production/domain/endpoint gate; consent read-write; no script before consent; exact SDK config; one script only; load error does not throw; URL query/hash redaction.
- [x] Run `npm run test:run -- src/observability/__tests__/rum.test.js` and confirm expected failure.
- [x] Implement module using the provided endpoint and CDN URL. Set `window.__rum` before inserting async script into body. Use `parseViewName`, `parseResourceName`, `evaluateApi` and `beforeReport` to avoid exposing URL query strings or bodies. Keep `tracing: false`.
- [x] Run targeted test; expect pass.

### Task 2: 同意提示与撤回

**Files:** Create `src/observability/RumConsent.vue`、`rum.css`、`__tests__/RumConsent.test.js`; modify `src/App.vue`、`src/main.js`。

**Interfaces:** Task 1's `readRumConsent`, `saveRumConsent`, `loadRumSdk`, `startRumIfConsented`, `isRumAvailable`。

- [x] Write component tests first: undecided shows notice; accept loads SDK and hides notice; decline hides notice without loading; settings reopens; withdrawal saves declined and reloads; all buttons are keyboard-accessible.
- [x] Run `npm run test:run -- src/observability/__tests__/RumConsent.test.js` and confirm expected failure.
- [x] Implement a bottom-left unobtrusive control with accessible buttons and concise SDK/privacy explanation; mount once in `App.vue`. Start stored consent from `main.js` before mounting Vue.
- [x] Run targeted test; expect pass.

### Task 3: 完整验证

**Files:** Only fixes in the files above if tests reveal issues.

- [x] Run `npm run lint`, `npm run test:run`, `npm run build` from `Quanta-admin-web`.
- [x] Inspect production bundle for correct service ID and SDK gating. Unit tests verify local/declined mode cannot insert the CDN script; live-site reporting requires a deployed smoke test.
- [x] Inspect `git diff --check` and `git status --short`; leave unrelated work intact and do not push.
