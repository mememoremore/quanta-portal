import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import * as api from '@/api/portal/members'
import MemberPage from '../member.vue'
vi.mock('@/api/portal/members',()=>({getPublicMemberProfile:vi.fn(),getMemberPosts:vi.fn()}))
enableAutoUnmount(afterEach)
const person={id:7,name:'林同学',department:'前端',title:'实习生',cohort:'21',bio:'',skills:[],githubUrl:'javascript:alert(1)',websiteUrl:'',contactEmail:'secret@example.com',showContactEmail:false}
async function setup() {
  const router=createRouter({history:createMemoryHistory(),routes:[{path:'/member/people/:userId',component:MemberPage}]})
  await router.push('/member/people/7')
  const wrapper=mount({template:'<router-view />'},{global:{plugins:[router]}})
  await flushPromises()
  return {wrapper,router}
}
describe('read-only member page',()=>{
  beforeEach(()=>{vi.clearAllMocks();api.getPublicMemberProfile.mockResolvedValue(person);api.getMemberPosts.mockResolvedValue({rows:[],total:0})})
  it('hides private contact and unsafe URLs, has no editing and shows empty states',async()=>{
    const {wrapper}=await setup()
    expect(wrapper.text()).toContain('暂未填写个人介绍')
    expect(wrapper.text()).not.toContain('secret@example.com')
    expect(wrapper.find('a[href^="javascript:"]').exists()).toBe(false)
    expect(wrapper.find('input').exists()).toBe(false)
    await wrapper.get('#member-posts-tab').trigger('click');await flushPromises()
    expect(api.getMemberPosts).toHaveBeenCalledWith('7',{pageNum:1,pageSize:10})
    expect(wrapper.text()).toContain('还没有发布帖子')
  })
  it('loads a changed route and discards a late previous response',async()=>{
    let resolveOld
    api.getPublicMemberProfile.mockReturnValueOnce(new Promise(r=>{resolveOld=r})).mockResolvedValueOnce({...person,id:8,name:'陈同学'})
    const {wrapper,router}=await setup()
    await router.push('/member/people/8');await flushPromises()
    resolveOld(person);await flushPromises()
    expect(wrapper.get('h1').text()).toBe('陈同学')
  })
  it('reports missing member without inventing a profile',async()=>{
    api.getPublicMemberProfile.mockRejectedValue(Object.assign(new Error('missing'),{code:404}))
    const {wrapper}=await setup()
    expect(wrapper.get('[role="alert"]').text()).toContain('成员不存在')
    expect(wrapper.find('.member-public-card').exists()).toBe(false)
  })
})
