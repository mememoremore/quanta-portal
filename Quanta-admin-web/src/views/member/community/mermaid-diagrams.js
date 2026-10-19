import { createMermaidRenderer } from './mermaid-client'
import { MERMAID_LIMITS, sanitizeDiagram } from './mermaid-policy'

export function mountMermaidDiagrams(root, prefix, { render } = {}) {
  const renderer = render ? null : createMermaidRenderer()
  const draw = render || renderer.render
  const cleanups = []
  for (const [index, block] of [...root.querySelectorAll('[data-mermaid-block]')].entries()) {
    const source = block.querySelector('pre code').textContent
    const status = block.querySelector('[data-mermaid-status]')
    const code = block.querySelector('[data-mermaid-code]')
    const sourceButton = block.querySelector('[data-mermaid-source]')
    const viewport = block.querySelector('[data-mermaid-viewport]')
    const canvas = block.querySelector('[data-mermaid-canvas]')
    const zoom = block.querySelector('[data-mermaid-zoom]')
    const dialog = block.querySelector('[data-mermaid-dialog]')
    const expand = block.querySelector('[data-mermaid-expand]')
    const menu = block.querySelector('details')
    const percent = block.querySelector('[data-mermaid-percent]')
    let disposed = false, request, timer, observer, generation = 0, width = 0, result, svg, scale = 1, showSource = false

    function sourceVisibility() {
      code.hidden = !showSource
      sourceButton.textContent = showSource ? '收起源码' : '查看源码'
      sourceButton.setAttribute('aria-expanded', String(showSource))
    }

    function zoomDiagram() {
      if (!result || !svg) return
      const available = dialog.open && viewport.clientWidth > 32 ? viewport.clientWidth - 32 : width
      const ratio = Math.min(1, available / result.width) * scale
      const w = result.width * ratio, h = result.height * ratio
      canvas.style.width = `${w}px`
      canvas.style.height = `${h}px`
      svg.style.maxWidth = 'none'
      svg.style.width = `${w}px`
      svg.style.height = `${h}px`
      percent.textContent = `${Math.round(ratio * 100)}%`
      block.querySelector('[data-mermaid-scale=down]').disabled = scale <= 1
      block.querySelector('[data-mermaid-scale=up]').disabled = scale >= 3
    }

    function failed(message) {
      status.textContent = message
      status.hidden = false
      if (dialog.open) dialog.close()
      viewport.hidden = expand.hidden = true
      showSource = true
      sourceVisibility()
    }

    async function redraw() {
      const current = ++generation
      request?.abort()
      request = new AbortController()
      status.textContent = '正在绘制 Mermaid 图表…'
      status.hidden = false
      try {
        const next = await draw(source, { width, prefix: `${prefix}-diagram-${index + 1}`, fontFamily: getComputedStyle(block).fontFamily }, { signal: request.signal })
        if (disposed || generation !== current) return
        const fragment = sanitizeDiagram(next.svg, `${prefix}-diagram-${index + 1}`)
        canvas.replaceChildren(fragment)
        result = next
        svg = canvas.querySelector('svg')
        svg.style.setProperty('--pele-font', getComputedStyle(block).fontFamily)
        scale = 1
        zoomDiagram()
        status.hidden = true
        viewport.hidden = expand.hidden = false
        block.dataset.mermaidReady = 'true'
        sourceVisibility()
      } catch (error) {
        if (disposed || current !== generation || error.name === 'AbortError') return
        const position = Number(error.line) > 0 ? `（第 ${Number(error.line)} 行）` : ''
        failed(`图表未能显示${position}：${String(error.message || '请检查 Mermaid 语法').slice(0, 300)}`)
      }
    }

    function schedule() {
      const nextWidth = Math.round(block.getBoundingClientRect().width - 32)
      if (dialog.open) zoomDiagram()
      if (dialog.open || nextWidth <= 0 || Math.abs(nextWidth - width) < 2) return
      width = nextWidth
      clearTimeout(timer)
      timer = setTimeout(redraw, 250)
    }

    function controls(event) {
      const target = event.target.closest?.('button')
      if (!target || !block.contains(target)) return
      if (target === sourceButton) { showSource = !showSource; sourceVisibility(); menu.open = false }
      if (target.hasAttribute('data-copy-code')) menu.open = false
      if (target === expand && result && typeof dialog.showModal === 'function') {
        menu.open = false
        dialog.insertBefore(viewport, zoom)
        dialog.showModal()
        scale = 1
        zoomDiagram()
      }
      if (target.hasAttribute('data-mermaid-close')) dialog.close()
      if (!result || !target.dataset.mermaidScale) return
      const action = target.dataset.mermaidScale
      scale = action === 'reset' ? 1 : Math.max(1, Math.min(3, scale * (action === 'up' ? 1.25 : 0.8)))
      zoomDiagram()
      if (action === 'reset') viewport.scrollTo?.({ left: 0, top: 0 })
    }

    function restoreViewport() {
      block.insertBefore(viewport, dialog)
      scale = 1
      zoomDiagram()
      if (!disposed) { expand.focus({ preventScroll: true }); schedule() }
    }
    function resize() { if (dialog.open) zoomDiagram() }
    dialog.addEventListener('close', restoreViewport)
    window.addEventListener('resize', resize)
    block.addEventListener('click', controls)
    if (index >= MERMAID_LIMITS.blocks) failed('每篇正文最多预览 8 个 Mermaid 图表，其余图表请查看源码。')
    else {
      sourceVisibility()
      observer = new ResizeObserver(schedule)
      observer.observe(block)
      schedule()
    }
    cleanups.push(() => {
      disposed = true
      generation += 1
      clearTimeout(timer)
      request?.abort()
      observer?.disconnect()
      if (dialog.open) { dialog.close(); restoreViewport() }
      dialog.removeEventListener('close', restoreViewport)
      window.removeEventListener('resize', resize)
      block.removeEventListener('click', controls)
    })
  }
  return () => { cleanups.forEach(cleanup => cleanup()); renderer?.destroy() }
}
