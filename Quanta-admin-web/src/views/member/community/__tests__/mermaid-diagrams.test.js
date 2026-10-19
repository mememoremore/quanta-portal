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
    expect(root.querySelector('[data-mermaid-dialog]')).not.toBeNull()
    expect(root.querySelector('[data-mermaid-zoom]').closest('dialog').open).toBe(false)
    expect(root.querySelector('[data-mermaid-source]').closest('details').open).toBe(false)
    root.querySelector('[data-mermaid-source]').click()
    expect(root.querySelector('pre').hidden).toBe(false)
    const expand = root.querySelector('[data-mermaid-expand]')
    expand.focus()
    expand.click()
    expect(root.querySelector('dialog').open).toBe(true)
    expect(root.querySelector('svg').closest('dialog')).not.toBeNull()
    const before = root.querySelector('svg').style.width
    root.querySelector('[data-mermaid-scale=up]').click()
    expect(root.querySelector('svg').style.width).not.toBe(before)
    root.querySelector('[data-mermaid-close]').click()
    expect(root.querySelector('dialog').open).toBe(false)
    expect(root.querySelector('svg').closest('dialog')).toBeNull()
    expect(document.activeElement).toBe(expand)
    expect(root.querySelector('svg').style.width).toBe(before)
    expand.click()
    cleanup()
    expect(root.querySelector('dialog').open).toBe(false)
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
})
