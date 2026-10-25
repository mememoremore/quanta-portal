import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import PortalLogin from '../index.vue'
import { useUserStore } from '@/stores/user'
import * as authApi from '@/api/auth'

vi.mock('@/api/auth', () => ({
  getCaptcha: vi.fn().mockResolvedValue({ captchaEnabled: false }),
  registerFreshman: vi.fn(),
  sendRegisterEmailCode: vi.fn().mockResolvedValue({ code: 200 }),
  login: vi.fn(),
  getInfo: vi.fn(),
  getRouters: vi.fn(),
  logout: vi.fn(),
}))

async function mountLogin(audience = 'freshman', redirect = '') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const path = audience === 'freshman' ? '/login/freshman' : '/login/member'
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>身份选择</div>' } },
      { path, component: PortalLogin, meta: { portalAudience: audience } },
      { path: '/freshman/home', component: { template: '<div />' } },
      { path: '/member/home', component: { template: '<div />' } },
    ],
  })
  await router.push({ path, query: redirect ? { redirect } : {} })
  await router.isReady()
  const wrapper = mount(PortalLogin, { global: { plugins: [pinia, router, ElementPlus] } })
  return { wrapper, router, store: useUserStore() }
}

describe('portal login', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authApi.getCaptcha.mockResolvedValue({ captchaEnabled: false })
  })

  it.each(['freshman', 'member'])('uses the shared brand and return link for %s', async (audience) => {
    const { wrapper } = await mountLogin(audience)
    expect(wrapper.get('.portal-login__brand small').text()).toBe('DIGITAL CLUB')
    expect(wrapper.get('.portal-login__brand').attributes('href')).toBe('/')
    expect(wrapper.get('.portal-login__back').text()).toBe('← 返回身份选择')
    expect(wrapper.get('.portal-login__back').attributes('href')).toBe('/')
  })

  it('labels the freshman username as student number and exposes registration only there', async () => {
    const freshman = await mountLogin('freshman')
    expect(freshman.wrapper.text()).toContain('学号')
    expect(freshman.wrapper.get('input[autocomplete="username"]').attributes('placeholder')).toBe('请输入学号')
    expect(freshman.wrapper.find('[data-testid="register-mode"]').exists()).toBe(true)

    const member = await mountLogin('member')
    expect(member.wrapper.text()).toContain('用户名')
    expect(member.wrapper.find('[data-testid="register-mode"]').exists()).toBe(false)
  })

  it('validates freshman registration fields before calling the backend', async () => {
    const { wrapper } = await mountLogin('freshman')
    await wrapper.get('[data-testid="register-mode"]').trigger('click')
    await wrapper.get('input[name="studentNo"]').setValue('2024100319')
    await wrapper.get('input[name="email"]').setValue('bad-email')
    await wrapper.get('input[name="registerPassword"]').setValue('secret123')
    await wrapper.get('input[name="confirmPassword"]').setValue('different')
    await wrapper.get('[data-testid="register-submit"]').trigger('click')
    await flushPromises()

    expect(authApi.registerFreshman).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入11位学号')
    expect(wrapper.text()).toContain('请输入正确的邮箱')
    expect(wrapper.text()).toContain('两次输入的密码不一致')
  })

  it('locks email code sending for 60 seconds after validating the email', async () => {
    vi.useFakeTimers()
    let wrapper
    try {
      ;({ wrapper } = await mountLogin('freshman'))
      await wrapper.get('[data-testid="register-mode"]').trigger('click')
      await wrapper.get('input[name="studentNo"]').setValue('20241003193')
      await wrapper.get('input[name="email"]').setValue('bad-email')
      await wrapper.get('[data-testid="send-email-code"]').trigger('click')
      expect(wrapper.get('[data-testid="send-email-code"]').attributes('disabled')).toBeUndefined()
      expect(wrapper.text()).toContain('请输入正确的邮箱')

      await wrapper.get('input[name="email"]').setValue('freshman@example.com')
      await wrapper.get('[data-testid="send-email-code"]').trigger('click')
      await flushPromises()
      expect(authApi.sendRegisterEmailCode).toHaveBeenCalledWith({
        email: 'freshman@example.com',
        studentNo: '20241003193',
      })
      expect(wrapper.get('[data-testid="send-email-code"]').attributes('disabled')).toBeDefined()
      expect(wrapper.get('[data-testid="send-email-code"]').text()).toContain('60 秒后重新发送')

      vi.advanceTimersByTime(60_000)
      await wrapper.vm.$nextTick()
      expect(wrapper.get('[data-testid="send-email-code"]').attributes('disabled')).toBeUndefined()
      expect(wrapper.get('[data-testid="send-email-code"]').text()).toContain('重新发送')
    } finally {
      wrapper?.unmount()
      vi.useRealTimers()
    }
  })

  it('requires a six-digit email code before registration', async () => {
    const { wrapper } = await mountLogin('freshman')
    await wrapper.get('[data-testid="register-mode"]').trigger('click')
    await wrapper.get('input[name="studentNo"]').setValue('20241003193')
    await wrapper.get('input[name="email"]').setValue('freshman@example.com')
    await wrapper.get('input[name="emailCode"]').setValue('12345')
    await wrapper.get('input[name="registerPassword"]').setValue('secret123')
    await wrapper.get('input[name="confirmPassword"]').setValue('secret123')
    await wrapper.get('[data-testid="register-submit"]').trigger('click')
    await flushPromises()

    expect(authApi.registerFreshman).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入6位邮箱验证码')
  })

  it('registers a freshman then returns to login with the student number filled', async () => {
    authApi.registerFreshman.mockResolvedValue({ code: 200 })
    const { wrapper } = await mountLogin('freshman')
    await wrapper.get('[data-testid="register-mode"]').trigger('click')
    await wrapper.get('input[name="studentNo"]').setValue('20241003193')
    await wrapper.get('input[name="email"]').setValue('freshman@example.com')
    await wrapper.get('input[name="emailCode"]').setValue('123456')
    await wrapper.get('input[name="registerPassword"]').setValue('secret123')
    await wrapper.get('input[name="confirmPassword"]').setValue('secret123')
    await wrapper.get('[data-testid="register-submit"]').trigger('click')
    await flushPromises()

    expect(authApi.registerFreshman).toHaveBeenCalledWith(expect.objectContaining({
      studentNo: '20241003193',
      email: 'freshman@example.com',
      emailCode: '123456',
      password: 'secret' + '123',
    }))
    expect(wrapper.get('input[autocomplete="username"]').element.value).toBe('20241003193')
    expect(wrapper.text()).toContain('新生登录')
  })

  it('prevents duplicate registration submissions', async () => {
    let resolveRegistration
    authApi.registerFreshman.mockReturnValue(new Promise((resolve) => { resolveRegistration = resolve }))
    const { wrapper } = await mountLogin('freshman')
    await wrapper.get('[data-testid="register-mode"]').trigger('click')
    await wrapper.get('input[name="studentNo"]').setValue('20241003193')
    await wrapper.get('input[name="email"]').setValue('freshman@example.com')
    await wrapper.get('input[name="emailCode"]').setValue('123456')
    await wrapper.get('input[name="registerPassword"]').setValue('secret123')
    await wrapper.get('input[name="confirmPassword"]').setValue('secret123')

    const button = wrapper.get('[data-testid="register-submit"]')
    await Promise.all([button.trigger('click'), button.trigger('click')])
    await flushPromises()
    expect(authApi.registerFreshman).toHaveBeenCalledTimes(1)

    resolveRegistration({ code: 200 })
    await flushPromises()
  })

  it('submits a password present only on the input element', async () => {
    const { wrapper, store } = await mountLogin('member')
    const login = vi.spyOn(store, 'login').mockResolvedValue({ token: 'token' })
    vi.spyOn(store, 'fetchUserInfo').mockImplementation(async () => { store.user = { userId: 2, isQuantaMember: '1' } })

    wrapper.get('input[name="username"]').element.value = 'qt_ceo'
    wrapper.get('input[name="password"]').element.value = 'secret-from-dom'
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(login).toHaveBeenCalledWith(
      expect.objectContaining({ username: 'qt_ceo', password: 'secret-from-dom' }),
      'member',
    )
  })

  it('submits the audience selected by the route', async () => {
    const { wrapper, store } = await mountLogin('freshman')
    const login = vi.spyOn(store, 'login').mockResolvedValue({ token: 'token' })
    vi.spyOn(store, 'fetchUserInfo').mockImplementation(async () => { store.user = { userId: 2, isQuantaMember: '0' } })

    await wrapper.get('input[name="username"]').setValue('qt_fresh')
    await wrapper.get('input[name="password"]').setValue('admin123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(login).toHaveBeenCalledWith(expect.objectContaining({ username: 'qt_fresh' }), 'freshman')
  })

  it('rejects an external redirect and enters the audience home', async () => {
    const { wrapper, router, store } = await mountLogin('member', '//evil.example')
    vi.spyOn(store, 'login').mockResolvedValue({ token: 'token' })
    vi.spyOn(store, 'fetchUserInfo').mockImplementation(async () => { store.user = { userId: 2, isQuantaMember: '1' } })

    await wrapper.get('input[name="username"]').setValue('qt_member')
    await wrapper.get('input[name="password"]').setValue('admin123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/member/home')
  })

  it('ignores a restored identity-page redirect on first member login', async () => {
    const { wrapper, router, store } = await mountLogin('member', '/')
    vi.spyOn(store, 'login').mockResolvedValue({ token: 'token' })
    vi.spyOn(store, 'fetchUserInfo').mockImplementation(async () => { store.user = { userId: 2, isQuantaMember: '1' } })

    await wrapper.get('input[name="username"]').setValue('qt_member')
    await wrapper.get('input[name="password"]').setValue('admin123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/member/home')
  })

  it.each([null, {}, { userId: 1, userName: 'admin', isQuantaMember: '0' }])(
    'does not navigate or report success for an unconfirmed/mismatched identity %j',
    async (identity) => {
      const { wrapper, router, store } = await mountLogin('member')
      authApi.login.mockResolvedValueOnce({ token: 'test-admin-token' })
      authApi.getInfo.mockResolvedValueOnce({
        user: identity, roles: ['admin'], permissions: ['*:*:*'],
      })
      await wrapper.get('input[name="username"]').setValue('admin')
      await wrapper.get('input[name="password"]').setValue('demo-password')
      await wrapper.get('form').trigger('submit')
      await flushPromises()

      expect(router.currentRoute.value.path).toBe('/login/member')
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(store.canAccessAdmin).toBe(false)
      wrapper.unmount()
    },
  )
})
