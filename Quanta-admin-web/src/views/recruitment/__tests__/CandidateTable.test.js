import { describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import CandidateTable from '../components/CandidateTable.vue'
import { useUserStore } from '@/stores/user'

const candidate = {
  applicationId: 1,
  name: '测试候选人',
  studentNo: '2026001',
  major: '软件工程',
  className: '2026级1班',
  appliedAt: '2026-08-26 10:00',
  choices: [
    {
      choiceOrder: 1,
      department: 'PRODUCT',
      rounds: {
        1: { status: 'PASS', evaluations: [] },
        2: { status: 'PENDING', advanced: true, evaluations: [] },
      },
    },
    {
      choiceOrder: 2,
      department: 'DESIGN',
      rounds: {
        1: { status: 'OUT', evaluations: [] },
        2: { status: 'PENDING', advanced: false, evaluations: [] },
      },
    },
  ],
}

function mountTable({ roles, permissions, roundId = 1, rows = [candidate], user = { userId: 21, deptCode: 'PRODUCT' } }) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useUserStore()
  store.user = user
  store.roles = roles
  store.permissions = permissions

  return mount(CandidateTable, {
    props: { rows, roundId },
    global: {
      plugins: [pinia, ElementPlus],
      stubs: {
        ElTable: { template: '<div><slot /></div>' },
        ElTableColumn: {
          props: ['label'],
          data: () => ({ testRow: rows[0] }),
          template: '<div><span>{{ label }}</span><slot name="header" /><slot :row="testRow" /></div>',
        },
      },
    },
  })
}

