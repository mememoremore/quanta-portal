import { beforeEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import LectureSignupsView from '../index.vue'
import { setToken } from '@/utils/token'

describe('lecture signups view', () => {
  beforeEach(() => setToken('mock-token-ceo'))

  it('renders the quota, signup rows, and export control', async () => {
    const wrapper = mount(LectureSignupsView, { global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(wrapper.text()).toContain('10 / 50')
    expect(wrapper.text()).toContain('周安琪')
    expect(wrapper.text()).toContain('导出名单')
  })
})
