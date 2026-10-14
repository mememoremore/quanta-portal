import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ResumeDialog from '../components/ResumeDialog.vue'

const ElDialogStub = {
  props: ['modelValue'],
  template: '<section><slot name="header" /><slot /></section>',
}

describe('ResumeDialog', () => {
  it('renders normalized choices and existing contact fields', () => {
    const wrapper = mount(ResumeDialog, {
      props: {
        modelValue: true,
        application: {
          realName: '陈思思',
          studentNo: '20241003193',
          major: '电子商务',
          className: '2024级1班',
          email: 'student@example.com',
          phone: '13800138000',
          resumeUrl: 'https://example.com/resume.pdf?token=x',
          resumeFileName: '陈思思-简历.pdf',
          choices: [
            { choiceOrder: 2, department: 'DESIGN' },
            { choiceOrder: 1, department: 'PRODUCT' },
          ],
        },
      },
      global: { stubs: { ElDialog: ElDialogStub } },
    })

    expect(wrapper.text()).toContain('第一志愿产品部')
    expect(wrapper.text()).toContain('第二志愿设计部')
    expect(wrapper.text()).toContain('student@example.com')
    expect(wrapper.text()).toContain('13800138000')
    const preview = wrapper.get('[data-testid="resume-preview"]')
    const download = wrapper.get('[data-testid="resume-download"]')
    expect(preview.attributes('href')).toBe('https://example.com/resume.pdf?token=x')
    expect(preview.attributes('target')).toBe('_blank')
    expect(download.attributes('download')).toBe('陈思思-简历.pdf')
  })

  it('shows a clear fallback when contact information is absent', () => {
    const wrapper = mount(ResumeDialog, {
      props: { modelValue: true, application: { realName: '陈思思', choices: [] } },
      global: { stubs: { ElDialog: ElDialogStub } },
    })

    expect(wrapper.text()).toContain('邮箱暂无')
    expect(wrapper.text()).toContain('联系方式暂无')
    expect(wrapper.text()).toContain('暂无 PDF 简历')
  })

})
