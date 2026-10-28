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
        memberDepartment: 'BACKEND',
        memberTitle: '经理',
        roleCategory: 'manager',
        memberCohort: '21',
        phonenumber: '138****0000',
        memberStatus: 'active',
      }),
    ).toMatchObject({
      id: 7,
      name: '测试成员',
      department: '后端',
      roleCode: 'MANAGER',
      roleLabel: '经理层',
      cohort: '第21届',
      status: 'active',
    })
  })

  it('normalizes member departments to Chinese labels', () => {
    expect(mapMember({ memberDepartment: 'BACKEND' }).department).toBe('后端')
    expect(mapMember({ memberDepartment: 'FRONTEND' }).department).toBe('前端')
    expect(mapMember({ memberDepartment: 'PRODUCT' }).department).toBe('产品')
    expect(mapMember({ memberDepartment: 'DESIGN' }).department).toBe('设计')
    // 历史数据直接存中文时原样透传，不被吞掉
    expect(mapMember({ memberDepartment: '后端部' }).department).toBe('后端部')
    expect(mapMember({ memberDepartment: '' }).department).toBe('-')
  })

  it('treats the legacy MEMBER role as an intern', () => {
    expect(mapMember({ roleCategory: 'MEMBER' }).roleCode).toBe('INTERN')
    expect(mapMember({ roleCategory: 'INTERN' }).roleCode).toBe('INTERN')
    expect(mapMember({ roleCategory: 'member' }).roleCode).toBe('INTERN')
    expect(mapMember({ roleCategory: 'MGMT' }).roleCode).toBe('MGMT')
    expect(mapMember({ roleCategory: 'MANAGER' }).roleCode).toBe('MANAGER')
  })

  it('prefers the cohort name and falls back to a numeric member cohort label', () => {
    expect(mapMember({ memberCohort: '21', cohortName: '第21届' }).cohort).toBe('第21届')
    expect(mapMember({ memberCohort: '第21届' }).cohort).toBe('第21届')
    expect(mapMember({ memberCohort: '' }).cohort).toBe('')
  })

  it('filters by cohort, department, role and name', async () => {
    const result = await getMemberList({
      pageNum: 1,
      pageSize: 20,
      memberCohort: '21',
      memberDepartment: 'BACKEND',
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
