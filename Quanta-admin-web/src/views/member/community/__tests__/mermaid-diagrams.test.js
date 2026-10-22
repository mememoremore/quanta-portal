import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mountMermaidDiagrams } from '../mermaid-diagrams'
import { markdownToHtml } from '../markdown'

const svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 100"><title id="diagram-title">测试图</title><text x="1" y="20">开始</text></svg>'
const fence = source => '```mermaid\n' + source + '\n```\n'

describe('Mermaid DOM lifecycle', () => {
  let root, cleanup, observers
  beforeEach(() => {
    vi.useFakeTimers()
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value() { this.open = true } })
    Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value() { this.open = false; this.dispatchEvent(new Event('close')) } })
    observers = []
    vi.stubGlobal('ResizeObserver', class {
      constructor(callback) { this.callback = callback; this.disconnect = vi.fn(); observers.push(this) }
      observe() {}
    })
    vi.spyOn(Element.prototype, 'getBoundingClientRect').mockReturnValue({ width: 360, height: 100 })
    root = document.createElement('div')
    root.innerHTML = markdownToHtml(fence('flowchart LR\n A-->B'))
    document.body.append(root)
  })
  afterEach(() => { cleanup?.(); root?.remove(); delete HTMLDialogElement.prototype.showModal; delete HTMLDialogElement.prototype.close; vi.unstubAllGlobals(); vi.restoreAllMocks(); vi.useRealTimers() })

  it('renders clean SVG, keeps source copy and supports source/zoom controls', async () => {
    const render = vi.fn().mockResolvedValue({ svg, width: 320, height: 100 })
    cleanup = mountMermaidDiagrams(root, 'article', { render })
    await vi.advanceTimersByTimeAsync(251)
    expect(root.querySelector('[data-mermaid-ready=true]')).not.toBeNull()
    expect(root.querySelector('[data-mermaid-status]').hidden).toBe(true)
    expect(root.querySelector('pre code').textContent).toBe('flowchart LR\n A-->B\n')
    expect(root.querySelector('pre').hidden).toBe(true)
    expect(root.querySelector('.community-mermaid__toolbar')).toBeNull()
    expect(document.querySelector('.community-media-viewer')).toBeNull()
    expect(root.querySelector('[data-mermaid-source]').closest('details').open).toBe(false)
    root.querySelector('[data-mermaid-source]').click()
    expect(root.querySelector('pre').hidden).toBe(false)
    const expand = root.querySelector('[data-mermaid-expand]')
    expand.focus()
    expand.click()
    const dialog = document.querySelector('.community-media-viewer')
    expect(dialog.open).toBe(true)
    expect(dialog.querySelector('svg')).not.toBe(root.querySelector('svg'))
    const before = dialog.querySelector('.community-media-viewer__content').style.transform
    dialog.querySelector('[data-media-action=in]').click()
    expect(dialog.querySelector('.community-media-viewer__content').style.transform).not.toBe(before)
    dialog.querySelector('[data-media-action=close]').click()
    expect(dialog.open).toBe(false)
    expect(root.querySelector('svg').closest('dialog')).toBeNull()
    expect(document.activeElement).toBe(expand)
    expect(render).toHaveBeenCalledOnce()
    expand.click()
    cleanup()
    expect(document.querySelector('.community-media-viewer')).toBeNull()
    expect(render.mock.lastCall[2].signal.aborted).toBe(true)
    expect(observers[0].disconnect).toHaveBeenCalled()
  })

  it('shows errors as text alongside original source without swallowing surrounding Markdown', async () => {
    const render = vi.fn().mockRejectedValue(new Error('<script>syntax problem</script>'))
    cleanup = mountMermaidDiagrams(root, 'article', { render })
    await vi.advanceTimersByTimeAsync(251)
    expect(root.querySelector('[data-mermaid-status]').textContent).toContain('<script>syntax problem</script>')
    expect(root.querySelector('script')).toBeNull()
    expect(root.querySelector('pre').hidden).toBe(false)
  })

  it('ignores late rendering after cleanup and cancels scheduled work', async () => {
    let resolve
    const render = vi.fn().mockImplementation(() => new Promise(done => { resolve = done }))
    cleanup = mountMermaidDiagrams(root, 'article', { render })
    await vi.advanceTimersByTimeAsync(251)
    cleanup()
    resolve({ svg, width: 320, height: 100 })
    await Promise.resolve()
    expect(root.querySelector('svg')).toBeNull()
    const second = mountMermaidDiagrams(root, 'article', { render })
    second()
    await vi.advanceTimersByTimeAsync(251)
    expect(render).toHaveBeenCalledOnce()
  })

  it('isolates diagram IDs, caps diagram count and rerenders only after width changes', async () => {
    root.innerHTML = markdownToHtml(fence('flowchart LR\n A-->B').repeat(9))
    const render = vi.fn().mockResolvedValue({ svg, width: 320, height: 100 })
    cleanup = mountMermaidDiagrams(root, 'article', { render })
    await vi.advanceTimersByTimeAsync(251)
    expect(render).toHaveBeenCalledTimes(8)
    const ids = [...root.querySelectorAll('svg [id]')].map(element => element.id)
    expect(new Set(ids).size).toBe(8)
    expect(root.lastElementChild.textContent).toContain('最多预览 8 个')
    observers[0].callback()
    await vi.advanceTimersByTimeAsync(251)
    expect(render).toHaveBeenCalledTimes(8)
    vi.spyOn(Element.prototype, 'getBoundingClientRect').mockReturnValue({ width: 300, height: 100 })
    observers[0].callback()
    await vi.advanceTimersByTimeAsync(251)
    expect(render).toHaveBeenCalledTimes(9)
  })

  it('syncs ordinary updates without drawing again and debounces only the changed diagram', async () => {
    root.innerHTML = markdownToHtml(fence('flowchart LR\n A-->B') + fence('flowchart LR\n C-->D'))
    let finish
    const render = vi.fn().mockResolvedValue({ svg, width: 320, height: 100 })
    cleanup = mountMermaidDiagrams(root, 'article', { render })
    await vi.advanceTimersByTimeAsync(251)
    const [first, second] = root.querySelectorAll('[data-mermaid-block]')
    const oldSvg = first.querySelector('svg'), secondSvg = second.querySelector('svg')
    cleanup.sync()
    await vi.advanceTimersByTimeAsync(400)
    expect(render).toHaveBeenCalledTimes(2)
    render.mockImplementation(() => new Promise(resolve => { finish = resolve }))
    first.querySelector('pre code').textContent = 'flowchart LR\n A-->C'
    cleanup.sync()
    await vi.advanceTimersByTimeAsync(200)
    first.querySelector('pre code').textContent = 'flowchart LR\n A-->E'
    cleanup.sync()
    await vi.advanceTimersByTimeAsync(299)
    expect(render).toHaveBeenCalledTimes(2)
    expect(first.querySelector('svg')).toBe(oldSvg)
    await vi.advanceTimersByTimeAsync(2)
    expect(render).toHaveBeenCalledTimes(3)
    expect(render.mock.lastCall[0]).toContain('A-->E')
    expect(first.querySelector('svg')).toBe(oldSvg)
    finish({ svg, width: 320, height: 100 })
    await vi.advanceTimersByTimeAsync(1)
    expect(first.querySelector('svg')).not.toBe(oldSvg)
    expect(second.querySelector('svg')).toBe(secondSvg)
  })

  it('retains last good SVG with an explicit stale error and recovers from incomplete syntax', async () => {
    const render = vi.fn().mockResolvedValue({ svg, width: 320, height: 100 })
    cleanup = mountMermaidDiagrams(root, 'article', { render })
    await vi.advanceTimersByTimeAsync(251)
    const old = root.querySelector('svg')
    render.mockRejectedValueOnce(new Error('语法未完成'))
    root.querySelector('pre code').textContent = 'flowchart LR\n A['
    cleanup.sync()
    await vi.advanceTimersByTimeAsync(301)
    expect(root.querySelector('svg')).toBe(old)
    expect(root.querySelector('[data-mermaid-viewport]').hidden).toBe(false)
    expect(root.querySelector('[data-mermaid-status]').textContent).toContain('上一次')
    expect(root.querySelector('pre').hidden).toBe(false)
    root.querySelector('pre code').textContent = 'flowchart LR\n A-->C'
    cleanup.sync()
    await vi.advanceTimersByTimeAsync(301)
    expect(root.querySelector('[data-mermaid-status]').hidden).toBe(true)
    expect(root.querySelector('svg')).not.toBe(old)
  })

  it('invalidates work immediately on source edits and ignores late success or failure', async () => {
    const pending = []
    const render = vi.fn().mockImplementation(() => new Promise((resolve, reject) => pending.push({ resolve, reject })))
    cleanup = mountMermaidDiagrams(root, 'article', { render })
    await vi.advanceTimersByTimeAsync(251)
    root.querySelector('pre code').textContent = 'flowchart LR\n A-->C'
    cleanup.sync()
    expect(render.mock.calls[0][2].signal.aborted).toBe(true)
    pending[0].resolve({ svg, width: 320, height: 100 })
    await vi.advanceTimersByTimeAsync(1)
    expect(root.querySelector('svg')).toBeNull()
    await vi.advanceTimersByTimeAsync(300)
    root.querySelector('pre code').textContent = 'flowchart LR\n A-->D'
    cleanup.sync()
    pending[1].reject(new Error('过期错误'))
    await vi.advanceTimersByTimeAsync(1)
    expect(root.textContent).not.toContain('过期错误')
    await vi.advanceTimersByTimeAsync(300)
    const block = root.querySelector('[data-mermaid-block]')
    block.remove()
    cleanup.sync()
    expect(render.mock.calls[2][2].signal.aborted).toBe(true)
    pending[2].resolve({ svg, width: 320, height: 100 })
    await vi.advanceTimersByTimeAsync(1)
    expect(block.querySelector('svg')).toBeNull()
  })

  it('enforces the chart cap after removals and gives newly inserted diagrams unique ids', async () => {
    root.innerHTML = markdownToHtml(Array.from({ length: 9 }, (_, i) => fence(`flowchart LR\n A${i}-->B${i}`)).join(''))
    const render = vi.fn().mockResolvedValue({ svg, width: 320, height: 100 })
    cleanup = mountMermaidDiagrams(root, 'article', { render })
    await vi.advanceTimersByTimeAsync(251)
    const last = root.lastElementChild
    expect(last.querySelector('svg')).toBeNull()
    root.firstElementChild.remove()
    cleanup.sync()
    await vi.advanceTimersByTimeAsync(301)
    expect(last.querySelector('svg')).not.toBeNull()
    root.firstElementChild.remove()
    const container = document.createElement('div')
    container.innerHTML = markdownToHtml(fence('flowchart LR\n X-->Y'))
    root.append(container.firstElementChild)
    cleanup.sync()
    await vi.advanceTimersByTimeAsync(251)
    const ids = [...root.querySelectorAll('svg [id]')].map(element => element.id)
    expect(ids).toHaveLength(8)
    expect(new Set(ids).size).toBe(8)
  })
})
