import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { flushPromises, mount } from '@vue/test-utils'
import Page from '../index.vue'
import { useUserStore } from '@/stores/user'
import * as api from '@/api/oauth'
import * as oauth from '@/utils/oauth'

vi.mock('@/api/oauth', () => ({ getInteraction: vi.fn(), getOAuthIdentity: vi.fn(), decideInteraction: vi.fn() }))
vi.mock('@/utils/request', () => ({ isDualTokenEnabled: () => false, request: vi.fn(), refreshSession: vi.fn() }))
const id = 'a'.repeat(43)
const value = () => ({ interactionId: id, status: 'pending', expiresAt: new Date(Date.now() + 600000).toISOString(), csrfToken: 'c'.repeat(43), scopes: ['openid', 'profile'], consentRequired: true, client: { clientId: 'quanta-challenge', name: 'Quanta Challenge', origin: 'https://challenge.quantacenter.com', redirectUri: 'https://challenge.quantacenter.com/api/auth/quanta/callback' } })
const wrappers = []
async function setup(user = { userId: 1, nickName: '实名同学', communityNickname: '论坛昵称', isQuantaMember: '1' }) {
  const pinia = createPinia(); setActivePinia(pinia)
  const store = useUserStore(); store.token = user ? 'token' : ''
  api.getOAuthIdentity.mockResolvedValue({ user })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/oauth/authorize', component: Page }] })
  await router.push(`/oauth/authorize?interaction=${id}`)
  const wrapper = mount(Page, { global: { plugins: [pinia, router], stubs: { LoginBrand: true, MemberAvatar: true, MemberLogin: { template: '<div>塔员登录表单</div>' } } } })
  wrappers.push(wrapper); await flushPromises()
  return wrapper
}
beforeEach(() => {
  vi.clearAllMocks()
  vi.spyOn(oauth, 'navigateOAuthContinuation').mockImplementation(() => {})
  api.getInteraction.mockResolvedValue(value())
})
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()); vi.useRealTimers() })

