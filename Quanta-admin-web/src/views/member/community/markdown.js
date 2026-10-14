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

export function markdownImagePreviews(source) {
  return [...String(source || '').matchAll(/!\[([^\]]*)\]\(([^)]+)\)/g)]
    .map(([, alt, url]) => ({ alt: alt.trim() || '帖子配图', src: safeUrl(url, true) }))
    .filter((image) => image.src)
}

function inline(text) {
  const sentinel = String.fromCharCode(0)
  const tokens = []
  const mark = (html) => {
    const token = `${sentinel}${tokens.length}${sentinel}`
    tokens.push(html)
    return token
  }
  const withTokens = String(text || '')
    .replace(/`([^`\n]+)`/g, (_, code) => mark(`<code>${escapeHtml(code)}</code>`))
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_, alt, url) => {
      const src = safeUrl(url, true)
      return src ? mark(`<img src="${escapeHtml(src)}" alt="${escapeHtml(alt)}" loading="lazy">`) : escapeHtml(alt)
    })
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, url) => {
      const href = safeUrl(url)
      return href ? mark(`<a href="${escapeHtml(href)}" rel="noopener noreferrer" target="_blank">${escapeHtml(label)}</a>`) : escapeHtml(label)
    })
  return escapeHtml(withTokens)
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(new RegExp(`${sentinel}(\\d+)${sentinel}`, 'g'), (_, index) => tokens[Number(index)])
}

export function markdownToHtml(source) {
  const lines = String(source || '').replace(/\r\n?/g, '\n').split('\n')
  const blocks = []
  let paragraph = []
  let list = []
  let listType = ''
  let code = null
  let language = ''

  const flushParagraph = () => {
    if (paragraph.length) blocks.push(`<p>${inline(paragraph.join(' '))}</p>`)
    paragraph = []
  }
  const flushList = () => {
    if (list.length) blocks.push(`<${listType}>${list.map((item) => `<li>${inline(item)}</li>`).join('')}</${listType}>`)
    list = []
    listType = ''
  }
  const flushCode = () => {
    if (code) blocks.push(`<pre><code${language ? ` class="language-${language}"` : ''}>${escapeHtml(code.join('\n'))}</code></pre>`)
    code = null
    language = ''
  }

  for (const line of lines) {
    const fence = line.match(/^\s*```([\w+-]*)\s*$/)
    if (fence) {
      flushParagraph(); flushList()
      if (code) flushCode()
      else { code = []; language = escapeHtml(fence[1]) }
      continue
    }
    if (code) { code.push(line); continue }
    if (!line.trim()) { flushParagraph(); flushList(); continue }
    const heading = line.match(/^(#{1,3})\s+(.+)$/)
    if (heading) { flushParagraph(); flushList(); blocks.push(`<h${heading[1].length}>${inline(heading[2])}</h${heading[1].length}>`); continue }
    const bullet = line.match(/^\s*[-*]\s+(.+)$/)
    const numbered = line.match(/^\s*\d+\.\s+(.+)$/)
    if (bullet || numbered) {
      flushParagraph()
      const type = bullet ? 'ul' : 'ol'
      if (listType && listType !== type) flushList()
      listType = type
      list.push((bullet || numbered)[1])
      continue
    }
    const quote = line.match(/^>\s*(.*)$/)
    if (quote) { flushParagraph(); flushList(); blocks.push(`<blockquote>${inline(quote[1])}</blockquote>`); continue }
    flushList()
    paragraph.push(line.trim())
  }
  flushParagraph(); flushList(); flushCode()
  return blocks.join('\n')
}
