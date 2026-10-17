import { afterEach, describe, expect, it } from 'vitest'
import { mockRequest } from '@/mock'
import { setToken } from '@/utils/token'
import { mockAccounts } from '@/mock/data/accounts'
import { mockMemberProfiles } from '../member-profiles'
const request=(url,method='get',data)=>mockRequest({url,method,data})
afterEach(()=>mockMemberProfiles.clear())
describe('member profile mock contracts',()=>{
  it('requires login, saves own editable fields and hides unshared contact',async()=>{
    expect((await request('/qt/member/2/profile')).code).toBe(401)
    setToken('mock-token-viewer')
    await request('/qt/member/profile','put',{headline:'你好',skills:['Vue'],contactEmail:'private@example.com',showContactEmail:false,userId:1,memberTitle:'CEO'})
    const own=await request('/qt/member/profile')
    expect(own.data.userId).toBe(2)
    expect(own.data.memberTitle).toBe('实习生')
    expect(own.data.contactEmail).toBe('private@example.com')
    const other=await request('/qt/member/2/profile')
    expect(other.data.contactEmail).toBe('')
    expect(other.data).not.toHaveProperty('userName')
    expect(other.data).not.toHaveProperty('phonenumber')
  })
  it('denies freshmen and returns only the selected authors posts',async()=>{
    setToken('mock-token-viewer')
    const account=mockAccounts.find(a=>a.user.userId===2)
    const previous=account.user.isQuantaMember
    try {account.user.isQuantaMember=0;expect((await request('/qt/member/profile')).code).toBe(403)}
    finally {if(previous===undefined)delete account.user.isQuantaMember;else account.user.isQuantaMember=previous}
    expect((await request('/qt/member/11/posts')).rows.every(p=>p.authorId===11)).toBe(true)
    expect((await request('/qt/member/999999/profile')).code).toBe(404)
  })
})
