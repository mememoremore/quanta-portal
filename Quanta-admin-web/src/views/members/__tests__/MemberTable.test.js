import { describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import MemberTable from '../components/MemberTable.vue'
import { useUserStore } from '@/stores/user'

const row = {
  id: 1,
  name: '测试成员',
  department: '研发部',
  title: '经理',
  cohort: '第21届',
  phoneMasked: '138****0000',
  joinedAt: '2024-09-01',
  status: 'active',
  canRetain: false,
}

function mountTable(permissions, roles = []) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useUserStore().permissions = permissions
  useUserStore().roles = roles
  return mount(MemberTable, {
    props: { rows: [row], isCurrentCohort: true },
    global: {
      plugins: [pinia, ElementPlus],
      stubs: {
        ElTable: { template: '<div><slot /></div>' },
        ElTableColumn: {
          data: () => ({ testRow: row }),
          template: '<div><slot name="header" /><slot :row="testRow" /></div>',
        },
      },
    },
  })
}

describe('MemberTable', () => {
  it('shows full member details on mobile and forwards the permitted action', async () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() })))
    let wrapper
    try {
      wrapper = mountTable(['system:user:remove', 'system:user:resetPwd'], ['ceo'])
      await wrapper.vm.$nextTick()
      const card = wrapper.get('.member-mobile__card')
      for (const text of ['测试成员', '研发部', '经理', '第21届', '138****0000', '2024-09-01', '在职']) {
        expect(card.text()).toContain(text)
      }
      expect(wrapper.find('.member-table').exists()).toBe(false)
      await card.get('.member-table__reset').trigger('click')
      expect(wrapper.emitted('reset-password')[0]).toEqual([row])
    } finally {
      wrapper?.unmount()
      vi.unstubAllGlobals()
    }
  })

  it('shows current-cohort high-risk actions to the CEO', () => {
    const wrapper = mountTable(['system:user:remove', 'system:user:resetPwd'], ['ceo'])
    expect(wrapper.text()).toContain('删除')
    expect(wrapper.text()).toContain('重置密码')
  })

  it('treats the RuoYi administrator as CEO for high-risk actions', () => {
    const wrapper = mountTable(['*:*:*'], ['admin'])
    expect(wrapper.text()).toContain('删除')
    expect(wrapper.text()).toContain('重置密码')
  })

  it('hides write actions from read-only users', () => {
    const wrapper = mountTable(['qt:member:list'])
    expect(wrapper.text()).not.toContain('删除')
    expect(wrapper.text()).not.toContain('重置密码')
  })

  it('labels members without a cohort as unassigned instead of 第届', async () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() })))
    let wrapper
    try {
      const pinia = createPinia()
      setActivePinia(pinia)
      useUserStore().permissions = ['qt:member:list']
      wrapper = mount(MemberTable, {
        props: { rows: [{ ...row, cohort: '' }], isCurrentCohort: false },
        global: {
          plugins: [pinia, ElementPlus],
          stubs: {
            ElTable: { template: '<div><slot /></div>' },
            ElTableColumn: {
              data: () => ({ testRow: { ...row, cohort: '' } }),
              template: '<div><slot name="header" /><slot :row="testRow" /></div>',
            },
          },
        },
      })
      await wrapper.vm.$nextTick()
      expect(wrapper.get('.member-mobile__card').text()).toContain('未分届')
      expect(wrapper.text()).not.toContain('第届')
    } finally {
      wrapper?.unmount()
      vi.unstubAllGlobals()
    }
  })

  it('renders retention as a compact filled action for historical cohorts', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useUserStore().permissions = ['qt:member:retain']
    const wrapper = mount(MemberTable, {
      props: {
        rows: [{ ...row, cohort: '20', status: 'retention_pending', canRetain: true }],
        isCurrentCohort: false,
      },
      global: {
        plugins: [pinia, ElementPlus],
        stubs: {
          ElTable: { template: '<div><slot /></div>' },
          ElTableColumn: {
            data: () => ({ testRow: { ...row, canRetain: true } }),
            template: '<div><slot name="header" /><slot :row="testRow" /></div>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('是否留任')
    expect(wrapper.find('.member-table__retain').classes()).toContain('member-table__action')
  })
})
