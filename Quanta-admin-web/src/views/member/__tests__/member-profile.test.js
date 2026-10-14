import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getCurrentProfile } from '@/api/portal/members'
import MemberProfile from '../profile/index.vue'

vi.mock('@/api/portal/members', () => ({ getCurrentProfile: vi.fn() }))

describe('member profile page', () => {
  beforeEach(() => {
    getCurrentProfile.mockReset()
    getCurrentProfile.mockResolvedValue({
      id: 7,
      memberNo: 'QT007',
      name: '李同学',
      department: '产品',
      title: '成员',
    })
  })

  it('states clearly that the business card is browser-local', async () => {
    const wrapper = mount(MemberProfile, {
      global: {
        stubs: {
          ElButton: { template: '<button><slot /></button>' },
          ElInput: { template: '<textarea />' },
          ElMessage: true,
        },
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('仅保存在当前浏览器')
    expect(wrapper.text()).toContain('李同学')
  })
})
