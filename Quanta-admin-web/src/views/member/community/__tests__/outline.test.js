import { describe, expect, it } from 'vitest'
import { markdownToDocument } from '../markdown'

describe('article outline', () => {
  it('restores backend-escaped Mermaid arrows without adding diagram text to TOC', () => {
    const result = markdownToDocument('## 流程\n\n```mermaid\nflowchart LR\n A--&gt;B\n```', 'article')
    const element = document.createElement('div')
    element.innerHTML = result.html
    expect(element.querySelector('pre code').textContent).toContain('A-->B')
    expect(result.headings.map(heading => heading.text)).toEqual(['流程'])
  })

  it('uses unique stable IDs, plain labels, supports repeated and nested headings', () => {
    const source = '## 重复 **标题**\n\n1. 内容\n\n   ### 嵌套 `code`\n\n## 重复 **标题**\n\n```md\n## 不是标题\n```'
    const result = markdownToDocument(source, 'article-one')
    expect(result.headings.map(h => h.text)).toEqual(['重复 标题', '嵌套 code', '重复 标题'])
    expect(new Set(result.headings.map(h => h.id)).size).toBe(3)
    expect(result.html).toContain('id="article-one-1"')
    expect(markdownToDocument(source, 'article-two').headings[0].id).not.toBe(result.headings[0].id)
    expect(markdownToDocument(source, 'article-one').headings).toEqual(result.headings)
  })
  it('keeps unsafe HTML inert and creates no headings from raw HTML', () => {
    const result = markdownToDocument('## <img src=x onerror=alert(1)>\n\n<h2>not a heading</h2>', 'safe')
    expect(result.headings).toHaveLength(1)
    expect(result.html).not.toContain('<img src=x')
    expect(result.html).toContain('&lt;img')
  })
})
