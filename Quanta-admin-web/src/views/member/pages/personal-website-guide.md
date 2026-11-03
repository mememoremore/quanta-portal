# Quanta 个人网站使用指南

你可以用 Quanta 门户创建个人网站，介绍自己、展示文章或项目。不会写代码也可以从模板开始；想自己设计页面，再看后面的进阶部分。

## 1. 先把网站建起来

### 选模板创建：不用准备 GitHub 仓库

1. 在塔员中心打开「个人网站」。
2. 选一套喜欢的模板：经典浅色、纸感文字、杂志作品集或终端深色。
3. 填写「站点标识」，也可以留空让系统分配。标识就是网址中属于你的那一段，例如 `YOUR_SITE_SLUG`。
4. 点击创建，查看部署记录。等部署成功，再点击页面上方的网站链接。

网站地址类似 `https://YOUR_SITE_SLUG.page.quantacenter.com`。创建成功和部署成功是两个步骤；暂时打不开时，先看部署记录，不必重复创建。

### 完善网站上的个人资料

前往「个人中心」，填写社区昵称、头像、简介、技能和公开链接。模板可以读取这些公开资料；联系邮箱只有在你勾选公开后才会提供。修改后刷新网站查看效果。

> 网站可以被互联网上的其他人访问。开放接口无需登录，能读取站主本人「塔员可见」的论坛文章，以及这些文章的留言。「经理可见」文章不会通过这些接口公开。发布内容前，请确认它适合公开展示。

## 2. 让网站展示文章和留言

文章写在门户的「社区论坛」，网站通过接口读取，不用把正文再复制到网站代码里。

- **文章来源**：只读取站主本人发布的「塔员可见」文章，不包含别人的文章或「经理可见」文章。
- **留言来源**：来自这些文章已有的论坛留言。开放接口只能读取，不能在个人网站里直接发布留言。
- **更新方式**：新发、修改或删除文章后，刷新网站重新读取。自定义页面需要实现读取逻辑，不是任意网页都会自动展示文章。
- **公开资料**：社区昵称、公开头像、简介、技能、公开链接，以及本人选择公开的联系邮箱。
- **留言作者**：社区昵称和公开头像，不返回真实姓名、部门或用户 ID。昵称、头像可能为空，要准备默认显示。

论坛上传的文章配图需要登录，公网页面通常无法显示。建议把配图放进自己的网站仓库，使用完整的公开图片地址；也可以使用自己的公开图床。

## 3. 想改版式？使用自己的仓库

只想先拥有网站，使用模板即可。这部分适合准备修改 HTML、CSS 或 JavaScript 的同学。

1. 在个人网站页面的模板提示中打开模板仓库，复制到自己的 **GitHub 公开仓库**。
2. 在自己的仓库里修改页面；也可以自行准备静态网站。
3. 回到「个人网站」，把仓库地址改为自己的仓库，填写实际存放发布文件的分支和目录。
4. 保存后查看部署记录。之后可以手动触发部署，或等待平台检查分支更新，以实际部署状态为准。

**发布目录**就是 `index.html` 等网页文件所在的目录。复制模板后的配置按页面提示填写；自建项目按实际文件位置填写，不要照抄不存在的目录。

平台只发布已经准备好的静态文件，**不会替你执行 `npm install`、`npm run build` 等构建命令**。Vue、React 项目需要先在自己的电脑或 CI 中构建，再把生成的静态文件放到配置的分支和目录。

部署失败时先查看该次日志。需要恢复旧版本时，可以在部署记录中回滚仍可用的成功版本。

## 4. 进阶：让网页读取数据

**API（接口）**可以理解为“让网页获取文章和资料的地址”。浏览器向地址发送 GET 请求，得到 JSON 数据，再由代码决定怎么展示。这里不需要 Token、密码或密钥。

示例站点标识是 `YOUR_SITE_SLUG`。从自己的网站控制台进入本指南时，链接会带入当前标识；显示 `your-site` 时，使用前请换成自己的标识。

