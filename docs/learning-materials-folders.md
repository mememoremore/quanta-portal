# 学习资料目录与批量上传 — 前端对接说明

> **读者**：`Quanta-admin-web` 管理端「学习资料」页、塔员中心「学习资料」页  
> **版本**：2026-10-17  
> **状态**：后端已实现；前端按本文改造。UniApp 小程序**无需改**：继续 `GET /qt/materials` 不传 `folderId`，仍为全量平铺列表。

---

## 1. 概念

| 层级 | 说明 |
|------|------|
| 部门分类 | 固定四类：`FRONTEND` / `BACKEND` / `PRODUCT` / `DESIGN`（展示名：前端 / 后端 / 产品 / 设计） |
| 目录 | 挂在某一分类下，`parentId = 0` 表示分类根下的第一层；最深 **5 层** |
| 资料 | `folderId = 0` 表示在该分类根目录；否则落在对应文件夹内 |

上传权限与现网一致：部门负责人只能操作**本部门分类**；CEO / 超管可选任意分类。目录的增删改与上传共用 `qt:material:add`。

---

## 2. 目录接口

基路径：`/qt/materials/folders`（与资料接口同域、同 `Authorization`）。

### 2.1 列出子目录

`GET /qt/materials/folders?category=FRONTEND&parentId=0`

| 参数 | 必填 | 说明 |
|------|------|------|
| category | 是 | 四类之一 |
| parentId | 否 | 默认 `0`（分类根） |

响应 `data` 为数组，元素字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| folderId | number | 目录 ID |
| parentId | number | 父目录 ID |
| category | string | 分类 |
| folderName | string | 名称 |
| depth | number | 1–5 |

### 2.2 目录详情与面包屑

`GET /qt/materials/folders/{folderId}`

```json
{
  "code": 200,
  "data": {
    "folder": { "folderId": 12, "parentId": 0, "category": "FRONTEND", "folderName": "培训", "depth": 1 },
    "breadcrumb": [
      { "folderId": 12, "folderName": "培训", "depth": 1 }
    ]
  }
}
```

面包屑从分类根下第一层到当前目录；**最前面的部门名**由前端用 `category` 映射（与现有 `materialCategoryLabel` 一致）拼在面包屑前。

### 2.3 新建目录

`POST /qt/materials/folders`  
权限：`qt:material:add`

```json
{
  "category": "FRONTEND",
  "parentId": 0,
  "folderName": "2026 春招"
}
```

