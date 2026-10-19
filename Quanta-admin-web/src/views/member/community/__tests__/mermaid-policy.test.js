import { describe, expect, it } from 'vitest'
import { MERMAID_LIMITS, mermaidOptions, sanitizeDiagram, validateMermaidSource } from '../mermaid-policy'

describe('Mermaid security policy', () => {
  it('applies app-owned limits and disables author navigation and images', () => {
    const options = mermaidOptions({ width: 320, prefix: 'article-1', fontFamily: 'Arial' })
    expect(options).toMatchObject({ links: false, images: false, maxWidth: 320, limit: 8000, maxEdges: 200, outputLimit: 500000 })
    expect(MERMAID_LIMITS.blocks).toBe(8)
    expect(() => validateMermaidSource('a'.repeat(8001))).toThrow('8000')
    expect(mermaidOptions({ width: Infinity, prefix: '<script>' }).idPrefix).not.toContain('<')
  })

  it('keeps SVG shapes/local markers but strips scripts, foreign HTML, URLs and external CSS', () => {
    const cleaned = sanitizeDiagram('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><marker id="arrow"><path d="M0 0 L10 5 L0 10z" /></marker></defs><path d="M0 0 L50 50" marker-end="url(#arrow)"/><rect width="20" height="20" style="fill:var(--pele-surface);stroke:url(https://bad.invalid/x)"/><a href="https://bad.invalid"><text>链接</text></a><image href="https://bad.invalid/pixel"/><foreignObject><div>HTML</div></foreignObject><script>alert(1)</script><animate attributeName="href" values="javascript:alert(1)"/><text onclick="alert(1)">安全文字</text></svg>')
    const container = document.createElement('div')
    container.append(cleaned)
    expect(container.querySelector('svg')).not.toBeNull()
    expect(container.querySelector('path[marker-end]').getAttribute('marker-end')).toBe(`url(#${container.querySelector('marker').id})`)
    expect(container.querySelector('script,a,image,foreignObject,animate')).toBeNull()
    expect(container.innerHTML).not.toContain('bad.invalid')
    expect(container.innerHTML).not.toContain('onclick')
    expect(container.textContent).toContain('安全文字')
  })

  it('rejects missing or oversized SVG output', () => {
    expect(() => sanitizeDiagram('<p>not SVG</p>')).toThrow()
    expect(() => sanitizeDiagram('x'.repeat(500001))).toThrow()
  })

  it('removes CSS escape/comment URL bypasses and external base URLs', () => {
    const fragment = sanitizeDiagram('<svg xmlns="http://www.w3.org/2000/svg" xml:base="https://bad.invalid"><path fill="u\\72l(https://bad.invalid/pixel)" stroke="url/**/(https://bad.invalid/pixel)" d="M0 0L20 20" /></svg>')
    const root = fragment.querySelector('svg')
    expect(root.hasAttribute('xml:base')).toBe(false)
    expect(root.querySelector('path').hasAttribute('fill')).toBe(false)
    expect(root.querySelector('path').hasAttribute('stroke')).toBe(false)
  })
})
