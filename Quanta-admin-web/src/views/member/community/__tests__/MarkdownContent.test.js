import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MarkdownContent from '../MarkdownContent.vue'
import { mountMermaidDiagrams } from '../mermaid-diagrams'

vi.mock('../mermaid-diagrams', () => ({ mountMermaidDiagrams: vi.fn(() => vi.fn()) }))

describe('Markdown code copying', () => {
  beforeEach(() => vi.clearAllMocks())
  it('never loads diagram controllers for ordinary Markdown', async () => {
    const wrapper = mount(MarkdownContent, { props: { source: '## 普通正文' } })
    await flushPromises()
    expect(mountMermaidDiagrams).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('initializes diagrams lazily, cleans old content and copies source rather than SVG labels', async () => {
    const cleanup = vi.fn()
    mountMermaidDiagrams.mockReturnValue(cleanup)
    const source = 'flowchart LR\n A-->B\n'
    const writeText = vi.fn().mockResolvedValue()
    vi.stubGlobal('navigator', { clipboard: { writeText } })
    const wrapper = mount(MarkdownContent, { props: { source: '```mermaid\n' + source + '```' } })
    try {
      await flushPromises()
      expect(mountMermaidDiagrams).toHaveBeenCalledOnce()
      await wrapper.get('[data-copy-code]').trigger('click')
      expect(writeText).toHaveBeenCalledWith(source)
      await wrapper.setProps({ source: '## 已删除图表' })
      await flushPromises()
      expect(cleanup).toHaveBeenCalledOnce()
    } finally { wrapper.unmount(); vi.unstubAllGlobals() }
  })

  it('copies the original code text without HTML markup or toolbar labels', async () => {
    const writeText = vi.fn().mockResolvedValue()
    vi.stubGlobal('navigator', { clipboard: { writeText } })
    const wrapper = mount(MarkdownContent, { props: { source: '```yaml\non:\n  push: true\n```' } })
    try {
      await wrapper.get('[data-copy-code]').trigger('click')
      expect(writeText).toHaveBeenCalledWith('on:\n  push: true\n')
      expect(wrapper.get('[data-copy-code]').text()).toBe('已复制')
    } finally { wrapper.unmount(); vi.unstubAllGlobals() }
  })

  it('keeps chart nodes and the same controller across ordinary Markdown input', async () => {
    const cleanup = Object.assign(vi.fn(), { sync: vi.fn() })
    mountMermaidDiagrams.mockReturnValue(cleanup)
    const source = '```mermaid\nflowchart LR\n A-->B\n```\n\n'
    const wrapper = mount(MarkdownContent, { props: { source: source + '原文' } })
    try {
      await flushPromises()
      const chart = wrapper.get('[data-mermaid-block]').element
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
      chart.querySelector('[data-mermaid-canvas]').append(svg)
      for (const text of ['修改', '修改正文', '修改正文完成']) {
        await wrapper.setProps({ source: source + text })
        await flushPromises()
        expect(wrapper.get('[data-mermaid-block]').element).toBe(chart)
        expect(chart.querySelector('svg')).toBe(svg)
      }
      expect(mountMermaidDiagrams).toHaveBeenCalledOnce()
      expect(cleanup).not.toHaveBeenCalled()
      expect(cleanup.sync).toHaveBeenCalledTimes(3)
    } finally { wrapper.unmount() }
    expect(cleanup).toHaveBeenCalledOnce()
  })
})
