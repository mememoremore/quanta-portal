import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ApplicationForm from '../ApplicationForm.vue'

describe('ApplicationForm', () => {
  it('shows the application deadline before the first field', () => {
    const wrapper = mount(ApplicationForm)
    const notice = wrapper.get('[data-testid="application-deadline-notice"]')
    const realName = wrapper.get('[name="realName"]')

    expect(notice.text()).toContain('报名已截止')
    expect(notice.text()).toContain('已投递同学仍可修改')
    expect(notice.text()).toContain('保存草稿不代表报名成功')
    expect(
      notice.element.compareDocumentPosition(realName.element) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()
  })
})
