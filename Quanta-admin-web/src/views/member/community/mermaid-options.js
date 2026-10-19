export const MERMAID_LIMITS = Object.freeze({ source: 8000, output: 500000, edges: 200, blocks: 8 })

export function validateMermaidSource(source) {
  if (typeof source !== 'string' || !source.trim()) throw new Error('Mermaid 图表内容为空')
  if (source.length > MERMAID_LIMITS.source) throw new Error('单个 Mermaid 图表最多支持 8000 字符，请简化图表。')
}

export function mermaidOptions({ width, prefix = 'diagram', fontFamily = 'sans-serif' } = {}) {
  return {
    links: false, images: false, responsive: true, autoDirection: true,
    limit: MERMAID_LIMITS.source, outputLimit: MERMAID_LIMITS.output, maxEdges: MERMAID_LIMITS.edges,
    maxWidth: Number.isFinite(width) ? Math.max(180, Math.min(1600, width)) : 640,
    idPrefix: String(prefix).replace(/[^a-zA-Z0-9_-]/g, '').slice(0, 100) || 'diagram',
    fontFamily: String(fontFamily).replace(/[<>;{}]/g, '').slice(0, 200),
    fontSize: 15, padding: 16,
  }
}
