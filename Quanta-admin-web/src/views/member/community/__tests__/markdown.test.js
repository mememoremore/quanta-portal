import { describe, expect, it } from 'vitest'
import { markdownToHtml } from '../markdown'

describe('forum Markdown renderer', () => {
  it('renders headings, code fences, links and images', () => {
    const html = markdownToHtml('# 标题\n\n```js\nconst a = 1\n```\n\n![图](https://example.com/a.png)\n\n[资料](https://example.com)')
    expect(html).toContain('<h1>标题</h1>')
    expect(html).toContain('<pre><code class="language-js">const a = 1</code></pre>')
    expect(html).toContain('<img src="https://example.com/a.png"')
    expect(html).toContain('rel="noopener noreferrer"')
  })

  it('escapes HTML and rejects unsafe image and link schemes', () => {
    const html = markdownToHtml('<script>alert(1)</script>\n\n![bad](javascript:alert(1))\n\n[x](javascript:alert(1))')
    expect(html).toContain('&lt;script&gt;')
    expect(html).not.toContain('<script>')
    expect(html).not.toContain('src="javascript:')
    expect(html).not.toContain('href="javascript:')
  })
})
