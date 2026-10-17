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
markdown.renderer.rules.fence = (tokens, index, options, env, renderer) => withCopyButton(renderFence(tokens, index, options, env, renderer), tokens[index].info)
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
  return markdown.render(String(source || ''))
}

export function markdownToDocument(source, prefix = 'article') {
  const safePrefix = String(prefix).replace(/[^a-zA-Z0-9_-]/g, '') || 'article'
  const env = {}, headings = []
  const tokens = markdown.parse(String(source || ''), env)
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