describe('CandidateTable', () => {
  it('marks Pass and Out statuses as plain text results', () => {
    const wrapper = mountTable({
      roles: ['ceo'],
      permissions: ['qt:interview:admin:list'],
    })

    const results = wrapper.findAll('.candidate-choice__status')
    expect(results).toHaveLength(2)
    expect(results.map((result) => result.text())).toEqual(['Pass', 'Out'])
    expect(results.every((result) => result.classes().includes('is-plain-result'))).toBe(true)
    expect(results[0].classes()).toContain('status-tag--success')
    expect(results[1].classes()).toContain('status-tag--danger')
  })

  it('renders the management first-round actions', () => {
    const wrapper = mountTable({
      roles: ['qt_mgmt'],
      permissions: [
        'qt:interview:admin:list',
        'qt:interview:admin:evaluate',
      ],
    })

    expect(wrapper.text()).toContain('阅览简历')
    expect(wrapper.text()).toContain('查看面评')
    expect(wrapper.text()).toContain('编辑面评')
    expect(wrapper.text()).not.toContain('评定')
  })

  it('gives a regular manager all first-round review actions without result controls', () => {
    const wrapper = mountTable({
      roles: ['qt_manager'],
      permissions: ['qt:interview:admin:list', 'qt:interview:admin:evaluate'],
    })

    expect(wrapper.text()).toContain('阅览简历')
    expect(wrapper.text()).toContain('编辑面评')
    expect(wrapper.text()).toContain('查看面评')
    expect(wrapper.find('[data-test="candidate-result-pass"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="candidate-result-out"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('是否录用')
  })

  it('recognizes the real memberDepartment field when showing feedback actions', () => {
    const wrapper = mountTable({
      roles: ['qt_manager'],
      permissions: ['qt:interview:admin:list', 'qt:interview:admin:evaluate'],
      user: { userId: 21, memberDepartment: '产品部' },
    })

    expect(wrapper.text()).toContain('编辑面评')
  })

  it('gives management all second-round review actions', () => {
    const wrapper = mountTable({
      roles: ['qt_mgmt'],
      permissions: ['qt:interview:admin:offer'],
      roundId: 2,
    })

    expect(wrapper.text()).toContain('面试评分')
    expect(wrapper.text()).toContain('阅览简历')
    expect(wrapper.text()).toContain('Pass')
    expect(wrapper.text()).toContain('Out')
    expect(wrapper.text()).toContain('发送邮件')
    expect(wrapper.text()).not.toContain('编辑面评')
    expect(wrapper.text()).not.toContain('查看面评')
  })

  it('hides Pass and Out when a department VP has no own-department track', () => {
    const wrapper = mountTable({
      roles: ['qt_mgmt'],
      permissions: ['qt:interview:admin:list', 'qt:interview:admin:offer', 'qt:interview:admin:evaluate'],
      roundId: 2,
      user: { userId: 21, memberDepartment: 'DESIGN', deptCode: 'DESIGN' },
    })

    expect(wrapper.find('[data-test="candidate-result-pass"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="candidate-result-out"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="candidate-score"]').exists()).toBe(false)
  })

  it('gives a regular manager all second-round review actions without admin controls', () => {
    const evaluatedCandidate = structuredClone(candidate)
    evaluatedCandidate.choices[0].rounds[2].evaluations = [
      { interviewerId: 21, content: '二面面评' },
    ]
    const wrapper = mountTable({
      roles: ['qt_manager'],
      permissions: ['qt:interview:admin:evaluate'],
      roundId: 2,
      rows: [evaluatedCandidate],
    })

    expect(wrapper.text()).toContain('面试评分')
    expect(wrapper.text()).toContain('阅览简历')
    expect(wrapper.find('[data-test="candidate-result-pass"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="candidate-result-out"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('发送邮件')
  })

  it('shows first-round Pass and Out as peer actions only for admin', async () => {
    const wrapper = mountTable({
      roles: ['admin'],
      permissions: ['*:*:*'],
    })

    expect(wrapper.get('[data-test="candidate-result-pass"]').text()).toBe('Pass')
    expect(wrapper.get('[data-test="candidate-result-out"]').text()).toBe('Out')
    await wrapper.get('[data-test="candidate-result-pass"]').trigger('click')
    expect(wrapper.emitted('select-result')).toEqual([[candidate, 'PASS']])
  })

  it('enables mail only for final PASS departments that are not sent', () => {
    const row = structuredClone(candidate)
    row.choices[0].rounds[2].status = 'PASS'
    row.choices[1].rounds[1].status = 'PASS'
    row.choices[1].rounds[2] = { status: 'PENDING', advanced: true }
    const props = { roles: ['ceo'], permissions: ['*:*:*'], roundId: 2, rows: [row] }
    const pending = mountTable(props)
    expect(pending.get('[data-test="candidate-notice"]').attributes('disabled')).toBeDefined()

    row.choices[1].rounds[2].status = 'OUT'
    const missingStatus = mountTable(props)
    expect(missingStatus.get('[data-test="candidate-notice"]').attributes('disabled')).toBeDefined()
    expect(missingStatus.get('[data-test="candidate-notice"]').text()).toBe('邮件状态待同步')
    row.noticeStatuses = { PRODUCT: 'UNSENT' }
    const single = mountTable(props)
    expect(single.get('[data-test="candidate-notice"]').attributes('disabled')).toBeUndefined()
    row.noticeStatuses = { PRODUCT: 'SENT' }
    const sent = mountTable(props)
    expect(sent.get('[data-test="candidate-notice"]').attributes('disabled')).toBeDefined()

    row.choices[1].rounds[2].status = 'PASS'
    row.noticeStatuses = { PRODUCT: 'SENT', DESIGN: 'UNSENT' }
    const two = mountTable(props)
    expect(two.get('[data-test="candidate-notice"]').attributes('disabled')).toBeUndefined()
    row.noticeStatuses.DESIGN = 'SENT'
    const bothSent = mountTable(props)
    expect(bothSent.get('[data-test="candidate-notice"]').attributes('disabled')).toBeDefined()

    row.choices[0].rounds[2].status = 'OUT'
    row.choices[1].rounds[2].status = 'OUT'
    const bothOut = mountTable(props)
    expect(bothOut.get('[data-test="candidate-notice"]').attributes('disabled')).toBeDefined()
  })

})
