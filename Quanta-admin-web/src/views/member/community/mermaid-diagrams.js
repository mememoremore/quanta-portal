import { createMermaidRenderer } from './mermaid-client'
import { MERMAID_LIMITS, sanitizeDiagram } from './mermaid-policy'
import { createMediaViewer } from './media-viewer'

export function mountMermaidDiagrams(root, prefix, { render, viewer: suppliedViewer } = {}) {
  const renderer = render ? null : createMermaidRenderer()
  const draw = render || renderer.render
  const viewer = suppliedViewer || createMediaViewer()
  const states = new Map()
  let sequence = 0, disposed = false
  function sync() {
    if (disposed) return
    const blocks = [...root.querySelectorAll('[data-mermaid-block]')]
    const present = new Set(blocks)
    for (const [block, state] of states) {
      if (!present.has(block)) { state.destroy(); states.delete(block) }
    }
    for (const [index, block] of blocks.entries()) {
      const allowed = index < MERMAID_LIMITS.blocks
      if (states.has(block)) states.get(block).update(allowed)
      else states.set(block, createDiagram(block, `${prefix}-diagram-${++sequence}`, draw, allowed, viewer))
    }
  }
  function destroy() {
    if (disposed) return
    disposed = true
    states.forEach(state => state.destroy())
    states.clear()
    renderer?.destroy()
    viewer.destroy()
  }
  sync()
  return Object.assign(destroy, { sync })
}

function createDiagram(block, prefix, draw, initialAllowed, viewer) {
  let source = block.querySelector('pre code').textContent, allowed = initialAllowed
  const status = block.querySelector('[data-mermaid-status]')
  const code = block.querySelector('[data-mermaid-code]')
  const sourceButton = block.querySelector('[data-mermaid-source]')
  const viewport = block.querySelector('[data-mermaid-viewport]')
  const canvas = block.querySelector('[data-mermaid-canvas]')
  const expand = block.querySelector('[data-mermaid-expand]')
  const menu = block.querySelector('details')
  let disposed = false, request, timer, observer, generation = 0, width = 0, result, svg, showSource = false, errorSource = false

  function sourceVisibility() {
    const visible = showSource || errorSource
    code.hidden = !visible
    sourceButton.textContent = visible ? '收起源码' : '查看源码'
    sourceButton.setAttribute('aria-expanded', String(visible))
  }

  function zoomDiagram() {
    if (!result || !svg) return
    const ratio = Math.min(1, width / result.width)
    const w = result.width * ratio, h = result.height * ratio
    canvas.style.width = `${w}px`
    canvas.style.height = `${h}px`
    svg.style.maxWidth = 'none'
    svg.style.width = `${w}px`
    svg.style.height = `${h}px`
  }

  function failed(message) {
    const keepPrevious = Boolean(result) && allowed
    status.textContent = message + (keepPrevious ? '；当前保留上一次成功绘制的图表，尚未反映最新源码。' : '')
    status.hidden = false
    block.dataset.mermaidState = 'error'
    block.setAttribute('aria-busy', 'false')
    delete block.dataset.mermaidReady
    viewport.hidden = expand.hidden = !keepPrevious
    errorSource = true
    sourceVisibility()
  }

  async function redraw(current) {
    if (disposed || current !== generation || !allowed || width <= 0) return
    request = new AbortController()
    try {
      const next = await draw(source, { width, prefix, fontFamily: getComputedStyle(block).fontFamily }, { signal: request.signal })
      if (disposed || generation !== current) return
      const fragment = sanitizeDiagram(next.svg, prefix)
      canvas.replaceChildren(fragment)
      result = next
      svg = canvas.querySelector('svg')
      svg.style.setProperty('--pele-font', getComputedStyle(block).fontFamily)
      zoomDiagram()
      status.hidden = true
      viewport.hidden = expand.hidden = false
      block.dataset.mermaidReady = 'true'
      block.dataset.mermaidState = 'ready'
      block.setAttribute('aria-busy', 'false')
      errorSource = false
      sourceVisibility()
    } catch (error) {
      if (disposed || current !== generation || error.name === 'AbortError') return
      const position = Number(error.line) > 0 ? `（第 ${Number(error.line)} 行）` : ''
      failed(`图表未能显示${position}：${String(error.message || '请检查 Mermaid 语法').slice(0, 300)}`)
    }
  }

  function invalidate() {
    generation += 1
    clearTimeout(timer)
    request?.abort()
  }

  function queueRedraw(delay) {
    invalidate()
    const current = generation
    block.dataset.mermaidState = 'pending'
    block.setAttribute('aria-busy', 'true')
    delete block.dataset.mermaidReady
    status.textContent = '正在绘制 Mermaid 图表…'
    // Preserve the last good drawing without adding a loading row on updates.
    status.hidden = Boolean(result)
    timer = setTimeout(() => redraw(current), delay)
  }

  function schedule() {
    const nextWidth = Math.round(block.getBoundingClientRect().width - 32)
    if (disposed || !allowed || nextWidth <= 0 || Math.abs(nextWidth - width) < 2) return
    width = nextWidth
    zoomDiagram()
    queueRedraw(250)
  }

  function update(nextAllowed) {
    const nextSource = code.querySelector('code').textContent
    if (nextSource === source && nextAllowed === allowed) return
    source = nextSource
    allowed = nextAllowed
    if (!allowed) { invalidate(); failed('每篇正文最多预览 8 个 Mermaid 图表，其余图表请查看源码。'); return }
    if (width <= 0) width = Math.max(0, Math.round(block.getBoundingClientRect().width - 32))
    queueRedraw(300)
  }

  function controls(event) {
    if (viewport.contains(event.target) && svg) { viewer.open(svg); return }
    const target = event.target.closest?.('button')
    if (!target || !block.contains(target)) return
    if (target === sourceButton) { showSource = !(showSource || errorSource); errorSource = false; sourceVisibility(); menu.open = false }
    if (target.hasAttribute('data-copy-code')) menu.open = false
    if (target === expand && result) {
      menu.open = false
      viewer.open(svg)
    }
  }
  function keyboard(event) {
    if (event.target === viewport && ['Enter', ' '].includes(event.key) && svg) { event.preventDefault(); viewer.open(svg) }
  }
  viewport.tabIndex = 0
  viewport.setAttribute('role', 'button')
  viewport.setAttribute('aria-label', '全屏查看图表')
  block.addEventListener('click', controls)
  block.addEventListener('keydown', keyboard)
  sourceVisibility()
  observer = new ResizeObserver(schedule)
  observer.observe(block)
  if (allowed) schedule()
  else failed('每篇正文最多预览 8 个 Mermaid 图表，其余图表请查看源码。')
  function destroy() {
    if (disposed) return
    disposed = true
    invalidate()
    observer?.disconnect()
    block.removeEventListener('click', controls)
    block.removeEventListener('keydown', keyboard)
  }
  return { update, destroy }
}
