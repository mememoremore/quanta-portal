# Frontend CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add read-only GitHub Actions CI for both frontend folders without deploying.

**Architecture:** One workflow has independent `web` and `uniapp` jobs. Both install from their own lockfiles on Node 24; Web also runs browser tests against its intercepted API fixture.

**Tech Stack:** GitHub Actions, npm, Vite, Vitest, Playwright, UniApp.

**Spec:** `docs/superpowers/specs/2026-10-15-frontend-ci-design.md`

## Global Constraints

- Change only `.github/workflows/frontend-ci.yml` and the Web E2E API fixture; do not change business code or backend CI.
- Trigger on `main` pushes, PRs to `main`, and manual dispatch, with frontend-path filtering for automatic events.
- Use Node 24, `npm ci`, and separate npm caches keyed by each lockfile.
- No production API calls, deployment, secrets, or ignored failures.

---

### Task 1: Add and verify the frontend workflow

**Files:**
- Create: `.github/workflows/frontend-ci.yml`
- Modify: `Quanta-admin-web/e2e/support/fixture.js`

**Interfaces:**
- Consumes: scripts in `Quanta-admin-web/package.json`, `Quanta-uniapp/package.json`, and Web `playwright.config.js`.
- Produces: GitHub Actions jobs named `web` and `uniapp`, with no deployment side effects.

- [x] **Step 1: Confirm the workflow does not exist**

```powershell
Test-Path .github/workflows/frontend-ci.yml
```

Expected: `False`.

- [x] **Step 2: Create the workflow**

```yaml
name: Frontend CI

on:
  push:
    branches: [main]
    paths:
      - 'Quanta-admin-web/**'
      - 'Quanta-uniapp/**'
      - '.github/workflows/frontend-ci.yml'
  pull_request:
    branches: [main]
    paths:
      - 'Quanta-admin-web/**'
      - 'Quanta-uniapp/**'
      - '.github/workflows/frontend-ci.yml'
  workflow_dispatch:

permissions:
  contents: read

jobs:
  web:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    defaults:
      run:
        working-directory: Quanta-admin-web
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-node@v7
        with:
          node-version: '24'
          cache: npm
          cache-dependency-path: Quanta-admin-web/package-lock.json
      - run: npm ci
      - run: npm run lint
      - run: npm run test:run
      - run: npm run build
      - run: npx playwright install --with-deps chromium
      - run: npm run test:e2e
      - if: failure()
        uses: actions/upload-artifact@v7
        with:
          name: web-playwright-report
          path: |
            Quanta-admin-web/playwright-report/
            Quanta-admin-web/test-results/
          if-no-files-found: ignore

  uniapp:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    defaults:
      run:
        working-directory: Quanta-uniapp
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-node@v7
        with:
          node-version: '24'
          cache: npm
          cache-dependency-path: Quanta-uniapp/package-lock.json
      - run: npm ci
      - run: npm test
      - run: npm run build:h5
```

- [x] **Step 3: Check syntax and local commands**

The existing E2E fixture fails because dual-token requests use credentials but the simulated cross-origin API does not permit credentials, and an unauthenticated route tries `/auth/refresh`. Add the following to `responseFor()` and the `headers` object in `fixture.js`:

```js
if (method === 'POST' && path === '/auth/refresh') {
  return { code: 401, msg: '登录状态已过期，请重新登录' }
}

// Alongside the existing access-control-allow-origin header:
'access-control-allow-credentials': 'true',
```

Then validate:

```powershell
git diff --check
Push-Location Quanta-admin-web
npm run lint
npm run test:run
npm run build
npx playwright install chromium
npm run test:e2e
Pop-Location
Push-Location Quanta-uniapp
npm test
npm run build:h5
Pop-Location
```

Expected: all commands exit zero. Confirm the YAML has no deployment commands or secrets.

- [x] **Step 4: Commit locally, not push**

```powershell
git add -- .github/workflows/frontend-ci.yml Quanta-admin-web/e2e/support/fixture.js docs/superpowers/specs/2026-10-15-frontend-ci-design.md docs/superpowers/plans/2026-10-15-frontend-ci.md
git commit -m "ci: 添加前端持续集成检查"
```

Leave the unrelated local handoff document untracked. Do not push without a separate request.
