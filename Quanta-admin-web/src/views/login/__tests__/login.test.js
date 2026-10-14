import { describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import LoginView from '../index.vue'
import { useUserStore } from '@/stores/user'

async function mountLogin(redirect = '') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>身份选择</div>' } },
      { path: '/admin/login', component: LoginView },
      { path: '/admin/dashboard', component: { template: '<div>dashboard</div>' } },
      { path: '/admin/recruitment', component: { template: '<div>recruitment</div>' } },
    ],
  })
  await router.push({ path: '/admin/login', query: redirect ? { redirect } : {} })
  await router.isReady()
  const wrapper = mount(LoginView, {
    global: { plugins: [pinia, router, ElementPlus] },
  })
  return { wrapper, router, userStore: useUserStore() }
}

describe('login view', () => {
  it('shows the Quanta brand and real backend development account', async () => {
    const { wrapper } = await mountLogin()

    expect(wrapper.get('img[alt="Quanta 社团 Logo"]').attributes('src')).toContain('quanta-logo.jpg')
    expect(wrapper.text()).toContain('真实后端')
    expect(wrapper.text()).toContain('admin / admin123')
    expect(wrapper.text()).not.toContain('viewer / quanta123')
  })

  it('matches the portal brand and return-link presentation', async () => {
    const { wrapper } = await mountLogin()
    const back = wrapper.get('[data-testid="back-to-identity"]')

    expect(wrapper.get('.login-page__brand small').text()).toBe('DIGITAL CLUB')
    expect(wrapper.get('.login-card__mobile-brand small').text()).toBe('DIGITAL CLUB')
    expect(back.text()).toBe('← 返回身份选择')
    expect(back.element.tagName).toBe('A')
    expect(back.attributes('href')).toBe('/')
  })

  it('does not submit an empty form', async () => {
    const { wrapper, userStore } = await mountLogin()
    const loginSpy = vi.spyOn(userStore, 'login')

    await wrapper.find('.login-card__submit').trigger('click')
    await flushPromises()

    expect(loginSpy).not.toHaveBeenCalled()
  })

  it('returns to the identity selector from the admin login page', async () => {
    const { wrapper, router } = await mountLogin()

    await wrapper.get('[data-testid="back-to-identity"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('logs in and enters the dashboard', async () => {
    const { wrapper, router, userStore } = await mountLogin()
    vi.spyOn(userStore, 'login').mockResolvedValue({ code: 200, token: 'test-token' })

    await wrapper.find('input[autocomplete="username"]').setValue('admin')
    await wrapper.find('input[autocomplete="current-password"]').setValue('admin123')
    await wrapper.find('.login-card__submit').trigger('click')
    await flushPromises()

    expect(userStore.login).toHaveBeenCalledOnce()
    expect(userStore.login).toHaveBeenCalledWith(expect.objectContaining({ username: 'admin' }), 'admin')
    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('ignores an identity-page redirect after admin login', async () => {
    const { wrapper, router, userStore } = await mountLogin('/')
    vi.spyOn(userStore, 'login').mockResolvedValue({ code: 200, token: 'test-token' })

    await wrapper.find('input[autocomplete="username"]').setValue('admin')
    await wrapper.find('input[autocomplete="current-password"]').setValue('admin123')
    await wrapper.find('.login-card__submit').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('keeps a valid management-page redirect after admin login', async () => {
    const { wrapper, router, userStore } = await mountLogin('/admin/recruitment')
    vi.spyOn(userStore, 'login').mockResolvedValue({ code: 200, token: 'test-token' })

    await wrapper.find('input[autocomplete="username"]').setValue('admin')
    await wrapper.find('input[autocomplete="current-password"]').setValue('admin123')
    await wrapper.find('.login-card__submit').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/admin/recruitment')
  })
})
