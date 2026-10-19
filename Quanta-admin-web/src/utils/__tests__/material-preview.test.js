import { describe, expect, it } from 'vitest'
import { checkPreviewSize, materialPreviewKind, previewImageType, readPreviewText, renderMaterialMarkdown } from '../material-preview'

describe('material preview boundaries', () => {
  it('classifies by an explicit extension allowlist, not server MIME or filename contents', () => {
    for (const name of ['笔记.MD', 'readme.markdown']) expect(materialPreviewKind(name)).toBe('markdown')
    for (const name of ['main.ts', 'config.yml', 'demo.html', '.env', 'Dockerfile', '数据.csv']) expect(materialPreviewKind(name)).toBe('text')
    expect(materialPreviewKind('教程.PDF')).toBe('pdf')
    expect(materialPreviewKind('照片.webp')).toBe('image')
    for (const name of ['slides.pptx', 'archive.zip', 'image.svg', 'test.txt.exe', 'file', '']) expect(materialPreviewKind(name)).toBe('unsupported')
  })

  it('limits actual text and binary preview sizes', () => {
    expect(() => checkPreviewSize(2 * 1024 * 1024 + 1, 'text')).toThrow('2 MB')
    expect(() => checkPreviewSize(21 * 1024 * 1024, 'pdf')).toThrow('20 MB')
    expect(() => checkPreviewSize(100, 'markdown')).not.toThrow()
  })

  it('decodes UTF-8/BOM and UTF-16 without treating binary data as text', async () => {
    await expect(readPreviewText(new Blob(['\ufeff你好，Quanta']))).resolves.toBe('你好，Quanta')
    await expect(readPreviewText(new Blob([new Uint8Array([255, 254, 45, 78, 135, 101])]))).resolves.toBe('中文')
    await expect(readPreviewText(new Blob([new Uint8Array([0, 1, 2, 3])]))).rejects.toThrow('不是可预览的文本')
  })

  it('never renders uploaded HTML, active URLs or automatic remote images', () => {
    const html = renderMaterialMarkdown('# 标题\n<script>alert(1)</script>\n\n![远程图片](https://tracker.invalid/a)\n\n[安全](https://example.com)\n\n[危险](javascript:alert(1))\n\n```html\n<img onerror=alert(1)>\n```')
    expect(html).toContain('<h1>标题</h1>')
    expect(html).not.toContain('<script>')
    expect(html).not.toContain('<img')
    expect(html).not.toContain('src=')
    expect(html).not.toContain('href="javascript:')
    expect(html).toContain('rel="noopener noreferrer"')
    expect(html).toContain('&lt;img onerror=alert(1)&gt;')
  })

  it('only assigns image MIME types to known raster signatures', () => {
    expect(previewImageType(new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]))).toBe('image/png')
    expect(previewImageType(new Uint8Array([255, 216, 255, 224]))).toBe('image/jpeg')
    expect(previewImageType(new TextEncoder().encode('<svg onload="alert(1)">'))).toBe('')
  })
})
