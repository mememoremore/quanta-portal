import { beforeEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import SharingSignupsView from '../index.vue'
import { setToken } from '@/utils/token'

describe('sharing signups view', () => {
  beforeEach(() => setToken('mock-token-product-manager'))

  it('renders the registration count, quota, and read-only table', async () => {
    const wrapper = mount(SharingSignupsView, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    expect(wrapper.text()).toContain('精英分享会报名名单')
    expect(wrapper.text()).toContain('10 / 50')
    expect(wrapper.text()).toContain('陈佳琪')
    expect(wrapper.text()).not.toContain('导出')
  })
})
