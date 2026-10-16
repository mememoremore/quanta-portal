import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownContent from '../MarkdownContent.vue'

describe('Markdown code copying', () => {
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