规则：同级（相同 `parentId` + `category`）名称唯一；名称 1–50 字，不能含 `/`、`\`。

### 2.4 重命名 / 移动

`PUT /qt/materials/folders/{folderId}`  
权限：`qt:material:add`

```json
{ "folderName": "新名称" }
```

或

```json
{ "parentId": 0 }
```

可同时传 `folderName` 与 `parentId`。不能移入自身或子孙目录；移动后整棵子树深度不得超过 5。

### 2.5 删除目录

`DELETE /qt/materials/folders/{folderId}`  
权限：`qt:material:add`  

仅当**无子目录且无资料**时可删。

---

## 3. 资料接口变更

### 3.1 列表 `GET /qt/materials`

在现有参数（`pageNum`、`pageSize`、`category`、`categoryScope`、`fileName` 等）上增加：

| 参数 | 说明 |
|------|------|
| folderId | **可选**。传入时按目录筛选；**不传**时行为与旧版一致（全部分类内平铺，供小程序兼容） |
| folderId=0 | 仅该 `category` 根目录下的资料（需同时传 `category`） |
| folderId>0 | 仅该目录**直接**包含的资料（不含子目录内文件） |

当带 `fileName` 搜索时，**忽略** `folderId`，在当前 `category` / `categoryScope` 范围内按文件名模糊查；每条结果额外带：

| 字段 | 说明 |
|------|------|
| folderId | 所属目录，根目录为 `0` |
| folderName | 直接父目录名；根目录为空字符串 |
| folderPath | 如 `前端 / 培训 / 2026`（含部门展示名） |

### 3.2 上传 `POST /qt/materials`

仍为 `multipart/form-data`，字段不变，**新增可选**：

| 字段 | 说明 |
|------|------|
| folderId | 目标目录；省略或 `0` 表示当前分类根 |

其余：`file`、`category`、`visibility`（前端继续传 `MEMBER`）。

### 3.3 移动资料 `PUT /qt/materials/{materialId}`

请求体可增加 `folderId`。CEO / 超管可把资料移到**其他分类**下的目录，后端会自动把 `category` 改为目标目录的分类；普通账号仅能在**同分类**内移动。

---

## 4. 批量上传（前端实现方式）

后端**没有**「一次请求多文件」接口（受全局单文件 20MB、整请求 25MB 限制）。批量上传约定：

1. 上传区支持多选（建议上限 **20** 个文件，与现网类型、20MB 单文件校验一致）。
2. 用户确认后，对**当前浏览目录**（当前 `category` + `folderId`）**串行**调用 `POST /qt/materials`，每个文件一次。
3. UI 展示每个文件状态：等待 / 上传中 / 成功 / 失败；单个失败不阻断其余文件。
4. 全部结束后刷新当前目录的资料列表。

`Quanta-admin-web` 可参考伪代码：

```js
for (const file of selectedFiles) {
  const data = new FormData()
  data.append('file', file)
  data.append('category', currentCategory)
  data.append('visibility', 'MEMBER')
  if (currentFolderId > 0) data.append('folderId', String(currentFolderId))
  await uploadMaterial(data) // 现有 request 封装，timeout 120s
}
```

---

## 5. 管理端页面建议（`learning-materials/index.vue`）

1. **布局**：左侧或顶部为「部门分类」；主体为「面包屑 + 子目录列表 + 资料表格」。
2. **进入分类根**：`parentId=0` 拉子目录；`GET /qt/materials?category=…&folderId=0` 拉根目录资料。
3. **进入子目录**：点击文件夹 → 更新 `currentFolderId` → 子目录接口 `parentId=currentFolderId`；资料列表 `folderId=currentFolderId`。
4. **操作**：新建文件夹、重命名、删除（空目录）、资料/目录移动到同分类另一目录（资料用 `PUT`，目录用 `PUT /folders/{id}`）。
5. **上传卡片**：`ElUpload` 去掉 `:limit="1"`，用队列 + 上文串行上传；`data-testid="confirm-material-upload"` 可保留。

涉及文件（供分工）：

- [Quanta-admin-web/src/views/learning-materials/index.vue](Quanta-admin-web/src/views/learning-materials/index.vue)
- [Quanta-admin-web/src/api/materials.js](Quanta-admin-web/src/api/materials.js) — 增加目录 API、`uploadMaterial` 支持 `folderId`

---

## 6. 成员端页面建议（`member/materials/index.vue`）

1. 保留顶部四个部门 Tab。
2. 选中部门后**不再**一次性平铺该部门全部文件；改为与管端类似的目录浏览（只读：无上传、无删目录）。
3. 下载逻辑不变（仍用 `downloadUrl`）。
4. 搜索：带 `fileName` 调列表，结果列展示 `folderPath`。

涉及文件：

- [Quanta-admin-web/src/views/member/materials/index.vue](Quanta-admin-web/src/views/member/materials/index.vue)
- [Quanta-admin-web/src/api/portal/content.js](Quanta-admin-web/src/api/portal/content.js)（若资料列表封装在此）

---

## 7. 数据库迁移

部署前在目标库执行：

`sql/2026-10-17-material-folders.sql`

已有资料的 `folder_id` 默认为 `0`（分类根）。

---

## 8. 错误提示（常见）

| 场景 | 后端 msg 示例 |
|------|----------------|
| 同级重名 | 同级目录下已存在同名文件夹 |
| 超过 5 层 | 目录层级不能超过 5 层 |
| 非空目录删除 | 目录内仍有资料，无法删除 / 请先清空子目录 |
| 分类与目录不一致 | 资料分类与目标目录不一致 |
| 跨部门上传 | 只能上传本部门分类（…）的学习资料 |

前端可直接 `ElMessage.error(error.message)` 展示。
