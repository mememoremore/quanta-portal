import { findAccountByToken } from '../data/accounts'
import { mockCohorts, mockMembers } from '../data/members'
import { MATERIAL_CATEGORIES } from '@/utils/material-categories'
import { managedMemberPayload } from '@/api/member-admin'

export const mockMemberManagementOptions = {
  contractVersion:'member-admin-v1',canCreate:true,departments:MATERIAL_CATEGORIES,
  cohorts:mockCohorts,
  roleProfiles:[
    {value:'MGMT',label:'管理层',titles:['VP','负责人','CTO'],permissions:['后台登录','按已有管理层模板操作，招新仍按所属部门校验']},
    {value:'MANAGER',label:'经理层',titles:['经理'],permissions:['后台登录','本部门招新','本部门资料上传']},
    {value:'INTERN',label:'普通塔员（实习生）',titles:['实习生'],permissions:['塔员门户，四方向资料均可查看']},
    {value:'MEMBER',label:'普通成员',titles:['成员'],permissions:['塔员门户，四方向资料均可查看']},
  ],
}
function guard(config) {
  const account=findAccountByToken(config.headers?.Authorization || '')
  return !account ? {code:401,msg:'请先登录'} : !account.roles.includes('ceo') ? {code:403,msg:'仅 CEO 可维护成员'} : null
}
const find=(config)=>mockMembers.find(row=>Number(row.userId)===Number(config.path.split('/').pop()) && (config.params?.memberCohort === 'unassigned' || !config.params?.memberCohort || String(row.memberCohort)===String(config.params.memberCohort)))
const protectedMember = row => ['CEO','ADMIN'].includes(String(row?.memberTitle || '').toUpperCase())
export const memberAdminHandlers=[
  {method:'get',path:'/qt/member/manage/options',handle(config){return guard(config)||{code:200,data:mockMemberManagementOptions}}},
  {method:'get',match:path=>/^\/qt\/member\/manage\/\d+$/.test(path),handle(config){
    const error=guard(config);if(error)return error
    const row=find(config)
    return row ? {code:200,data:{...row,userName:row.userName||String(row.userId),version:String(row.version||1),protected:protectedMember(row),canEdit:!protectedMember(row),canAssignRoles:!protectedMember(row)}}:{code:404,msg:'成员不存在'}
  }},
  {method:'post',path:'/qt/member/manage',handle(config){
    const error=guard(config);if(error)return error
    try {
      const data=managedMemberPayload(config.data,mockMemberManagementOptions,'create')
      if(mockMembers.some(row=>row.userName===data.userName))return {code:409,msg:'账号已存在'}
      const userId=Math.max(0,...mockMembers.map(row=>Number(row.userId)))+1
      mockMembers.push({...data,userId,version:'1',memberStatus:'active',isQuantaMember:'1',joinTime:new Date().toISOString().slice(0,10)})
      return {code:200,data:{userId,version:'1'}}
    } catch(failure){return {code:400,msg:failure.message}}
  }},
  {method:'put',match:path=>/^\/qt\/member\/manage\/\d+$/.test(path),handle(config){
    const error=guard(config);if(error)return error
    const row=mockMembers.find(item=>Number(item.userId)===Number(config.path.split('/').pop()))
    if(!row)return {code:404,msg:'成员不存在'}
    if(protectedMember(row))return {code:403,msg:'该账号受保护'}
    if(String(row.version||1)!==String(config.data?.version))return {code:409,msg:'成员资料已被更新，请重新加载'}
    try {
      const data=managedMemberPayload(config.data,mockMemberManagementOptions,config.data.mode)
      if(data.mode==='create')return {code:400,msg:'无效操作'}
      if(data.mode==='identity' && (data.roleCategory!==row.roleCategory || data.memberTitle!==row.memberTitle))return {code:400,msg:'请通过分配权限修改身份层级'}
      Object.assign(row,data,{version:String(Number(row.version||1)+1)})
      return {code:200,data:{userId:row.userId,version:row.version}}
    } catch(failure){return {code:400,msg:failure.message}}
  }},
]
