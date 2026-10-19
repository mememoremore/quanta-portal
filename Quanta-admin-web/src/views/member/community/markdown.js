import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import { safeMarkdownUrl as safeUrl } from './image-previews'

export { markdownImagePreviews } from './image-previews'

for (const [name, language] of Object.entries({ bash, css, java, javascript, json, python, sql, typescript, xml, yaml })) {
  hljs.registerLanguage(name, language)
}
for (const [alias, name] of Object.entries({ sh: 'bash', js: 'javascript', jsx: 'javascript', py: 'python', ts: 'typescript', tsx: 'typescript', html: 'xml', vue: 'xml', yml: 'yaml' })) {
  hljs.registerAliases(alias, { languageName: name })
}

export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[char])
}

/** 还原若依 XSS 过滤器写入库的正文实体，便于 Markdown 解析与编辑 */
export function restoreForumMarkdown(source) {
  return String(source ?? '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
}

const markdown = new MarkdownIt({
  html: false,
  linkify: false,
  highlight(code, language) {
    const name = language.trim().split(/\s+/)[0]
    // Limit language detection work on long unlabelled blocks, then highlight once.
    const detected = !name ? hljs.highlightAuto(code.slice(0, 2000), ['yaml', 'javascript', 'json', 'bash', 'python', 'sql', 'css', 'xml']).language : ''
    const resolved = name && hljs.getLanguage(name) ? name : !name ? detected : ''
    const highlighted = resolved ? hljs.highlight(code, { language: resolved, ignoreIllegals: true }).value : escapeHtml(code)
    const className = name && hljs.getLanguage(name) ? ` class="language-${escapeHtml(name)}"` : ''
    return `<pre class="hljs"><code${className}>${highlighted}</code></pre>`
  },
})

function withCopyButton(html, language = '') {
  const label = language.trim().split(/\s+/)[0] || 'code'
  return `<div class="community-code-block"><div class="community-code-block__toolbar"><span>${escapeHtml(label)}</span><button type="button" class="community-code-copy" data-copy-code aria-live="polite">复制代码</button></div>${html}</div>\n`
}

const renderFence = markdown.renderer.rules.fence
markdown.renderer.rules.fence = (tokens, index, options, env, renderer) => {
  const token = tokens[index]
  if (token.info.trim().split(/\s+/)[0].toLowerCase() === 'mermaid') {
    return `<div class="community-code-block community-mermaid" data-mermaid-block>
      <div class="community-mermaid__actions"><button type="button" data-mermaid-expand hidden>放大查看</button><details class="community-mermaid__menu"><summary aria-label="图表选项">⋯</summary><div><button type="button" data-mermaid-source aria-expanded="true">收起源码</button><button type="button" class="community-code-copy" data-copy-code aria-live="polite">复制代码</button></div></details></div>
      <p class="community-mermaid__status" data-mermaid-status role="status">正在准备图表…</p>
      <div class="community-mermaid__viewport" data-mermaid-viewport hidden><div class="community-mermaid__canvas" data-mermaid-canvas></div></div>
      <dialog class="community-mermaid__dialog" data-mermaid-dialog aria-label="图表放大查看"><div class="community-mermaid__dialog-header"><span>图表</span><button type="button" data-mermaid-close autofocus>关闭</button></div><div class="community-mermaid__zoom" data-mermaid-zoom><button type="button" data-mermaid-scale="down" aria-label="缩小图表">−</button><span data-mermaid-percent>100%</span><button type="button" data-mermaid-scale="up" aria-label="放大图表">＋</button><button type="button" data-mermaid-scale="reset">适应宽度</button></div></dialog>
      <pre class="hljs" data-mermaid-code><code class="language-mermaid">${escapeHtml(token.content)}</code></pre>
    </div>\n`
  }
  return withCopyButton(renderFence(tokens, index, options, env, renderer), token.info)
}
const renderCodeBlock = markdown.renderer.rules.code_block
markdown.renderer.rules.code_block = (tokens, index, options, env, renderer) => withCopyButton(renderCodeBlock(tokens, index, options, env, renderer))

markdown.validateLink = (url) => Boolean(safeUrl(url, true))
const renderImage = markdown.renderer.rules.image
markdown.renderer.rules.link_open = (tokens, index, options, env, renderer) => {
  const token = tokens[index]
  if (!safeUrl(token.attrGet('href'))) return ''
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return renderer.renderToken(tokens, index, options)
}
markdown.renderer.rules.image = (tokens, index, options, env, renderer) => {
  const token = tokens[index]
  if (!safeUrl(token.attrGet('src'), true)) return escapeHtml(token.content)
  token.attrSet('loading', 'lazy')
  return renderImage(tokens, index, options, env, renderer)
}

export function markdownToHtml(source) {
  return markdown.render(restoreForumMarkdown(source))
}

export function markdownToDocument(source, prefix = 'article') {
  const safePrefix = String(prefix).replace(/[^a-zA-Z0-9_-]/g, '') || 'article'
  const env = {}, headings = []
  const tokens = markdown.parse(restoreForumMarkdown(source), env)
  for (let index = 0; index < tokens.length; index += 1) {
    const token = tokens[index]
    if (token.type !== 'heading_open') continue
    const inline = tokens[index + 1]
    const text = (inline?.children || []).filter(child => ['text', 'code_inline', 'image', 'softbreak', 'hardbreak'].includes(child.type))
      .map(child => ['softbreak', 'hardbreak'].includes(child.type) ? ' ' : child.content).join('').trim()
    const id = `${safePrefix}-${headings.length + 1}`
    token.attrSet('id', id)
    headings.push({ id, text: text || '未命名标题', level: Number(token.tag.slice(1)) })
  }
  return { html: markdown.renderer.render(tokens, markdown.options, env), headings }
}
