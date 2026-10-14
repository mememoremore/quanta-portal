import { describe, expect, it } from 'vitest'
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
  cohort: '21',
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
