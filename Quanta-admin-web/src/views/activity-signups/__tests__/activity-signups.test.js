import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ActivitySignupsView from '../index.vue'
import { getActivities, getActivitySignups } from '@/api/activity-signups'
import { downloadCsv } from '@/utils/csv'

vi.mock('@/api/activity-signups', () => ({
  getActivities: vi.fn(),
  getActivitySignups: vi.fn(),
}))

vi.mock('@/utils/csv', () => ({ downloadCsv: vi.fn() }))

const activities = [
  {
    activityId: 7,
    title: '前端技术分享会',
    activityType: 'SHARING',
    activityStart: '2026-09-18 19:00:00',
    locationDesc: '实验室A区',
    capacity: 40,
    status: 'PUBLISHED',
  },
]

describe('activity signup management view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getActivities.mockResolvedValue({ rows: activities, total: 1 })
    getActivitySignups.mockResolvedValue({
      rows: [{
        signupId: 21,
        nickName: '李同学',
        studentNo: '20251003193',
        major: '计算机科学',
        phonenumber: '13800000000',
        signupTime: '2026-09-18',
        status: 'APPLIED',
      }],
      total: 1,
    })
  })

  it('filters activities and opens the unified signup list', async () => {
    const wrapper = mount(ActivitySignupsView, {
      attachTo: document.body,
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('前端技术分享会')
    await wrapper.get('[data-testid="activity-title-filter"]').setValue('前端')
    await wrapper.get('[data-testid="activity-search"]').trigger('click')
    await flushPromises()
    expect(getActivities).toHaveBeenLastCalledWith(expect.objectContaining({ title: '前端' }))

    await wrapper.get('[data-testid="view-signups-7"]').trigger('click')
    await flushPromises()
    expect(getActivitySignups).toHaveBeenCalledWith(7, { pageNum: 1, pageSize: 1000 })
    expect(document.body.textContent).toContain('李同学')
    expect(document.body.textContent).toContain('20251003193')
    expect(document.body.textContent).toContain('13800000000')

    await wrapper.get('[data-testid="export-signups"]').trigger('click')
    expect(downloadCsv).toHaveBeenCalledWith(
      '前端技术分享会-报名名单.csv',
      expect.any(Array),
      expect.arrayContaining([expect.objectContaining({ studentNo: '20251003193' })]),
    )
    wrapper.unmount()
  })

  it('shows an actionable error when activities cannot load', async () => {
    getActivities.mockRejectedValueOnce(new Error('网络异常'))
    const wrapper = mount(ActivitySignupsView, { global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(wrapper.text()).toContain('活动列表加载失败')
    expect(wrapper.text()).toContain('重新加载')
  })
})
