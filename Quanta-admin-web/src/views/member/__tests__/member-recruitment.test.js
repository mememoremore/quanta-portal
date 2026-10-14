import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MemberRecruitment from '@/views/member/recruitment/index.vue'
import { getMemberApplications } from '@/api/portal/recruitment'

vi.mock('@/api/portal/recruitment', () => ({
  recruitmentDepartments: [{ value: 'FRONTEND', label: '前端部' }],
  getMemberApplications: vi.fn(),
}))

describe('member recruitment table', () => {
  it('omits the join status column', async () => {
    getMemberApplications.mockResolvedValue([{
      applicationId: 1,
      realName: '测试同学',
      offeredDepartment: 'FRONTEND',
      joinStatus: 'ACCEPTED',
    }])
    const wrapper = mount(MemberRecruitment, {
      global: { stubs: { StatusTag: { template: '<span><slot /></span>' } } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('录用部门')
    expect(wrapper.text()).not.toContain('加入状态')
    expect(wrapper.text()).not.toContain('已接受')
  })
})
