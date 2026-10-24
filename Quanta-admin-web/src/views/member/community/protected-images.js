import { request } from '@/utils/request'

// Only our canonical relative resource path may receive the Authorization header.
export const isProtectedForumImage = value => /^\/qt\/forum\/images\/[a-f0-9]{32}$/.test(String(value || ''))

export function createProtectedImages() {
  const entries = new Map()
  let alive = true
  function release(image, entry) {
    entry.abort.abort()
    if (entry.url) URL.revokeObjectURL(entry.url)
    image.removeAttribute('src')
    entries.delete(image)
  }
  function sync(root) {
    if (!alive) return
    const images = new Set(root.querySelectorAll('img[data-forum-image]'))
    for (const [image, entry] of entries) {
      if (!images.has(image) || entry.source !== image.dataset.forumImage) release(image, entry)
    }
    for (const image of images) {
      const source = image.dataset.forumImage
      if (!isProtectedForumImage(source)) continue
      const existing = entries.get(image)
      if (existing) { if (existing.url && image.getAttribute('src') !== existing.url) image.src = existing.url; continue }
      const entry = { source, abort: new AbortController(), url: '' }
      entries.set(image, entry)
      image.setAttribute('aria-busy', 'true')
      request({ url: source, method: 'get', responseType: 'blob', signal: entry.abort.signal }).then(response => {
        if (!alive || entry.abort.signal.aborted || entries.get(image) !== entry) return
        const blob = response.data
        if (!blob || !/^image\/(png|jpeg|webp|gif)$/.test(blob.type)) throw new Error('图片响应类型无效')
        entry.url = URL.createObjectURL(blob)
        image.src = entry.url
        image.removeAttribute('aria-busy')
      }).catch(error => {
        if (!alive || entry.abort.signal.aborted || entries.get(image) !== entry) return
        image.removeAttribute('aria-busy')
        image.setAttribute('title', error.message || '图片无法加载，请刷新重试')
        image.classList.add('community-image-unavailable')
      })
    }
  }
  return { sync, destroy() { alive = false; for (const [image, entry] of entries) release(image, entry) } }
}
