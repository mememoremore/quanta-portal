import { beforeEach, describe, expect, it } from 'vitest'
import {
  getMemberCohorts,
  getMemberList,
  mapCohorts,
  mapMember,
  retainMember,
} from '@/api/members'
import { mockMembers, resetMockMembers } from '@/mock/data/members'
import { setToken } from '@/utils/token'

describe('member API adapter', () => {
  beforeEach(() => {
    resetMockMembers()
    setToken('mock-token-admin')
  })

  it('maps RuoYi user fields to the stable member model', () => {
    expect(
      mapMember({
        userId: 7,
        nickName: '测试成员',
        memberDepartment: '研发部',
        memberTitle: '经理',
        roleCategory: 'manager',
        memberCohort: '21',
        phonenumber: '138****0000',
        memberStatus: 'active',
      }),
    ).toMatchObject({
      id: 7,
      name: '测试成员',
      department: '研发部',
      roleCode: 'MANAGER',
      roleLabel: '经理层',
      cohort: '21',
      status: 'active',
    })
  })

  it('filters by cohort, department, role and name', async () => {
    const result = await getMemberList({
      pageNum: 1,
      pageSize: 20,
      memberCohort: '21',
      memberDepartment: '研发部',
      roleCategory: 'MANAGER',
      nickName: '赵',
    })

    expect(result.total).toBe(1)
    expect(result.rows[0]).toMatchObject({ name: '赵明', roleCode: 'MANAGER' })
  })

  it('retains a member and synchronizes them idempotently to the next cohort', async () => {
    const first = await retainMember(2002, { sourceCohortId: '20' })
    const second = await retainMember(2002, { sourceCohortId: '20' })

    expect(first.data.targetCohort).toBe('21')
    expect(second.code).toBe(200)
    expect(mockMembers.find((item) => item.userId === 2002)?.memberStatus).toBe('retained')
    expect(
      mockMembers.filter((item) => item.memberCohort === '21' && item.nickName === '周浩'),
    ).toHaveLength(1)
  })

  it('returns dynamic cohort metadata', async () => {
    const cohorts = await getMemberCohorts()
    expect(cohorts.find((item) => item.isCurrent)).toMatchObject({ value: '21' })
  })

  it('maps the real backend cohort payload', () => {
    expect(
      mapCohorts({
        currentCohort: { cohortId: 21, cohortName: '第21届', isCurrent: true },
        cohorts: [{ id: 21, name: '第21届', isCurrent: true }],
      }),
    ).toEqual([{ value: '21', label: '第21届', isCurrent: true }])
  })
})
