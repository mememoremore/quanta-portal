import { describe, expect, it } from 'vitest'
import { markdownImagePreviews } from '../image-previews'

describe('lightweight forum image previews', () => {
  it('keeps safe images and rejects unsafe URLs', () => {
    expect(markdownImagePreviews('![现场](https://example.com/a.png) ![bad](javascript:alert(1)) ![笔记](/files/note.webp)')).toEqual([
      { alt: '现场', src: 'https://example.com/a.png' },
      { alt: '笔记', src: '/files/note.webp' },
    ])
  })

  it('ignores image syntax inside fenced or inline code', () => {
    const source = '```md\n![代码](https://example.com/code.png)\n```\n`![行内](https://example.com/inline.png)`\n![正文](https://example.com/real.png)'
    expect(markdownImagePreviews(source)).toEqual([{ alt: '正文', src: 'https://example.com/real.png' }])
  })

  it('supports parentheses in image URLs and escaped alt text', () => {
    expect(markdownImagePreviews('![图\\]一](https://example.com/a(1).png)')).toEqual([
      { alt: '图]一', src: 'https://example.com/a(1).png' },
    ])
  })
})
