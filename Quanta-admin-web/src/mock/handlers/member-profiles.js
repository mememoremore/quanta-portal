import { mockAccounts, findAccountByToken } from '../data/accounts'
import { mockMembers } from '../data/members'
import { mockForumPosts } from '../data/forum'
import { normalizeProfile, validateProfile } from '@/utils/member-profile'

export const mockMemberProfiles = new Map()
const userFor = (id) => mockAccounts.find((a) => Number(a.user.userId) === Number(id))?.user || mockMembers.find((u) => Number(u.userId) === Number(id))
export const currentMockAvatar = (id) => userFor(id)?.avatar || ''
export function mockPublicProfile(id, own = false) {
  const user = userFor(id)
  if (!user) return null
  const details = mockMemberProfiles.get(Number(id)) || normalizeProfile()
  return { userId:user.userId, nickName:user.nickName || user.userName, avatar:user.avatar || '', memberDepartment:user.memberDepartment, memberTitle:user.memberTitle || user.roleCategory, memberCohort:user.memberCohort, ...(own ? {memberNo:user.memberNo || ''} : {}), ...details, contactEmail:own || details.showContactEmail ? details.contactEmail : '' }
}
function guard(config) {
  const account = findAccountByToken(config.headers?.Authorization || '')
  if (!account) return { error:{code:401,msg:'请先登录塔员账号'} }
  if (account.user.isQuantaMember != null && ![true,1,'1'].includes(account.user.isQuantaMember)) return {error:{code:403,msg:'仅塔员可访问成员档案'}}
  return { account }
}
export const memberProfileHandlers = [
  { method:'get', path:'/qt/member/profile', handle(config) { const {account,error}=guard(config); return error || {code:200,data:mockPublicProfile(account.user.userId,true)} } },
  { method:'put', path:'/qt/member/profile', handle(config) { const {account,error}=guard(config); if(error)return error; const message=validateProfile(config.data); if(message)return {code:400,msg:message}; mockMemberProfiles.set(Number(account.user.userId),normalizeProfile(config.data)); return {code:200} } },
  { method:'get', match:path=>/^\/qt\/member\/\d+\/profile$/.test(path), handle(config) { const {error}=guard(config); if(error)return error; const data=mockPublicProfile(config.path.split('/')[3]); return data ? {code:200,data} : {code:404,msg:'成员不存在'} } },
  { method:'get', match:path=>/^\/qt\/member\/\d+\/posts$/.test(path), handle(config) { const {error}=guard(config); if(error)return error; const id=Number(config.path.split('/')[3]); if(!userFor(id))return {code:404,msg:'成员不存在'}; const rows=mockForumPosts.filter(p=>Number(p.authorId)===id).map(p=>({...p,authorAvatar:currentMockAvatar(id)})); const page=Math.max(1,Number(config.params?.pageNum)||1),size=Math.max(1,Number(config.params?.pageSize)||10); return {code:200,rows:rows.slice((page-1)*size,page*size),total:rows.length} } },
  { method:'post', path:'/system/user/profile/avatar', async handle(config) { const {account,error}=guard(config); if(error)return error; const file=config.data instanceof FormData ? config.data.get('avatarfile') : null; if(!file || file.type!=='image/jpeg' || file.size>5*1024*1024)return {code:400,msg:'请上传5 MB以内的JPG头像'}; const url=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result));reader.onerror=()=>reject(new Error('图片读取失败'));reader.readAsDataURL(file)}); account.user.avatar=url; for(const member of mockMembers)if(Number(member.userId)===Number(account.user.userId))member.avatar=url; return {code:200,imgUrl:url} } },
]
