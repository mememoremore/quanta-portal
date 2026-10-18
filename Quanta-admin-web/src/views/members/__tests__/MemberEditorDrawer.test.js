import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import ElementPlus, { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import * as api from '@/api/member-admin'
import MemberEditorDrawer from '../components/MemberEditorDrawer.vue'

vi.mock('@/api/member-admin', async original => ({ ...await original(), getManagedMember:vi.fn(), getMemberManagementOptions:vi.fn(), saveManagedMember:vi.fn() }))
enableAutoUnmount(afterEach)
const options={canCreate:true,departments:[{value:'FRONTEND',label:'前端'}],cohorts:[{value:'21',label:'第21届'}],roleProfiles:[{value:'MANAGER',label:'经理层',titles:['经理'],permissions:['本部门招新']},{value:'INTERN',label:'普通塔员',titles:['实习生'],permissions:[]}]}
const detail={userId:7,nickName:'实名',userName:'20241001',memberDepartment:'FRONTEND',memberTitle:'经理',memberCohort:'21',roleCategory:'MANAGER',version:'1',canEdit:true,canAssignRoles:true,roles:['qt_member','qt_manager']}
async function setup(roles=['ceo']) {
  const pinia=createPinia();useUserStore(pinia).roles=roles
  const wrapper=mount(MemberEditorDrawer,{props:{modelValue:true,mode:'identity',member:{id:7,name:'实名'},cohort:'21'},global:{plugins:[pinia,ElementPlus],stubs:{ElDrawer:{template:'<div><slot/><slot name="footer"/></div>'}}}})
  await flushPromises();return wrapper
}
describe('CEO member maintenance',()=>{
  beforeEach(()=>{vi.clearAllMocks();api.getMemberManagementOptions.mockResolvedValue(options);api.getManagedMember.mockResolvedValue(detail);api.saveManagedMember.mockResolvedValue({userId:7})})
  it('does not treat the technical admin or a manager as the CEO',async()=>{
    const wrapper=await setup(['admin'])
    expect(api.getMemberManagementOptions).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('仅 CEO')
    expect(wrapper.findAll('button').at(-1).attributes('disabled')).toBeDefined()
  })
  it('fails closed and gives a retry when the backend is not connected',async()=>{
    api.getMemberManagementOptions.mockRejectedValue(Object.assign(new Error('missing'),{code:404}))
    const wrapper=await setup()
    expect(wrapper.get('[role=alert]').text()).toContain('接口尚未接通')
    expect(wrapper.findAll('button').at(-1).attributes('disabled')).toBeDefined()
    expect(api.saveManagedMember).not.toHaveBeenCalled()
  })
  it('protects the CEO even when a response mistakenly contains editable flags',async()=>{
    api.getManagedMember.mockResolvedValue({...detail,roles:['ceo']})
    const wrapper=await setup()
    expect(wrapper.text()).toContain('账号受保护')
    expect(wrapper.findAll('button').at(-1).attributes('disabled')).toBeDefined()
  })
  it('confirms before saving and retains input on backend failure',async()=>{
    vi.spyOn(ElMessageBox,'confirm').mockResolvedValue('confirm')
    api.saveManagedMember.mockRejectedValue(new Error('版本冲突，请重新加载'))
    const wrapper=await setup()
    await wrapper.get('input[aria-label="真实姓名"]').setValue('修改后的实名')
    await wrapper.get('form').trigger('submit');await flushPromises()
    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(api.saveManagedMember).toHaveBeenCalledWith(7,expect.objectContaining({nickName:'修改后的实名'}),options,'identity')
    expect(wrapper.text()).toContain('版本冲突')
    expect(wrapper.get('input[aria-label="真实姓名"]').element.value).toBe('修改后的实名')
    expect(wrapper.emitted('saved')).toBeUndefined()
  })
  it('lets the CEO pick a cohort on identity saves but keeps it read-only for access',async()=>{
    api.getManagedMember.mockResolvedValue({...detail,memberCohort:''})
    const identity=await setup()
    expect(identity.get('[aria-label="维护成员届数"]').attributes('disabled')).toBeUndefined()
    expect(identity.text()).toContain('请选择届数')
    identity.unmount()
    const pinia=createPinia();useUserStore(pinia).roles=['ceo']
    const access=mount(MemberEditorDrawer,{props:{modelValue:true,mode:'access',member:{id:7,name:'实名'},cohort:'21'},global:{plugins:[pinia,ElementPlus],stubs:{ElDrawer:{template:'<div><slot/><slot name="footer"/></div>'}}}})
    await flushPromises()
    expect(access.get('[aria-label="维护成员届数"]').attributes('disabled')).toBeDefined()
  })
})