四类接口共用以下地址前缀：

```text
https://www.quantacenter.com/open/pages/YOUR_SITE_SLUG
```

| 想读取什么 | GET 地址 | 返回的主要内容 |
| --- | --- | --- |
| 公开资料 | `/profile` | 社区昵称、头像、个人介绍、技能、公开链接等 |
| 文章列表 | `/posts` | 标题、分类、摘要、留言数、发布时间 |
| 一篇文章 | `/posts/{postId}` | 标题、Markdown 正文、作者社区昵称等 |
| 一篇文章的留言 | `/posts/{postId}/comments` | 正文、昵称、公开头像、回复关系等 |

`{postId}` 要换成文章列表返回的 `postId`。文章分类包括 `TECH`（技术）、`PROJECT`（项目）、`LIFE`（生活）。

### 分页和返回格式

文章列表、留言列表都可以加上 `?pageNum=1&pageSize=20`。`pageNum` 是页码，`pageSize` 是每页数量，默认 20，最大 50。文章按发布时间倒序排列。

列表成功返回示例（这里只展示一条文章）：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "items": [{
      "postId": 18,
      "title": "我的第一篇文章",
      "category": "TECH",
      "excerpt": "这是文章摘要……",
      "commentCount": 0,
      "createdAt": "2026-10-26 15:02:11"
    }],
    "total": 1,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

列表数据在 `data.items`，总数在 `data.total`。单篇文章、公开资料直接在 `data` 中。正文的 `content` 是 Markdown 原文，需要网页自己渲染。留言回复关系用 `parentId` 和 `replyToNickname` 表示。

| 接口 | `data` 中的字段 |
| --- | --- |
| 公开资料 | `slug`、`displayName`（社区昵称）、`avatar`、`headline`、`bio`、`skills`、`githubUrl`、`websiteUrl`、`contactEmail` |
| 文章详情 | `postId`、`title`、`category`、`content`、`commentCount`、`createdAt`、`authorNickname` |
| 留言列表的每一项 | `commentId`、`parentId`、`content`、`authorNickname`、`authorAvatar`、`replyToNickname`、`createdAt` |

以上以当前接口代码为依据，实际可用性以部署后的服务为准。当前接口不包含点赞、收藏或浏览量，也不支持写入。

### 判断请求有没有成功

用 `response.ok` 或 `response.status` 判断成功，不要只判断 JSON 里的 `code`。

| HTTP 状态 | 意思 | 怎么处理 |
| --- | --- | --- |
| 200 | 读取成功 | 展示数据 |
| 400 | 参数格式不对 | 检查页码、文章编号等参数 |
| 404 | 站点或文章不可用，也可能是功能未开启 | 检查标识、文章归属和可见性 |
| 429 | 请求太频繁 | 等待后再试，避免连续轮询 |
| 500 | 服务暂时异常 | 稍后手动重试 |

当前限制为每个 IP 对每个接口 60 秒内最多 120 次，一般阅读不需要定时轮询。

## 5. 复制一个能展示文章的示例

下面是完整的 HTML 页面，适合先试通“文章列表 → 文章详情 → 留言”。列表先显示 20 篇，单篇留言先显示 50 条；更多内容需要按上一节的方法扩展分页。

1. 在**实际发布目录**新建 `posts.html`。下面的 `SLUG` 必须是你的站点标识。
2. 放进自己的 GitHub 仓库并推送；需要构建的项目，先把页面纳入构建流程，再发布构建产物。
3. 在门户确认部署成功，打开 `https://YOUR_SITE_SLUG.page.quantacenter.com/posts.html`。

