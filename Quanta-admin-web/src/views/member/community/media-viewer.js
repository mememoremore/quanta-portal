import { attachMediaGestures } from './media-gestures'
import { sanitizeDiagram } from './mermaid-policy'
import { safeMarkdownUrl } from './image-previews'

let sequence = 0

export function createMediaViewer() {
  let dialog, stage, content, status, percent, gestures, opener, restoreOverflow, disposeImage, generation = 0
  function finish() {
    generation += 1
    gestures?.destroy(); gestures = null
    disposeImage?.(); disposeImage = null
    content?.replaceChildren()
    restoreOverflow?.(); restoreOverflow = null
    if (opener?.isConnected) opener.focus?.({ preventScroll: true })
  }
  function close() { if (dialog?.open) dialog.close() }
  function controls(event) {
    const action = event.target.closest('button')?.dataset.mediaAction
    if (action === 'close') close()
    else if (action === 'reset') gestures?.reset()
    else if (action === 'in') gestures?.zoomBy(1.25)
    else if (action === 'out') gestures?.zoomBy(0.8)
  }
  function create() {
    dialog = document.createElement('dialog')
    dialog.className = 'community-media-viewer'
    dialog.setAttribute('aria-label', '全屏看图')
    dialog.innerHTML = '<header><span>全屏看图</span><button type="button" data-media-action="close" autofocus>关闭</button></header><div class="community-media-viewer__stage"><div class="community-media-viewer__content"></div><p role="status"></p></div><footer><button type="button" data-media-action="out" aria-label="缩小图片">−</button><span data-media-percent>100%</span><button type="button" data-media-action="in" aria-label="放大图片">＋</button><button type="button" data-media-action="reset">适应屏幕</button></footer>'
    stage = dialog.querySelector('.community-media-viewer__stage')
    content = dialog.querySelector('.community-media-viewer__content')
    status = dialog.querySelector('[role=status]')
    percent = dialog.querySelector('[data-media-percent]')
    dialog.addEventListener('click', controls)
    dialog.addEventListener('close', finish)
    document.body.append(dialog)
  }
  function open(element) {
    if (!element?.isConnected) return
    if (!dialog) create()
    if (dialog.open) return
    const current = ++generation
    opener = document.activeElement
    if (!opener || opener === document.body) opener = element
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    restoreOverflow = () => { document.body.style.overflow = previousOverflow }
    status.hidden = false; status.textContent = '正在加载图片…'
    content.style.transform = ''
    dialog.showModal()
    function ready(width, height) {
      if (current !== generation || !dialog.open) return
      if (![width, height].every(value => Number.isFinite(value) && value > 0)) { status.textContent = '图片尺寸无效，请关闭后重试。'; return }
      content.style.width = `${width}px`; content.style.height = `${height}px`
      status.hidden = true
      gestures = attachMediaGestures(stage, content, { width, height, onTap: close, onChange: state => { percent.textContent = `${Math.round(state.scale * 100)}%` } })
    }
    try {
      if (element.tagName.toLowerCase() === 'svg') {
        const fragment = sanitizeDiagram(element.outerHTML, `quanta-media-${++sequence}`)
        const svg = fragment.querySelector('svg')
        const values = svg.getAttribute('viewBox')?.trim().split(/[\s,]+/).map(Number)
        svg.style.width = '100%'; svg.style.height = '100%'; svg.style.maxWidth = 'none'
        content.replaceChildren(fragment)
        ready(values?.[2], values?.[3])
      } else {
        const src = safeMarkdownUrl(element.getAttribute('src'), true)
        if (!src) throw new Error('图片地址无效')
        const image = document.createElement('img')
        image.alt = element.alt || '帖子图片'; image.draggable = false
        image.onload = () => ready(image.naturalWidth, image.naturalHeight)
        image.onerror = () => { if (current === generation) status.textContent = '图片加载失败，请关闭后重试。' }
        disposeImage = () => { image.onload = image.onerror = null }
        content.replaceChildren(image)
        image.src = src
      }
    } catch (error) { status.textContent = error.message || '暂时无法查看图片，请关闭后重试。' }
  }
  function destroy() {
    if (!dialog) return
    if (dialog.open) dialog.close()
    finish()
    dialog.removeEventListener('close', finish)
    dialog.removeEventListener('click', controls)
    dialog.remove(); dialog = null
  }
  return { open, destroy }
}
