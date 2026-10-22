import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { createMediaViewer } from '../media-viewer'

let viewer, source
beforeEach(() => {
  vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} })
  vi.spyOn(Element.prototype, 'getBoundingClientRect').mockReturnValue({ width: 800, height: 600 })
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value() { this.open = true } })
  Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value() { this.open = false; this.dispatchEvent(new Event('close')) } })
  viewer = createMediaViewer()
})
afterEach(() => {
  viewer.destroy(); source?.remove(); document.body.style.overflow = ''
  delete HTMLDialogElement.prototype.showModal; delete HTMLDialogElement.prototype.close
  vi.unstubAllGlobals(); vi.restoreAllMocks()
})

it('sanitizes cloned SVG, isolates IDs and preserves the original node on close', () => {
  source = document.createElement('div')
  source.innerHTML = '<svg viewBox="0 0 400 200"><title id="title">流程图</title><script>alert(1)</script><text onclick="alert(1)">开始</text></svg>'
  document.body.append(source)
  const original = source.querySelector('svg')
  viewer.open(original)
  const dialog = document.querySelector('dialog')
  expect(dialog.open).toBe(true)
  expect(dialog.querySelector('script')).toBeNull()
  expect(dialog.querySelector('[onclick]')).toBeNull()
  expect(dialog.querySelector('title').id).not.toBe('title')
  dialog.querySelector('[data-media-action=close]').click()
  expect(original.isConnected).toBe(true)
  expect(source.querySelector('svg')).toBe(original)
})

it('shows image failure safely and restores scroll on destroy without late load updates', () => {
  source = document.createElement('img'); source.src = '/broken-image.png'
  document.body.append(source)
  document.body.style.overflow = 'auto'
  viewer.open(source)
  const dialog = document.querySelector('dialog'), image = dialog.querySelector('img')
  image.dispatchEvent(new Event('error'))
  expect(dialog.querySelector('[role=status]').textContent).toContain('图片加载失败')
  expect(document.body.style.overflow).toBe('hidden')
  viewer.destroy()
  expect(document.querySelector('dialog')).toBeNull()
  expect(document.body.style.overflow).toBe('auto')
  expect(image.onload).toBeNull()
  expect(image.onerror).toBeNull()
})

it('rejects unsafe image sources without assigning them to a viewer image', () => {
  source = document.createElement('img'); source.setAttribute('src', 'javascript:alert(1)')
  document.body.append(source)
  viewer.open(source)
  expect(document.querySelector('dialog img')).toBeNull()
  expect(document.querySelector('dialog [role=status]').textContent).toContain('图片地址无效')
})
