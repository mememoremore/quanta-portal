import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { getMemberManagementOptions, managedMemberPayload, saveManagedMember } from '../member-admin'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))
const options = { contractVersion: 'member-admin-v1', departments: [{value:'FRONTEND',label:'前端'}], cohorts:[{value:'21',label:'第21届'}], roleProfiles:[{value:'MANAGER',label:'经理层',titles:['经理'],permissions:['可登录后台']} ] }
const form = { nickName:'实名',userName:'20241001',memberDepartment:'FRONTEND',memberTitle:'经理',memberCohort:'21',roleCategory:'MANAGER',version:'1' }
describe('CEO member management contract', () => {
  beforeEach(() => request.mockReset())
  it('fails closed on missing/old management options rather than writing generic user APIs', async () => {
    request.mockResolvedValue({data:{}})
    await expect(getMemberManagementOptions()).rejects.toThrow('接口')
    expect(request.mock.calls.map(([c])=>c.url)).toEqual(['/qt/member/manage/options'])
  })
  it('strips arbitrary permissions and role IDs, and validates existing role/department combinations', () => {
    expect(managedMemberPayload({...form,roleIds:[1],permissions:['*:*:*']},options,'identity')).not.toHaveProperty('roleIds')
    expect(()=>managedMemberPayload({...form,roleCategory:'CEO'},options,'access')).toThrow('角色')
    expect(()=>managedMemberPayload({...form,memberDepartment:'ANDROID'},options,'access')).toThrow('部门')
    expect(()=>managedMemberPayload({...form,memberTitle:'COO'},options,'access')).toThrow('职位')
  })
  it('requires a version and sends access changes atomically, without identity fields', async () => {
    expect(()=>managedMemberPayload({...form,version:''},options,'access')).toThrow('版本')
    request.mockResolvedValue({data:{userId:7}})
    await saveManagedMember(7,form,options,'access')
    const config=request.mock.calls[0][0]
    expect(config.url).toBe('/qt/member/manage/7')
    expect(config.data).toEqual({mode:'access',version:'1',memberDepartment:'FRONTEND',memberTitle:'经理',memberCohort:'21',roleCategory:'MANAGER'})
    expect(config.data).not.toHaveProperty('nickName')
  })
})
