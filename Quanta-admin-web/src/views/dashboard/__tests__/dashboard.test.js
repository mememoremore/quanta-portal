import { describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import DashboardView from '../index.vue'
import { setToken } from '@/utils/token'
import { Document } from '@element-plus/icons-vue'
import StatCard from '../components/StatCard.vue'

describe('dashboard view', () => {
  it.each([[0, '0'], [1234, '1234'], [null, '--']])('preserves the supplied metric %s with decorative icons hidden from assistive technology', (value, expected) => {
    const wrapper = mount(StatCard, { props: { label: '今日新增简历', value, icon: Document }, global: { plugins: [ElementPlus] } })
    expect(wrapper.get('strong').text()).toBe(expected)
    expect(wrapper.get('p').text()).toBe('今日新增简历')
    expect(wrapper.get('.stat-card__icon').attributes('aria-hidden')).toBe('true')
    wrapper.unmount()
  })

  it('renders the three active mock statistics without retired reservations', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    setToken('mock-token-admin')

    const wrapper = mount(DashboardView, {
      global: { plugins: [pinia, ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('当届成员总数')
    expect(wrapper.text()).toContain('今日新增简历')
    expect(wrapper.text()).not.toContain('当前待生效预约')
    expect(wrapper.text()).toContain('待审核塔服缴费')
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('2')
  })
})

