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

for (const [name, language] of Object.entries({ bash, css, java, javascript, json, python, sql, typescript, xml })) {
  hljs.registerLanguage(name, language)
}
for (const [alias, name] of Object.entries({ sh: 'bash', js: 'javascript', jsx: 'javascript', py: 'python', ts: 'typescript', tsx: 'typescript', html: 'xml', vue: 'xml' })) {
  hljs.registerAliases(alias, { languageName: name })
}

export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[char])
}

function safeUrl(value, image = false) {
  const url = String(value || '').trim()
  if (url.startsWith('/') && !url.startsWith('//')) return url
  if (/^https:\/\/[^\s"'<>]+$/i.test(url)) return url
  if (image && /^data:image\/(?:png|jpeg|webp|gif);base64,[a-z0-9+/=]+$/i.test(url)) return url
  if (!image && /^http:\/\/[^\s"'<>]+$/i.test(url)) return url
  return ''
}

const markdown = new MarkdownIt({
  html: false,
  linkify: false,
  highlight(code, language) {
    const name = language.trim().split(/\s+/)[0]
    const highlighted = name && hljs.getLanguage(name)
      ? hljs.highlight(code, { language: name, ignoreIllegals: true }).value
      : escapeHtml(code)
    const className = name && hljs.getLanguage(name) ? ` class="language-${escapeHtml(name)}"` : ''
    return `<pre class="hljs"><code${className}>${highlighted}</code></pre>`
  },
})

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

export function markdownImagePreviews(source) {
  const images = []
  const visit = (tokens) => {
    for (const token of tokens || []) {
      if (token.type === 'image') {
        const src = safeUrl(token.attrGet('src'), true)
        if (src) images.push({ alt: token.content.trim() || '帖子配图', src })
      }
      if (token.children) visit(token.children)
    }
  }
  visit(markdown.parse(String(source || ''), {}))
  return images
}
