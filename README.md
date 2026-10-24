# Quanta Portal

Quanta 社团的统一门户项目。仓库包含 Web 门户、UniApp 移动端和若依体系的 Java 后端源码。本仓库从当前代码重新开始，不包含旧仓库的提交历史、部署文件或演示账号密码。

## 项目结构

| 目录 | 技术与用途 |
| --- | --- |
| [`Quanta-admin-web/`](Quanta-admin-web/) | Vue 3 + Vite 的统一 Web 门户，包含新生、塔员和管理三个入口。 |
| [`Quanta-uniapp/`](Quanta-uniapp/) | Vue 3 + UniApp 的移动端项目，支持 H5 和微信小程序开发。 |
| `ruoyi-admin/` 等 7 个 `ruoyi-*` 模块 | Java 后端，提供认证、权限、成员、招新、活动等业务接口。 |
| [`pom.xml`](pom.xml) | 后端 Maven 多模块构建入口。 |

Web 端包括招新报名与管理、塔员服务、学习资料和社区论坛等页面。两个前端通过 HTTP 接口与后端交互；页面中的权限控制不能替代后端鉴权。

塔员还可以在「个人网站」控制台绑定 GitHub 公开仓库，把已构建的静态产物自动发布到 `{slug}.page.quantacenter.com`（后端 `ruoyi-qt` 的 Pages 业务 + 宿主机 Nginx 静态直出，默认关闭，启用条件见 [`.code_deploy/pages/`](.code_deploy/pages/) 与其上线核对清单）。

## 本地运行

前端开发先安装 Node.js 和 npm。使用真实接口时，还需要可访问的后端服务。

### Web 门户

```bash
cd Quanta-admin-web
npm ci
npm run dev
```

开发环境在 `Quanta-admin-web/.env.development` 中设置 `VITE_API_BASE_URL`。更多入口、接口和权限说明见 [Web 端 README](Quanta-admin-web/README.md)。

### UniApp 移动端

```bash
cd Quanta-uniapp
npm ci
npm run dev:h5
```

开发微信小程序时使用 `npm run dev:mp-weixin`；生产构建分别使用 `npm run build:h5` 或 `npm run build:mp-weixin`。接口地址通过该项目的 Vite 环境配置提供。

## 检查与构建

```bash
# 在 Quanta-admin-web 目录
npm run test:run
npm run lint
npm run build

# 在 Quanta-uniapp 目录
npm test
npm run build:h5
```

## 后端协作

后端采用若依体系、Java 17 和 Maven 多模块结构，由后端负责身份认证、角色权限及业务数据。本次只收录源码和构建文件；数据库脚本在 `sql/`（含 Pages 的 `2026-10-25-qt-pages.sql`），云部署配置在 `.code_deploy/`。后端同学确认数据库初始化方式、环境变量和启动流程后，再补充可复现的运行说明。

不要提交真实密码、密钥或本机私有配置。`VITE_` 开头的变量会进入前端构建产物，不能用于保存秘密。
