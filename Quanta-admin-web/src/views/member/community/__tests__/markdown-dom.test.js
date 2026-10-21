import { describe, expect, it } from 'vitest'
import { reconcileMarkdown } from '../markdown-dom'
import { markdownToHtml } from '../markdown'

const fence = source => '```mermaid\n' + source + '\n```\n'
const a = fence('flowchart LR\n A-->B'), b = fence('flowchart LR\n C-->D')
const blocks = root => [...root.querySelectorAll('[data-mermaid-block]')]

describe('stable Markdown chart islands', () => {
  it('updates text while preserving the actual diagram and SVG nodes', () => {
    const root = document.createElement('div')
    reconcileMarkdown(root, markdownToHtml('## 标题\n\n' + a + '\n正文'))
    const chart = blocks(root)[0]
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    chart.querySelector('[data-mermaid-canvas]').append(svg)
    chart.querySelector('details').open = true
    reconcileMarkdown(root, markdownToHtml('## 新标题\n\n' + a + '\n正文已修改'))
    expect(blocks(root)[0]).toBe(chart)
    expect(chart.querySelector('svg')).toBe(svg)
    expect(chart.querySelector('details').open).toBe(true)
    expect(root.querySelector('h2').textContent).toBe('新标题')
    expect(root.textContent).toContain('正文已修改')
  })

  it('matches unchanged sources before inserting or updating neighboring diagrams', () => {
    const root = document.createElement('div')
    reconcileMarkdown(root, markdownToHtml(a + b))
    const [first, second] = blocks(root)
    reconcileMarkdown(root, markdownToHtml(fence('flowchart LR\n X-->Y') + a + b))
    expect(blocks(root)[1]).toBe(first)
    expect(blocks(root)[2]).toBe(second)
    reconcileMarkdown(root, markdownToHtml(a + fence('flowchart LR\n C-->E')))
    expect(blocks(root)[0]).toBe(first)
    expect(blocks(root)[1]).toBe(second)
    expect(second.querySelector('pre code').textContent).toContain('C-->E')
  })

  it('keeps repeated identical diagrams distinct and removes only deleted islands', () => {
    const root = document.createElement('div')
    reconcileMarkdown(root, markdownToHtml(a + a + b))
    const [first, duplicate, last] = blocks(root)
    reconcileMarkdown(root, markdownToHtml('文字\n\n' + a + a + b))
    expect(blocks(root)).toEqual([first, duplicate, last])
    reconcileMarkdown(root, markdownToHtml(a + b))
    expect(blocks(root)).toEqual([first, last])
    expect(root.contains(duplicate)).toBe(false)
  })

  it('does not swap identical neighboring charts when only the first source is edited', () => {
    const root = document.createElement('div')
    reconcileMarkdown(root, markdownToHtml(a + a))
    const [first, second] = blocks(root)
    reconcileMarkdown(root, markdownToHtml(b + a))
    expect(blocks(root)).toEqual([first, second])
    expect(first.querySelector('pre code').textContent).toContain('C-->D')
    expect(second.querySelector('pre code').textContent).toContain('A-->B')
  })

  it('preserves nested list/blockquotes, heading ids, attributes and safe Markdown semantics', () => {
    const root = document.createElement('div')
    const nested = '- 条目\n\n  ```mermaid\n  flowchart LR\n   A-->B\n  ```\n\n> 引用\n\n[链接](https://example.com)'
    reconcileMarkdown(root, markdownToHtml(nested))
    const chart = blocks(root)[0]
    reconcileMarkdown(root, markdownToHtml(nested.replace('条目', '**新条目**').replace('引用', '新引用')))
    expect(root.querySelector('li [data-mermaid-block]')).toBe(chart)
    expect(root.querySelector('li strong').textContent).toBe('新条目')
    expect(root.querySelector('blockquote').textContent).toContain('新引用')
    expect(root.querySelector('a').rel).toBe('noopener noreferrer')
    reconcileMarkdown(root, markdownToHtml('<script>bad()</script>\n\n## 结束'))
    expect(root.querySelector('script')).toBeNull()
    expect(blocks(root)).toHaveLength(0)
    expect(root.querySelector('h2').textContent).toBe('结束')
  })
})