示例用 markdown-it 渲染 Markdown，关闭原始 HTML；留言用 `textContent` 显示。示例从 CDN 加载 markdown-it，也可以下载到网站仓库，改为本地引用。

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <meta name="color-scheme" content="light" />
  <title>我的文章</title>
  <!-- Markdown 渲染器；也可以下载到本地 vendor/ 自托管 -->
  <script src="https://cdn.jsdelivr.net/npm/markdown-it@14/dist/markdown-it.min.js"></script>
  <style>
    body { max-width: 720px; margin: 0 auto; padding: 32px 20px 80px;
           font: 16px/1.75 system-ui, -apple-system, "Segoe UI", sans-serif; color: #242529; background: #fff; }
    h1 { font-size: 28px; margin-bottom: 24px; }
    .post { padding: 16px 0; border-bottom: 1px solid #ececef; }
    .post button { all: unset; cursor: pointer; font-size: 18px; font-weight: 600; color: #242529; }
    .post button:hover { color: #f97316; }
    .meta { margin-top: 6px; font-size: 13px; color: #8a8a92; }
    .tag { display: inline-block; padding: 1px 8px; margin-right: 8px; border-radius: 999px;
           background: #fff3e8; color: #f97316; font-size: 12px; }
    #status { color: #8a8a92; }
    #detail { margin-top: 40px; }
    #detail img { max-width: 100%; }
    #detail pre { overflow: auto; padding: 14px; background: #f6f6f7; border-radius: 8px; }
    #detail blockquote { margin: 16px 0; padding: 8px 16px; border-left: 3px solid #f97316; background: #fafafa; }
    #comments { margin-top: 40px; }
    .comment { padding: 12px 0; border-bottom: 1px solid #f0f0f2; }
    .comment small { color: #8a8a92; }
    .comment p { margin: 6px 0 0; white-space: pre-wrap; }
  </style>
</head>
<body>
  <h1>我的文章</h1>
  <div id="status">加载中…</div>
  <div id="list"></div>
  <div id="detail"></div>

  <script>
    // ===== 只改这一行：换成你自己的站点标识 =====
    const SLUG = 'YOUR_SITE_SLUG'
    const BASE = `https://www.quantacenter.com/open/pages/${SLUG}`
    const md = window.markdownit({ html: false, linkify: true })

    const statusEl = document.getElementById('status')
    const listEl = document.getElementById('list')
    const detailEl = document.getElementById('detail')

    async function request(path) {
      const response = await fetch(BASE + path)
      const body = await response.json().catch(() => ({}))
      if (!response.ok) {
        throw new Error(body.msg || `请求失败（HTTP ${response.status}）`)
      }
      return body.data
    }

    function categoryLabel(category) {
      return { TECH: '技术', PROJECT: '项目', LIFE: '生活' }[category] || category || ''
    }

    async function showList() {
      const data = await request('/posts?pageNum=1&pageSize=20')
      if (!data.items.length) {
        statusEl.textContent = '还没有公开的文章'
        return
      }
      statusEl.textContent = `共 ${data.total} 篇`
      listEl.innerHTML = ''
      for (const post of data.items) {
        const item = document.createElement('div')
        item.className = 'post'
        const button = document.createElement('button')
        button.textContent = post.title
        button.addEventListener('click', () => showPost(post.postId))
        const meta = document.createElement('div')
        meta.className = 'meta'
        meta.textContent = `${post.createdAt} · ${post.commentCount} 条留言`
        const tag = document.createElement('span')
        tag.className = 'tag'
        tag.textContent = categoryLabel(post.category)
        item.append(tag, button, meta)
        listEl.appendChild(item)
      }
    }

    async function showPost(postId) {
      detailEl.textContent = '加载中…'
      try {
        const post = await request(`/posts/${postId}`)
        const comments = await request(`/posts/${postId}/comments?pageNum=1&pageSize=50`)
        renderPost(post, comments)
        detailEl.scrollIntoView({ behavior: 'smooth' })
      } catch (error) {
        detailEl.innerHTML = ''
        detailEl.textContent = error.message
      }
    }

    function renderPost(post, comments) {
      detailEl.innerHTML = `
        <article>
          <h2></h2>
          <div class="meta"></div>
          <div class="content"></div>
        </article>
        <section id="comments">
          <h3>留言（${comments.total}）</h3>
          <div id="comment-list"></div>
        </section>`
      detailEl.querySelector('h2').textContent = post.title
      detailEl.querySelector('.meta').textContent =
        `${post.createdAt} · ${categoryLabel(post.category)}`
      // Markdown 渲染；html:false 可避免正文里的原始 HTML 被执行
      detailEl.querySelector('.content').innerHTML = md.render(post.content || '')

      const list = detailEl.querySelector('#comment-list')
      if (!comments.items.length) {
        list.innerHTML = '<p class="meta">还没有留言</p>'
        return
      }
      for (const comment of comments.items) {
        const wrapper = document.createElement('div')
        wrapper.className = 'comment'
        const meta = document.createElement('small')
        meta.textContent = `${comment.authorNickname || '塔员'} · ${comment.createdAt}`
        const text = document.createElement('p')
        text.textContent = comment.content
        wrapper.append(meta, text)
        list.appendChild(wrapper)
      }
    }

    showList().catch((error) => {
      statusEl.textContent = error.message
    })
  </script>
</body>
</html>
```

### 接进现有 Vue / React 项目

在页面加载或组件挂载后，用 `fetch` 请求接口，再渲染列表。不要只在构建时抓取文章，否则文章会固定在本次构建的文件里，论坛更新后网页也不会跟着变。

需要“加载更多”时，增加 `pageNum` 并追加 `data.items`；已显示数量达到 `data.total` 就停止。留言也要分别处理分页，`commentCount` 或 `total` 不代表当前这一页已经显示全部留言。

## 6. 遇到问题怎么办

### 网站打不开，或者改完没有更新

先看部署记录和在线版本，确认最新部署是否成功；检查分支、发布目录和 `index.html` 的位置。部署成功后仍显示旧内容，可以强制刷新浏览器。不要把“保存配置成功”当作“部署完成”。

### 论坛有文章，网站却没显示

依次检查：作者是不是站主本人、文章是不是「塔员可见」、列表是否只显示了第一页、页面是否实现了接口读取。可以直接在浏览器中打开文章列表接口，检查返回数据。

### 文字正常，但文章配图加载失败

论坛上传图片受登录保护，匿名网页无法直接读取。使用网站仓库里的图片或其他公开的完整地址。留言作者的公开头像是另外的字段，与文章配图权限不同。

### 本地双击 HTML，或者 localhost 请求失败

开放接口的跨域规则只允许 HTTPS 的 Pages 子域。`file://`、`http://localhost` 和其他自定义域名不在允许范围内，浏览器可能拦截请求。这不等于接口不可用。请部署到 `https://YOUR_SITE_SLUG.page.quantacenter.com` 后验证；要单独检查接口，可以直接在浏览器地址栏打开接口地址。

### 显示 404 或 429

404 时核对标识、文章归属、可见性和功能是否开启，不要直接等同于“没有文章”。429 时先停止连续请求，稍后再试。

### 想在自己的页面里显示 Markdown

使用 Markdown 渲染器并关闭原始 HTML，例如 `markdownit({ html: false })`。不要把未处理的文章正文直接拼进 `innerHTML`。留言是纯文本，使用 `textContent`。

## 7. 发布前检查一下

- 示例标识已经换成自己的，不再是 `your-site`。
- 最新部署成功，网站通过 HTTPS 的 Pages 地址访问。
- 准备展示的文章适合在公网上公开，且属于站主本人。
- 联系邮箱只在你愿意公开时勾选。
- 配图使用公开地址，手机上也能正常阅读。
- 接口失败时有提示，分页不会漏掉后续内容。
- 网站源码没有 Token、密码或其他密钥。

更新文章去论坛；更新公开资料去个人中心；改网站布局去自己的仓库。三者分开维护，会更容易管理。
