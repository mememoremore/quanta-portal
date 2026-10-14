import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import RecruitmentBoard from '../components/RecruitmentBoard.vue'

describe('RecruitmentBoard', () => {
  it('renders the four unique-candidate statistics', () => {
    const wrapper = mount(RecruitmentBoard, {
      props: { statistics: { submissions: 8, firstPassed: 5, secondPassed: 2, joined: 1 } },
      global: { plugins: [ElementPlus] },
    })

    expect(wrapper.text()).toContain('投递总数')
    expect(wrapper.text()).toContain('一面通过（进入二面）')
    expect(wrapper.text()).toContain('二面通过（发 offer）')
    expect(wrapper.text()).toContain('确认加入')
    expect(wrapper.text()).toContain('8')
  })
})
