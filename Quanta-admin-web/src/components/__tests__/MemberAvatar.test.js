import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MemberAvatar from '../MemberAvatar.vue'
import { setMemberAvatar, clearMemberAvatars } from '@/utils/member-avatars'

describe('MemberAvatar', () => {
  it('falls back after a broken image and retries a changed URL', async () => {
    const wrapper = mount(MemberAvatar, { props: { name: '林同学', src: '/old.jpg', size: 44 } })
    await wrapper.get('img').trigger('error')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toBe('林')
    await wrapper.setProps({ src: '/new.jpg' })
    expect(wrapper.get('img').attributes('src')).toContain('new.jpg')
    wrapper.unmount()
  })
  it('reactively updates every instance of the same member, not other members', async () => {
    const wrapper = mount(MemberAvatar, { props: { memberId: 7, name: '林' } })
    setMemberAvatar(7, '/new.jpg')
    await wrapper.vm.$nextTick()
    expect(wrapper.get('img').attributes('src')).toContain('new.jpg')
    clearMemberAvatars()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('img').exists()).toBe(false)
    wrapper.unmount()
  })
})
