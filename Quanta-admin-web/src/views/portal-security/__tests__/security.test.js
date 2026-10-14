import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword, sendFreshmanEmailCode, updateFreshmanEmail } from '@/api/portal/account'
import PortalSecurity from '../index.vue'
import { useUserStore } from '@/stores/user'

vi.mock('@/api/portal/account', () => ({
  changePassword: vi.fn(),
  sendFreshmanEmailCode: vi.fn(),
  updateFreshmanEmail: vi.fn(),
}))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), error: vi.fn() } }))

async function mountSecurity(audience = 'freshman') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/security', component: PortalSecurity, meta: { portalAudience: audience } }],
  })
  await router.push('/security')
  await router.isReady()
  const store = useUserStore()
  store.user = { email: 'old@example.com', isQuantaMember: audience === 'freshman' ? '0' : '1' }
  const wrapper = mount(PortalSecurity, { global: { plugins: [pinia, router] } })
  return { wrapper, store }
}

describe('portal account security', () => {
  beforeEach(() => vi.clearAllMocks())

  it('centers the member password card and hides the native password reveal control', () => {
    const css = readFileSync(join(process.cwd(), 'src/views/portal-security/security.css'), 'utf8')
    expect(css).toContain('.portal-security__grid:not(.has-email-card)')
    expect(css).toContain('justify-content: center')
    expect(css).toContain('::-ms-reveal')
  })

  it('validates confirmation before calling the backend', async () => {
    const { wrapper } = await mountSecurity()
    await wrapper.get('input[name="oldPassword"]').setValue('old-secret')
    await wrapper.get('input[name="newPassword"]').setValue('new-secret')
    await wrapper.get('input[name="confirmPassword"]').setValue('different')
    await wrapper.get('form').trigger('submit')

    expect(changePassword).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('两次输入的新密码不一致')
  })

  it('reports success only after the real request succeeds', async () => {
    changePassword.mockResolvedValue({ code: 200 })
    const { wrapper } = await mountSecurity()
    await wrapper.get('input[name="oldPassword"]').setValue('old-secret')
    await wrapper.get('input[name="newPassword"]').setValue('new-secret')
    await wrapper.get('input[name="confirmPassword"]').setValue('new-secret')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(changePassword).toHaveBeenCalledWith({ oldPassword: 'old-secret', newPassword: 'new-secret' })
    expect(ElMessage.success).toHaveBeenCalledWith('密码修改成功')
    expect(wrapper.get('input[name="oldPassword"]').element.value).toBe('')
  })

  it('keeps failure visible and never reports success', async () => {
    changePassword.mockRejectedValue(new Error('当前密码错误'))
    const { wrapper } = await mountSecurity()
    await wrapper.get('input[name="oldPassword"]').setValue('wrong-old')
    await wrapper.get('input[name="newPassword"]').setValue('new-secret')
    await wrapper.get('input[name="confirmPassword"]').setValue('new-secret')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('当前密码错误')
    expect(ElMessage.success).not.toHaveBeenCalled()
  })

  it('shows email management only to freshmen', async () => {
    const freshman = await mountSecurity('freshman')
    expect(freshman.wrapper.find('[data-testid="email-card"]').exists()).toBe(true)
    expect(freshman.wrapper.get('input[name="email"]').element.value).toBe('old@example.com')

    const member = await mountSecurity('member')
    expect(member.wrapper.find('[data-testid="email-card"]').exists()).toBe(false)
  })

  it('toggles visibility for all three password fields', async () => {
    const { wrapper } = await mountSecurity('freshman')
    for (const [buttonId, inputName] of [
      ['toggle-old-password', 'oldPassword'],
      ['toggle-new-password', 'newPassword'],
      ['toggle-confirm-password', 'confirmPassword'],
    ]) {
      const input = wrapper.get(`input[name="${inputName}"]`)
      expect(input.attributes('type')).toBe('password')
      await wrapper.get(`[data-testid="${buttonId}"]`).trigger('click')
      expect(input.attributes('type')).toBe('text')
    }
  })

  it('locks new-email code sending for 60 seconds', async () => {
    vi.useFakeTimers()
    let wrapper
    try {
      sendFreshmanEmailCode.mockResolvedValue({ code: 200 })
      ;({ wrapper } = await mountSecurity('freshman'))
      await wrapper.get('input[name="email"]').setValue('new@example.com')
      await wrapper.get('[data-testid="send-new-email-code"]').trigger('click')
      await flushPromises()

      expect(sendFreshmanEmailCode).toHaveBeenCalledWith('new@example.com')
      expect(wrapper.get('[data-testid="send-new-email-code"]').attributes('disabled')).toBeDefined()
      expect(wrapper.get('[data-testid="send-new-email-code"]').text()).toContain('60 秒后重新发送')

      vi.advanceTimersByTime(60_000)
      await wrapper.vm.$nextTick()
      expect(wrapper.get('[data-testid="send-new-email-code"]').attributes('disabled')).toBeUndefined()
    } finally {
      wrapper?.unmount()
      vi.useRealTimers()
    }
  })

  it('requires a six-digit code before updating the freshman email', async () => {
    const { wrapper } = await mountSecurity('freshman')
    await wrapper.get('input[name="email"]').setValue('new@example.com')
    await wrapper.get('input[name="emailCode"]').setValue('12345')
    await wrapper.get('[data-testid="email-card"]').trigger('submit')
    await flushPromises()

    expect(updateFreshmanEmail).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入6位邮箱验证码')
  })

  it('updates a verified freshman email through the real API adapter', async () => {
    sendFreshmanEmailCode.mockResolvedValue({ code: 200 })
    updateFreshmanEmail.mockResolvedValue({ code: 200 })
    const { wrapper, store } = await mountSecurity('freshman')
    vi.spyOn(store, 'fetchUserInfo').mockResolvedValue({})
    await wrapper.get('input[name="email"]').setValue('new@example.com')
    await wrapper.get('[data-testid="send-new-email-code"]').trigger('click')
    await flushPromises()
    await wrapper.get('input[name="emailCode"]').setValue('123456')
    await wrapper.get('[data-testid="email-card"]').trigger('submit')
    await flushPromises()

    expect(updateFreshmanEmail).toHaveBeenCalledWith({ email: 'new@example.com', emailCode: '123456' })
    expect(store.fetchUserInfo).toHaveBeenCalledOnce()
    expect(ElMessage.success).toHaveBeenCalledWith('邮箱修改成功')
  })
})
