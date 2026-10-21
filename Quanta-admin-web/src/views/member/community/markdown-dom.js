const isDiagram = node => node.nodeType === 1 && node.hasAttribute('data-mermaid-block')
const sourceOf = block => block.querySelector('[data-mermaid-code] code').textContent

// Consume only HTML produced by our safe Markdown renderer. Mermaid islands are
// opaque: patching ordinary prose must not touch their SVG, controls or workers.
export function reconcileMarkdown(root, html) {
  const template = root.ownerDocument.createElement('template')
  template.innerHTML = html
  const oldBlocks = [...root.querySelectorAll('[data-mermaid-block]')]
  const nextBlocks = [...template.content.querySelectorAll('[data-mermaid-block]')]
  const matches = new Map(), used = new Set(), bySource = new Map(), nextBySource = new Map()
  for (const block of oldBlocks) {
    const source = sourceOf(block)
    if (!bySource.has(source)) bySource.set(source, [])
    bySource.get(source).push(block)
  }
  for (const block of nextBlocks) {
    const source = sourceOf(block)
    if (!nextBySource.has(source)) nextBySource.set(source, [])
    nextBySource.get(source).push(block)
  }
  // Preserve occurrence order, and choose the nearest surviving occurrence when
  // one of several identical diagrams was edited. Never share a live island.
  for (const [source, nextGroup] of nextBySource) {
    const oldGroup = bySource.get(source) || []
    const count = Math.min(oldGroup.length, nextGroup.length)
    let start = 0
    for (let index = 0; index < count; index += 1) {
      const candidates = oldGroup.slice(start, oldGroup.length - (count - index) + 1)
      const position = nextBlocks.indexOf(nextGroup[index])
      const old = candidates.reduce((best, candidate) => Math.abs(oldBlocks.indexOf(candidate) - position) < Math.abs(oldBlocks.indexOf(best) - position) ? candidate : best)
      start = oldGroup.indexOf(old) + 1
      matches.set(nextGroup[index], old)
      used.add(old)
    }
  }
  for (const [index, block] of nextBlocks.entries()) {
    if (matches.has(block)) continue
    const candidates = oldBlocks.filter(old => !used.has(old))
    if (!candidates.length) continue
    // Anchor changed diagrams to unchanged neighbors instead of shifting every
    // identity after an insertion/deletion earlier in the document.
    let expected = index
    const before = nextBlocks.slice(0, index).reverse().find(next => matches.has(next))
    const after = nextBlocks.slice(index + 1).find(next => matches.has(next))
    if (before) expected = oldBlocks.indexOf(matches.get(before)) + index - nextBlocks.indexOf(before)
    else if (after) expected = oldBlocks.indexOf(matches.get(after)) - nextBlocks.indexOf(after) + index
    const old = candidates.reduce((best, candidate) => Math.abs(oldBlocks.indexOf(candidate) - expected) < Math.abs(oldBlocks.indexOf(best) - expected) ? candidate : best)
    matches.set(block, old)
    used.add(old)
  }

  function patchChildren(parent, incoming) {
    let cursor = parent.firstChild
    for (const next of [...incoming.childNodes]) {
      const diagram = isDiagram(next)
      const compatible = cursor && !isDiagram(cursor) && !diagram && cursor.nodeType === next.nodeType && cursor.nodeName === next.nodeName
      const node = diagram ? matches.get(next) || next.cloneNode(true) : compatible ? cursor : next.cloneNode(false)
      if (node !== cursor) parent.insertBefore(node, cursor)
      if (diagram) {
        const code = node.querySelector('[data-mermaid-code] code'), source = sourceOf(next)
        if (code.textContent !== source) code.textContent = source
      } else if (node.nodeType === 1) {
        for (const attr of [...node.attributes]) if (!next.hasAttribute(attr.name)) node.removeAttribute(attr.name)
        for (const attr of [...next.attributes]) if (node.getAttribute(attr.name) !== attr.value) node.setAttribute(attr.name, attr.value)
        patchChildren(node, next)
      } else if (node.nodeValue !== next.nodeValue) node.nodeValue = next.nodeValue
      cursor = node.nextSibling
    }
    while (cursor) { const next = cursor.nextSibling; cursor.remove(); cursor = next }
  }
  patchChildren(root, template.content)
}
