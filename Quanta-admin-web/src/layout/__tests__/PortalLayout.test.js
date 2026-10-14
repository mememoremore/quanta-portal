import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { mount } from '@vue/test-utils'
import PortalLayout from '../PortalLayout.vue'
import { useUserStore } from '@/stores/user'

async function mountLayout(audience = 'freshman') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const prefix = audience === 'freshman' ? '/freshman' : '/member'
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{
      path: `${prefix}/home`,
      component: { template: '<div data-testid="page-content">页面内容</div>' },
      meta: { portalAudience: audience },
    }, { path: '/', component: { template: '<div />' } }],
  })
  await router.push(`${prefix}/home`)
  await router.isReady()
  const store = useUserStore()
  store.user = { nickName: audience === 'freshman' ? '新生小李' : '塔员小王' }
  const wrapper = mount(PortalLayout, { global: { plugins: [pinia, router] } })
  return { wrapper, router, store }
}

describe('PortalLayout', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => document.body.classList.remove('portal-scroll-active'))

  it('enables document scrolling only while the portal layout is mounted', async () => {
    const { wrapper } = await mountLayout('freshman')

    expect(document.body.classList.contains('portal-scroll-active')).toBe(true)
    wrapper.unmount()
    expect(document.body.classList.contains('portal-scroll-active')).toBe(false)
  })

  it('renders a branded desktop header and the freshman navigation', async () => {
    const { wrapper } = await mountLayout('freshman')

    expect(wrapper.get('[data-testid="portal-desktop-nav"]').text()).toContain('加入我们')
    expect(wrapper.get('[data-testid="portal-desktop-nav"]').text()).not.toContain('通讯录')
    expect(wrapper.get('img[alt="Quanta 社团 Logo"]')).toBeTruthy()
    expect(wrapper.get('[data-testid="page-content"]').text()).toBe('页面内容')
  })

  it('renders member links from the server-approved portal audience', async () => {
    const { wrapper } = await mountLayout('member')

    expect(wrapper.get('[data-testid="portal-desktop-nav"]').text()).toContain('通讯录')
    expect(wrapper.text()).toContain('塔员小王')
  })

  it('exposes an accessible mobile menu trigger', async () => {
    const { wrapper } = await mountLayout('freshman')
    const trigger = wrapper.get('[data-testid="portal-mobile-menu"]')

    expect(trigger.attributes('aria-expanded')).toBe('false')
    expect(trigger.attributes('aria-label')).toBe('打开导航菜单')
    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(trigger.attributes('aria-label')).toBe('关闭导航菜单')
    expect(wrapper.get('[data-testid="portal-mobile-panel"]').text()).toContain('新生首页')
  })

  it('provides an accessible account menu', async () => {
    const { wrapper } = await mountLayout('member')
    const trigger = wrapper.get('[aria-haspopup="menu"]')

    expect(trigger.attributes('aria-expanded')).toBe('false')
    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(wrapper.get('[role="menu"]').text()).toContain('账号与安全')
    expect(wrapper.get('[role="menu"]').text()).toContain('退出登录')
  })
})
