import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { beforeEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { pinia } from '@/stores'
import { useUserStore } from '@/stores/user'
import EntryView from '../index.vue'

const source = readFileSync(resolve(process.cwd(), 'src/views/entry/index.vue'), 'utf8')

describe('identity entry scroll', () => {
  it('owns vertical overflow because the global body is locked', () => {
    expect(source).toMatch(/\.entry-page\s*\{[^}]*overflow-y:\s*auto/s)
    expect(source).toMatch(/\.entry-page\s*\{[^}]*height:\s*100dvh/s)
  })
})

describe('identity entry navigation', () => {
  const routes = [
    '/',
    '/login/freshman',
    '/login/member',
    '/admin/login',
    '/freshman/home',
    '/member/home',
    '/admin/dashboard',
  ].map((path) => ({ path, component: { template: '<div />' } }))

  let router
  let userStore

  async function mountEntry() {
    router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/')
    await router.isReady()
    return mount(EntryView, { global: { plugins: [pinia, router] } })
  }

  async function clickEntry(wrapper, href) {
    await wrapper.find(`a[href="${href}"]`).trigger('click', { button: 0 })
    await flushPromises()
  }

  beforeEach(() => {
    userStore = useUserStore(pinia)
    userStore.reset()
  })

  it('keeps the login page for a visitor who is not signed in', async () => {
    const wrapper = await mountEntry()
    await clickEntry(wrapper, '/login/member')
    expect(router.currentRoute.value.path).toBe('/login/member')
    wrapper.unmount()
  })

  it('takes a signed-in member straight into the portal without a login detour', async () => {
    userStore.token = 'member-token'
    userStore.user = { userName: 'member', isQuantaMember: '1' }

    const wrapper = await mountEntry()
    await clickEntry(wrapper, '/login/member')

    // 一次直达：不再先导航到登录页再被守卫重定向，因此不会出现闪跳
    expect(router.currentRoute.value.path).toBe('/member/home')
    wrapper.unmount()
  })

  it('stops a signed-in member at admin login without destroying the session', async () => {
    userStore.token = 'member-token'
    userStore.user = { userName: 'member', isQuantaMember: '1' }
    userStore.roles = ['qt_member']

    const wrapper = await mountEntry()
    await clickEntry(wrapper, '/admin/login')

    expect(router.currentRoute.value.path).toBe('/admin/login')
    // 关键回归点：打开管理入口只表示"看看这个门"，不得清空当前会话
    expect(userStore.token).toBe('member-token')
    expect(userStore.user).not.toBeNull()
    wrapper.unmount()
  })
})
