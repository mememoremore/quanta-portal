import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import * as api from '@/api/portal/members'
import { useUserStore } from '@/stores/user'
import MemberProfile from '../profile/index.vue'
import AvatarUploadDialog from '../profile/components/AvatarUploadDialog.vue'

vi.mock('@/api/portal/members', () => ({ getCurrentProfile:vi.fn(), getOwnMemberProfile:vi.fn(), saveMemberProfile:vi.fn(), uploadMemberAvatar:vi.fn() }))
vi.mock('element-plus', async (original) => ({ ...await original(), ElMessage:{ success:vi.fn(), error:vi.fn(), warning:vi.fn() } }))
enableAutoUnmount(afterEach)

const identity = { id:7, memberNo:'QT007', name:'李同学', department:'产品', title:'实习生', cohort:'第21届', avatar:'/old.jpg' }
async function setup() {
  const pinia = createPinia()
  const router = createRouter({ history:createMemoryHistory(), routes:[{path:'/member/profile',component:MemberProfile},{path:'/member/people/:userId',component:{template:'<p>公开主页</p>'}}] })
  await router.push('/member/profile')
  const store = useUserStore(pinia)
  store.user = { userId:7, nickName:'李同学', avatar:'/old.jpg' }
  const wrapper = mount({template:'<router-view />'}, { global:{plugins:[pinia,router], stubs:{ElDialog:true}} })
  await flushPromises()
  return {wrapper,store}
}
describe('cloud member profile', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.getCurrentProfile.mockResolvedValue(identity)
    api.getOwnMemberProfile.mockResolvedValue({...identity,headline:'原简介',bio:'',skills:['Vue'],githubUrl:'',websiteUrl:'',contactEmail:'',showContactEmail:false})
    api.saveMemberProfile.mockResolvedValue({})
  })
  it('replaces browser-local cards, locks identity and previews cloud fields', async () => {
    localStorage.setItem('quanta_portal_business_cards_v1',JSON.stringify({QT007:{name:'伪造姓名'}}))
    const {wrapper}=await setup()
    expect(wrapper.text()).not.toContain('仅保存在当前浏览器')
    expect(wrapper.text()).not.toContain('伪造姓名')
    expect(wrapper.text()).toContain('李同学')
    expect(wrapper.find('input[aria-label="姓名"]').exists()).toBe(false)
    await wrapper.get('input[aria-label="一句话介绍"]').setValue('新的介绍')
    expect(wrapper.get('.member-preview-card').text()).toContain('新的介绍')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(api.saveMemberProfile).toHaveBeenCalledWith(expect.objectContaining({headline:'新的介绍'}))
    expect(ElMessage.success).toHaveBeenCalledWith('个人档案已保存')
  })
  it('retains edited values on failed save and can cancel to saved snapshot', async () => {
    api.saveMemberProfile.mockRejectedValue(new Error('保存失败'))
    const {wrapper}=await setup()
    await wrapper.get('input[aria-label="一句话介绍"]').setValue('不要丢失')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('input[aria-label="一句话介绍"]').element.value).toBe('不要丢失')
    expect(ElMessage.success).not.toHaveBeenCalled()
    await wrapper.findAll('button').find(b=>b.text()==='取消修改').trigger('click')
    expect(wrapper.get('input[aria-label="一句话介绍"]').element.value).toBe('原简介')
  })
  it('keeps avatar available while a missing cloud endpoint disables saving', async () => {
    api.getOwnMemberProfile.mockRejectedValue(Object.assign(new Error('Not found'),{code:404}))
    const {wrapper}=await setup()
    expect(wrapper.get('[role="alert"]').text()).toContain('接口尚未接通')
    expect(wrapper.get('fieldset').element.disabled).toBe(true)
    expect(wrapper.get('button[aria-label="更换头像"]').element.disabled).toBe(false)
  })
  it('updates shared identity only after avatar upload succeeds', async () => {
    api.uploadMemberAvatar.mockRejectedValueOnce(new Error('上传失败')).mockResolvedValueOnce('/new.jpg')
    const {wrapper,store}=await setup()
    const dialog=wrapper.findComponent(AvatarUploadDialog)
    dialog.vm.$emit('confirm',new File(['jpeg'],'a.jpg',{type:'image/jpeg'}))
    await flushPromises()
    expect(store.user.avatar).toBe('/old.jpg')
    expect(dialog.props('error')).toBe('上传失败')
    dialog.vm.$emit('confirm',new File(['jpeg'],'a.jpg',{type:'image/jpeg'}))
    await flushPromises()
    expect(store.user.avatar).toBe('/new.jpg')
  })
})
