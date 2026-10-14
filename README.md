# Quanta Portal

Quanta 社团的统一门户项目。仓库当前包含 Web 门户和 UniApp 移动端；后端代码将由后端同学后续合入。本 README 根据旧项目说明整理，但只描述当前仓库确实具备的内容，不包含旧仓库的提交历史、部署文件或演示账号密码。

## 项目结构

| 目录 | 技术与用途 |
| --- | --- |
| [`Quanta-admin-web/`](Quanta-admin-web/) | Vue 3 + Vite 的统一 Web 门户，包含新生、塔员和管理三个入口。 |
| [`Quanta-uniapp/`](Quanta-uniapp/) | Vue 3 + UniApp 的移动端项目，支持 H5 和微信小程序开发。 |
| 后端（待合入） | 提供认证、权限、成员、招新、活动和其他业务接口；代码与运行说明由后端同学提交后补充。 |

Web 端包括招新报名与管理、塔员服务、学习资料和社区论坛等页面。两个前端通过 HTTP 接口与后端交互；页面中的权限控制不能替代后端鉴权。

## 本地运行

先安装 Node.js 和 npm。使用真实接口时，还需要可访问的后端服务；后端尚未合入本仓库，因此这里只列前端命令。

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

旧项目采用若依体系，由后端负责身份认证、角色权限及业务数据。后端同学合入代码时，请在本节补充实际目录、依赖版本、数据库初始化方式、启动命令和接口配置；在那之前，不应把旧仓库的后端启动步骤当成本仓库的操作手册。

不要提交真实密码、密钥或本机私有配置。`VITE_` 开头的变量会进入前端构建产物，不能用于保存秘密。
