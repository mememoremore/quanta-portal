import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OfferConfirmDialog from '../components/OfferConfirmDialog.vue'
import OfferDialog from '../components/OfferDialog.vue'

const ElDialogStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<section><slot name="header" /><slot /><slot name="footer" /></section>',
}

const ElButtonStub = {
  inheritAttrs: false,
  emits: ['click'],
  template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>',
}

const ElSelectStub = {
  props: ['modelValue', 'disabled'],
  template: '<select :aria-label="$attrs[\'aria-label\']" :disabled="disabled"><slot /></select>',
}

const commonStubs = {
  ElDialog: ElDialogStub,
  ElButton: ElButtonStub,
  ElSelect: ElSelectStub,
  ElOption: { template: '<option />' },
  ElInput: {
    props: ['modelValue', 'type'],
    template: '<textarea v-if="type === \'textarea\'" :value="modelValue" />',
  },
  ElEmpty: { props: ['description'], template: '<div>{{ description }}</div>' },
}

describe('second-round offer flow', () => {
  it('confirms the locked choice before editing a notification', async () => {
    const wrapper = mount(OfferConfirmDialog, {
      props: {
        modelValue: true,
        candidateName: '陈思思',
        department: 'PRODUCT',
        options: [{ value: 'PRODUCT', label: '第二志愿 · 产品部' }],
      },
      global: { stubs: commonStubs },
    })

    expect(wrapper.text()).toContain('二面结果评定')
    expect(wrapper.text()).toContain('确定向该同学(陈思思)发送二面是否通过的录用通知？')
    expect(wrapper.text()).not.toContain('通知内容')
    expect(wrapper.get('[aria-label="录用志愿"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-test="cancel-offer-step"]').classes()).toContain('offer-confirm-dialog__cancel')
    expect(wrapper.get('[data-test="confirm-offer-step"]').classes()).toContain('offer-confirm-dialog__confirm')

    await wrapper.get('[data-test="confirm-offer-step"]').trigger('click')
    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })

  it('emits Pass and Out changes from the notification editor', async () => {
    const wrapper = mount(OfferDialog, {
      props: {
        modelValue: true,
        candidate: { name: '陈思思' },
        department: 'PRODUCT',
        options: [{ value: 'PRODUCT', label: '第二志愿 · 产品部' }],
        decision: 'PASS',
        content: '祝贺你通过面试',
        evaluations: [
          { evaluationId: 1, interviewerName: '产品部经理', content: '表达清晰，有产品意识。' },
        ],
      },
      global: { stubs: commonStubs },
    })

    expect(wrapper.get('[aria-label="录用志愿"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-test="offer-tab-decision"]').classes()).toContain('is-active')
    await wrapper.get('[data-test="offer-out"]').trigger('click')
    await wrapper.get('[data-test="offer-pass"]').trigger('click')

    expect(wrapper.emitted('update:decision')).toEqual([['FAIL'], ['PASS']])

    await wrapper.get('[data-test="offer-tab-feedback"]').trigger('click')
    expect(wrapper.get('[data-test="offer-tab-feedback"]').classes()).toContain('is-active')
    expect(wrapper.text()).toContain('产品部经理')
    expect(wrapper.text()).toContain('表达清晰，有产品意识。')
    expect(wrapper.find('[data-test="submit-offer"]').exists()).toBe(false)
  })

  it('shows a clear empty state when no second-round feedback exists', async () => {
    const wrapper = mount(OfferDialog, {
      props: {
        modelValue: true,
        candidate: { name: '陈思思' },
        department: 'PRODUCT',
        options: [{ value: 'PRODUCT', label: '第二志愿 · 产品部' }],
        evaluations: [],
      },
      global: { stubs: commonStubs },
    })

    await wrapper.get('[data-test="offer-tab-feedback"]').trigger('click')
    expect(wrapper.text()).toContain('暂无二面面评')
  })
})