describe('Quanta OAuth page', () => {
  it('shows real-name consent without community nickname', async () => {
    const wrapper = await setup()
    expect(wrapper.text()).toContain('实名同学')
    expect(wrapper.text()).not.toContain('论坛昵称')
    expect(api.decideInteraction).not.toHaveBeenCalled()
  })
  it('prefers the canonical realName field introduced by the latest portal backend', async () => {
    const wrapper = await setup({ userId: 1, realName: '成员实名', nickName: '旧显示名称', communityNickname: '社区昵称', isQuantaMember: '1' })
    expect(wrapper.text()).toContain('成员实名')
    expect(wrapper.text()).not.toContain('旧显示名称')
    expect(wrapper.text()).not.toContain('社区昵称')
  })
  it('keeps unauthenticated users in the neutral transaction', async () => {
    const wrapper = await setup(null)
    expect(wrapper.text()).toContain('塔员登录表单')
    expect(api.getOAuthIdentity).not.toHaveBeenCalled()
  })
  it('does not infer membership from admin roles', async () => {
    const wrapper = await setup({ userId: 1, roles: ['ceo'], nickName: '管理员' })
    expect(wrapper.text()).toContain('该登录方式仅向塔员开放')
    expect(wrapper.find('[data-testid="oauth-approve"]').exists()).toBe(false)
  })
  it('continues immediately after approval without rendering a success card or countdown', async () => {
    api.decideInteraction.mockResolvedValue({ status: 'approved', continuePath: `/oauth2/interaction/${id}/continue`, expiresAt: value().expiresAt })
    const wrapper = await setup()
    await wrapper.get('[data-testid="oauth-approve"]').trigger('click'); await flushPromises()
    expect(api.decideInteraction).toHaveBeenCalledExactlyOnceWith(id, 'approve', 'c'.repeat(43))
    expect(oauth.navigateOAuthContinuation).toHaveBeenCalledExactlyOnceWith(id)
    expect(wrapper.text()).not.toContain('登录成功，欢迎回来')
    expect(wrapper.find('[data-testid="oauth-return"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="oauth-countdown"]').exists()).toBe(false)
  })
  it('rejects unsafe approval responses instead of showing success', async () => {
    api.decideInteraction.mockResolvedValue({ status: 'approved', continuePath: 'https://evil.test', expiresAt: value().expiresAt })
    const wrapper = await setup()
    await wrapper.get('[data-testid="oauth-approve"]').trigger('click'); await flushPromises()
    expect(wrapper.text()).toContain('授权结果无效')
    expect(wrapper.find('[data-testid="oauth-return"]').exists()).toBe(false)
  })
  it('never asks for consent on an expired interaction', async () => {
    api.getInteraction.mockResolvedValue({ ...value(), expiresAt: new Date(0).toISOString() })
    const wrapper = await setup()
    expect(wrapper.text()).toContain('本次授权已过期')
    expect(api.getOAuthIdentity).not.toHaveBeenCalled()
  })
  it('handles unavailable backend without using a demo success', async () => {
    api.getInteraction.mockRejectedValue(new Error('授权服务尚未就绪'))
    const wrapper = await setup()
    expect(wrapper.text()).toContain('授权服务尚未就绪')
    expect(wrapper.find('[data-testid="oauth-approve"]').exists()).toBe(false)
  })

  it('continues exactly once without advancing any timers, never directly to the homepage', async () => {
    vi.useFakeTimers()
    const navigate = vi.spyOn(oauth, 'navigateOAuthContinuation').mockImplementation(() => {})
    api.decideInteraction.mockResolvedValue({ status: 'approved', continuePath: `/oauth2/interaction/${id}/continue`, expiresAt: value().expiresAt })
    const wrapper = await setup()
    await wrapper.get('[data-testid="oauth-approve"]').trigger('click'); await flushPromises()
    expect(navigate).toHaveBeenCalledExactlyOnceWith(id)
    await vi.advanceTimersByTimeAsync(10000)
    expect(navigate).toHaveBeenCalledOnce()
  })

  it('never continues with an expired approval response', async () => {
    vi.useFakeTimers()
    const navigate = vi.spyOn(oauth, 'navigateOAuthContinuation').mockImplementation(() => {})
    api.decideInteraction.mockResolvedValue({ status: 'approved', continuePath: `/oauth2/interaction/${id}/continue`, expiresAt: new Date(Date.now() - 1).toISOString() })
    const wrapper = await setup()
    await wrapper.get('[data-testid="oauth-approve"]').trigger('click'); await flushPromises()
    expect(navigate).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('本次授权已过期')
  })
  it('ignores an approval that resolves after unmount', async () => {
    let resolve
    api.decideInteraction.mockReturnValue(new Promise(done => { resolve = done }))
    const wrapper = await setup()
    await wrapper.get('[data-testid="oauth-approve"]').trigger('click')
    wrapper.unmount()
    resolve({ status: 'approved', continuePath: `/oauth2/interaction/${id}/continue`, expiresAt: value().expiresAt })
    await flushPromises()
    expect(oauth.navigateOAuthContinuation).not.toHaveBeenCalled()
  })
  it('continues an already-approved transaction immediately after checking the same account', async () => {
    api.getInteraction.mockResolvedValue({ ...value(), status: 'approved', subjectUserId: 1 })
    await setup()
    expect(oauth.navigateOAuthContinuation).toHaveBeenCalledExactlyOnceWith(id)
    expect(api.decideInteraction).not.toHaveBeenCalled()
  })
  it('does not continue an approved transaction belonging to a different account', async () => {
    api.getInteraction.mockResolvedValue({ ...value(), status: 'approved', subjectUserId: 2 })
    const wrapper = await setup()
    expect(wrapper.text()).toContain('授权账号已变化')
    expect(oauth.navigateOAuthContinuation).not.toHaveBeenCalled()
  })
  it('ignores an approval that resolves after the interaction changes', async () => {
    const navigate = vi.spyOn(oauth, 'navigateOAuthContinuation').mockImplementation(() => {})
    let resolve
    api.decideInteraction.mockReturnValue(new Promise(done => { resolve = done }))
    const wrapper = await setup()
    await wrapper.get('[data-testid="oauth-approve"]').trigger('click')
    api.getInteraction.mockRejectedValue(new Error('新授权已失效'))
    await wrapper.vm.$router.push(`/oauth/authorize?interaction=${'b'.repeat(43)}`)
    await flushPromises()
    resolve({ status: 'approved', continuePath: `/oauth2/interaction/${id}/continue`, expiresAt: value().expiresAt })
    await flushPromises()
    expect(navigate).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('新授权已失效')
  })
})
