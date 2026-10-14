import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PortalNoticeDialog from '../PortalNoticeDialog.vue'

describe('PortalNoticeDialog', () => {
  it('renders a branded success notice and closes from the action', async () => {
    const wrapper = mount(PortalNoticeDialog, {
      props: {
        modelValue: true,
        type: 'success',
        title: '提交成功',
        message: '报名已成功提交。',
      },
    })

    expect(wrapper.get('[role="alertdialog"]').text()).toContain('提交成功')
    expect(wrapper.classes()).toContain('is-success')
    await wrapper.get('[data-testid="notice-confirm"]').trigger('click')
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('does not render while closed', () => {
    const wrapper = mount(PortalNoticeDialog, { props: { modelValue: false } })
    expect(wrapper.find('[role="alertdialog"]').exists()).toBe(false)
  })
})
