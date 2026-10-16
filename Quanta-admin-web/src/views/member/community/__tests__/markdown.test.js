import { describe, expect, it } from 'vitest'
import { markdownImagePreviews, markdownToHtml } from '../markdown'

describe('forum Markdown renderer', () => {
  it('highlights YAML and its yml alias and includes a safe copy-code control', () => {
    for (const language of ['yaml', 'yml']) {
      const html = markdownToHtml('```' + language + '\non:\n  push:\n    branches: [main]\n```')
      expect(html).toContain('hljs-attr')
      expect(html).toContain('data-copy-code')
      expect(html).toContain('复制代码')
    }
    expect(markdownToHtml('```\npermissions:\n  contents: read\n```')).toContain('hljs-')
  })
  it('renders headings, code fences, links and images', () => {
    const html = markdownToHtml('# 标题\n\n```js\nconst a = 1\n```\n\n![图](https://example.com/a.png)\n\n[资料](https://example.com)')
    expect(html).toContain('<h1>标题</h1>')
    expect(html).toContain('<pre class="hljs"><code class="language-js">')
    expect(html).toContain('hljs-keyword')
    expect(html).toContain('<img src="https://example.com/a.png"')
    expect(html).toContain('alt="图"')
    expect(html).toContain('rel="noopener noreferrer"')
  })

  it('renders nested headings, fenced code and tables from a long technical post', () => {
    const html = markdownToHtml('1. 常见浅拷贝的方式\n\n   ### Object.assign\n\n   ```js\n   const newObj = Object.assign({}, obj)\n   ```\n\n3. Content-Type\n\n   | 数据格式 | Content-Type |\n   | --- | --- |\n   | JSON | `application/json` |\n\n#### 四级标题')
    expect(html).toContain('<h3>Object.assign</h3>')
    expect(html).toContain('<h4>四级标题</h4>')
    expect(html).toContain('<table>')
    expect(html).toContain('<td>JSON</td>')
    expect(html).toContain('<pre class="hljs"><code class="language-js">')
  })

  it('shows raw and already-encoded arrows as text without double escaping', () => {
    const html = markdownToHtml('f->F.prototype->Object.prototype\n\nf-&gt;F.prototype')
    const element = document.createElement('div')
    element.innerHTML = html
    expect(element.textContent).toContain('f->F.prototype->Object.prototype')
    expect(element.textContent).toContain('f->F.prototype')
  })

  it('escapes HTML and rejects unsafe image and link schemes', () => {
    const html = markdownToHtml('<script>alert(1)</script>\n\n![bad](javascript:alert(1))\n\n[x](javascript:alert(1))')
    expect(html).toContain('&lt;script&gt;')
    expect(html).not.toContain('<script>')
    expect(html).not.toContain('src="javascript:')
    expect(html).not.toContain('href="javascript:')
  })

  it('uses only safe Markdown images in feed previews', () => {
    const images = markdownImagePreviews('![现场照片](https://example.com/a.png)\n![bad](javascript:alert(1))\n![笔记](/files/note.webp)')
    expect(images).toEqual([
      { alt: '现场照片', src: 'https://example.com/a.png' },
      { alt: '笔记', src: '/files/note.webp' },
    ])
  })
})
