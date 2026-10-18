import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MaterialFileIcon from '../MaterialFileIcon.vue'

describe('material document icons', () => {
  it.each([
    ['资料.PDF', 'pdf', ''], ['说明.doc', 'word', 'W'], ['说明.docx', 'word', 'W'],
    ['笔记.md', 'markdown', 'M'], ['课件.pptx', 'slides', 'P'], ['数据.xlsx', 'sheet', 'X'],
    ['截图.png', 'image', ''], ['readme.txt', 'text', ''], ['README', 'generic', ''], ['文件.unknown', 'generic', ''],
    ['', 'generic', ''], [null, 'generic', ''], ['文件.__proto__', 'generic', ''],
  ])('renders a local document graphic for %s', (name, kind, letter) => {
    const wrapper = mount(MaterialFileIcon, { props: { name } })
    expect(wrapper.attributes('data-kind')).toBe(kind)
    expect(wrapper.attributes('aria-hidden')).toBe('true')
    expect(wrapper.find('svg').exists()).toBe(true)
    expect(wrapper.find('svg path').exists()).toBe(true)
    expect(wrapper.find('img').exists()).toBe(false)
    if (letter) expect(wrapper.get('svg text').text()).toBe(letter)
    wrapper.unmount()
  })

  it('updates when the queued filename changes', async () => {
    const wrapper = mount(MaterialFileIcon, { props: { name: '资料.pdf' } })
    await wrapper.setProps({ name: '资料.docx' })
    expect(wrapper.attributes('data-kind')).toBe('word')
    expect(wrapper.get('svg text').text()).toBe('W')
    wrapper.unmount()
  })
})
