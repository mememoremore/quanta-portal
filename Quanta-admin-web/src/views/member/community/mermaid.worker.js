import { render } from 'pele'
import { mermaidOptions, validateMermaidSource } from './mermaid-options'

// The module/engine is downloaded only when a MarkdownContent has diagrams.
self.onmessage = ({ data }) => {
  try {
    validateMermaidSource(data.source)
    const options = mermaidOptions(data.options)
    const context = typeof OffscreenCanvas === 'function' ? new OffscreenCanvas(1, 1).getContext('2d') : null
    if (context) {
      options.measurer = { width(text, size, style) {
        context.font = `${style & 2 ? 'italic ' : ''}${style & 1 ? 'bold ' : ''}${size}px ${style & 4 ? 'monospace' : options.fontFamily}`
        return context.measureText(text).width
      } }
    }
    const result = render(data.source, options)
    if (![result.width, result.height].every(size => Number.isFinite(size) && size > 0 && size <= 20000)) {
      throw new Error('图表尺寸过大，请拆分图表。')
    }
    self.postMessage({ id: data.id, result: { svg: result.svg, width: result.width, height: result.height, type: result.type } })
  } catch (error) {
    self.postMessage({ id: data.id, error: { message: String(error.message || '图表无法绘制').slice(0, 300), code: error.code, line: Number(error.line) || 0 } })
  }
}
self.postMessage({ ready: true })
