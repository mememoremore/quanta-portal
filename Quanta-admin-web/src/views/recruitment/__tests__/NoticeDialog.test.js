import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import NoticeDialog from '../components/NoticeDialog.vue'

const stubs = {
  ElDialog: { template: '<section><slot name="header"/><slot/><slot name="footer"/></section>' },
  ElButton: { emits: ['click'], template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot/></button>' },
}

describe('NoticeDialog', () => {
  beforeEach(() => {
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:qr-preview'),
      revokeObjectURL: vi.fn(),
    })
  })

  afterEach(() => vi.unstubAllGlobals())

  it('requires a QR image for an admission email', async () => {
    const wrapper = mount(NoticeDialog, {
      props: { modelValue: true, preview: { result: 'PASS', offeredDepartment: 'FRONTEND', subject: 'Quanta 前端部录用通知', content: '<p>你好，李同学。</p><p>你已通过 Quanta 前端部面试，正式加入前端部。</p>' } },
      global: { stubs },
    })
    await wrapper.get('[data-test="send-notice"]').trigger('click')
    expect(wrapper.text()).toContain('录用邮件必须添加群二维码')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('renders the department admission template as HTML', () => {
    const wrapper = mount(NoticeDialog, {
      props: { modelValue: true, preview: { result: 'PASS', offeredDepartment: 'BACKEND', content: '<p>你好，李同学。</p><p>你已通过 Quanta 后端部面试，正式加入后端部。</p>' } },
      global: { stubs },
    })
    expect(wrapper.get('[aria-label="邮件模板"]').html()).toContain('后端部')
    expect(wrapper.text()).toContain('将显示在邮件中，并作为附件备份')
  })

  it('reuses and clears a cached department QR image', async () => {
    const cachedFile = new File(['qr'], 'frontend-group.png', { type: 'image/png' })
    const wrapper = mount(NoticeDialog, {
      props: {
        modelValue: true,
        cachedFile,
        preview: { result: 'PASS', offeredDepartment: 'FRONTEND', content: '<p>录用通知</p>' },
      },
      global: { stubs },
    })
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('frontend-group.png')
    expect(wrapper.get('[data-test="qr-preview"]').attributes('src')).toBe('blob:qr-preview')

    await wrapper.get('[data-test="send-notice"]').trigger('click')
    expect(wrapper.emitted('submit')?.[0]).toEqual([cachedFile])

    await wrapper.get('[data-test="clear-qr-cache"]').trigger('click')
    expect(wrapper.emitted('update:cachedFile')?.at(-1)).toEqual([null])

    await wrapper.get('[data-test="send-notice"]').trigger('click')
    expect(wrapper.text()).toContain('录用邮件必须添加群二维码')
  })

  it('offers separate PASS departments and disables one already sent', async () => {
    const wrapper = mount(NoticeDialog, {
      props: {
        modelValue: true,
        department: 'DESIGN',
        options: [
          { value: 'PRODUCT', label: '产品部', status: 'SENT' },
          { value: 'DESIGN', label: '设计部', status: 'UNSENT' },
        ],
        preview: { department: 'DESIGN', departmentLabel: '设计部', result: 'PASS' },
      },
      global: { stubs },
    })
    const choices = wrapper.findAll('[data-test="notice-department"]')
    expect(choices).toHaveLength(2)
    expect(choices[0].attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('邮件已发送')
    expect(wrapper.text()).toContain('设计部')
    await choices[1].trigger('click')
    expect(wrapper.emitted('update:department')?.[0]).toEqual(['DESIGN'])
  })
})
