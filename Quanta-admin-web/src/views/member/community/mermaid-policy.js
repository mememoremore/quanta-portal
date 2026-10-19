import DOMPurify from 'dompurify'
import { MERMAID_LIMITS } from './mermaid-options'
export { MERMAID_LIMITS, mermaidOptions, validateMermaidSource } from './mermaid-options'

const safeStyleNames = new Set('max-width height color font font-family font-size font-weight font-style letter-spacing word-spacing text-transform direction rx fill stroke stroke-width stroke-dasharray opacity fill-opacity stroke-opacity text-anchor dominant-baseline'.split(' '))

export function sanitizeDiagram(svg, prefix = 'diagram') {
  if (typeof svg !== 'string' || svg.length > MERMAID_LIMITS.output) throw new Error('图表输出超过安全限制，请简化图表。')
  const fragment = DOMPurify.sanitize(svg, {
    USE_PROFILES: { svg: true, svgFilters: true }, RETURN_DOM_FRAGMENT: true,
    FORBID_TAGS: ['script', 'foreignObject', 'style', 'image', 'a', 'use', 'animate', 'animateMotion', 'animateTransform', 'set', 'feImage'],
    FORBID_ATTR: ['href', 'xlink:href', 'src', 'srcset', 'xml:base'],
  })
  const root = fragment.querySelector('svg')
  if (!root || root.parentNode !== fragment || fragment.children.length !== 1) throw new Error('图表输出无效，请查看源码。')
  const safePrefix = String(prefix).replace(/[^a-zA-Z0-9_-]/g, '') || 'diagram'
  const ids = new Map()
  for (const element of fragment.querySelectorAll('[id]')) {
    const old = element.id
    const next = `${safePrefix}-${ids.size + 1}`
    ids.set(old, next)
    element.id = next
  }
  for (const element of fragment.querySelectorAll('*')) {
    for (const attribute of [...element.attributes]) {
      const name = attribute.name.toLowerCase(), value = attribute.value
      if (name.startsWith('on')) { element.removeAttribute(attribute.name); continue }
      if (name === 'style') {
        for (const property of Array.from({ length: element.style.length }, (_, i) => element.style.item(i))) {
          const content = element.style.getPropertyValue(property)
          const allowed = safeStyleNames.has(property) || /^--_[a-z0-9-]+$/i.test(property)
          // CSS is not sanitized by DOMPurify. Keep only inert drawing styles.
          if (!allowed || /url\s*\(|expression|@import|javascript:|data:|\\|\/\*/i.test(content)) element.style.removeProperty(property)
        }
        if (!element.style.length) element.removeAttribute('style')
      } else if (/\\|\/\*/.test(value)) {
        // Escaped/commented CSS function names must not bypass URL checks.
        element.removeAttribute(attribute.name)
      } else if (['aria-labelledby', 'aria-describedby'].includes(name)) {
        const references = value.split(/\s+/).map(id => ids.get(id)).filter(Boolean)
        if (references.length) element.setAttribute(name, references.join(' '))
        else element.removeAttribute(name)
      } else if (/url\s*\(/i.test(value)) {
        const local = value.match(/^url\(#([a-zA-Z0-9_:-]+)\)$/)?.[1]
        if (local && ids.has(local)) element.setAttribute(name, `url(#${ids.get(local)})`)
        else element.removeAttribute(name)
      }
    }
  }
  return fragment
}
