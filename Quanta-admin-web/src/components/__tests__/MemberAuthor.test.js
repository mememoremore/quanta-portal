import { afterEach, describe, expect, it } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { sessionMemberId } from '@/utils/member-avatars'
import MemberAuthor from '../MemberAuthor.vue'
enableAutoUnmount(afterEach)
async function setup(id) {
  const router=createRouter({history:createMemoryHistory(),routes:[{path:'/:pathMatch(.*)*',component:{template:'<div />'}}]})
  await router.push('/member/community/3')
  const wrapper=mount(MemberAuthor,{props:{memberId:id,name:'林',avatar:'/photo.jpg'},global:{plugins:[router]}})
  return {wrapper,router}
}
describe('member author links',()=>{
  it('links other members independently and remembers the originating article',async()=>{
    const {wrapper,router}=await setup(8)
    expect(wrapper.get('a').attributes('href')).toBe('/member/people/8')
    await wrapper.get('a').trigger('click')
    await new Promise(r=>setTimeout(r,0))
    expect(router.options.history.state.memberReturn).toBe('/member/community/3')
  })
  it('sends own author links to personal center and never creates invalid links',async()=>{
    sessionMemberId.value=7
    const {wrapper}=await setup(7)
    expect(wrapper.get('a').attributes('href')).toBe('/member/profile')
    await wrapper.setProps({memberId:NaN})
    expect(wrapper.find('a').exists()).toBe(false)
  })
})
