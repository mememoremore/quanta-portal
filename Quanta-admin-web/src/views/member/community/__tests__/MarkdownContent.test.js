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
})
