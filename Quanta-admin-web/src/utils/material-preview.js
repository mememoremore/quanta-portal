import MarkdownIt from 'markdown-it'

const textExtensions = new Set('txt text log csv tsv json jsonc yaml yml xml html htm css scss less js mjs cjs jsx ts tsx vue svelte java py sh bash sql c cpp h hpp cs go rs rb php ini toml properties conf config env diff patch'.split(' '))
const textNames = new Set(['dockerfile', 'makefile', 'readme', 'license', '.gitignore', '.env'])

export function materialPreviewKind(fileName) {
  const name = String(fileName || '').toLowerCase()
  const extension = name.split('.').pop()
  if (['md', 'markdown'].includes(extension)) return 'markdown'
  if (extension === 'pdf') return 'pdf'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(extension)) return 'image'
  if (textExtensions.has(extension) || textNames.has(name)) return 'text'
  return 'unsupported'
}

export function checkPreviewSize(size, kind) {
  const text = ['text', 'markdown'].includes(kind)
  const limit = (text ? 2 : 20) * 1024 * 1024
  if (Number(size) > limit) throw new Error(`文件超过 ${text ? 2 : 20} MB 的预览上限，请下载后查看。`)
}

export function readPreviewBytes(blob) {
  if (blob.arrayBuffer) return blob.arrayBuffer()
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(new Error('文件读取失败，请重试或下载查看。'))
    reader.readAsArrayBuffer(blob)
  })
}

export async function readPreviewText(blob) {
  checkPreviewSize(blob.size, 'text')
  const bytes = new Uint8Array(await readPreviewBytes(blob))
  let text
  if (bytes[0] === 255 && bytes[1] === 254) text = new TextDecoder('utf-16le', { fatal: true }).decode(bytes)
  else if (bytes[0] === 254 && bytes[1] === 255) text = new TextDecoder('utf-16be', { fatal: true }).decode(bytes)
  else {
    try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes) }
    catch {
      try { text = new TextDecoder('gb18030', { fatal: true }).decode(bytes) }
      catch { throw new Error('无法识别文本编码，请下载后查看。') }
    }
  }
  // Never mistake a binary upload with a text extension for a readable document.
  // eslint-disable-next-line no-control-regex
  const controls = text.match(/[\x00-\x08\x0b\x0c\x0e-\x1f]/g) || []
  if (text.includes('\0') || controls.length > Math.max(1, text.length * 0.01)) {
    throw new Error('这个文件不是可预览的文本，请下载后查看。')
  }
  return text
}

const markdown = new MarkdownIt({ html: false, linkify: false })
markdown.validateLink = value => {
  try { return ['https:', 'http:', 'mailto:'].includes(new URL(value).protocol) }
  catch { return false }
}
markdown.renderer.rules.link_open = (tokens, index, options, env, renderer) => {
  tokens[index].attrSet('target', '_blank')
  tokens[index].attrSet('rel', 'noopener noreferrer')
  return renderer.renderToken(tokens, index, options)
}
// Private materials must not silently contact remote image/tracking endpoints.
markdown.renderer.rules.image = (tokens, index) => `${markdown.utils.escapeHtml(tokens[index].content || '图片')}（图片未自动加载）`

export function renderMaterialMarkdown(text) {
  return markdown.render(text)
}

export function previewImageType(bytes) {
  if ([137, 80, 78, 71, 13, 10, 26, 10].every((byte, index) => bytes[index] === byte)) return 'image/png'
  if (bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255) return 'image/jpeg'
  const header = String.fromCharCode(...bytes.slice(0, 12))
  if (header.startsWith('GIF87a') || header.startsWith('GIF89a')) return 'image/gif'
  if (header.startsWith('RIFF') && header.slice(8, 12) === 'WEBP') return 'image/webp'
  return ''
}
