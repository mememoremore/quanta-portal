import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DecisionDialog from '../components/DecisionDialog.vue'

const ElDialogStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<section><slot name="header" /><slot /><slot name="footer" /></section>',
}

const ElSelectStub = {
  props: ['modelValue', 'disabled'],
  template: '<select :aria-label="$attrs[\'aria-label\']" :disabled="disabled"><slot /></select>',
}

const ElButtonStub = {
  inheritAttrs: false,
  emits: ['click'],
  template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>',
}

describe('DecisionDialog', () => {
  it('confirms a locked first-round result without Waiting', async () => {
    const wrapper = mount(DecisionDialog, {
      props: {
        modelValue: true,
        candidateName: '陈思思',
        roundId: 1,
        result: 'PASS',
        department: 'PRODUCT',
        options: [{ value: 'PRODUCT', label: '第一志愿 · 产品部' }],
      },
      global: {
        stubs: {
          ElDialog: ElDialogStub,
          ElSelect: ElSelectStub,
          ElOption: { template: '<option />' },
          ElButton: ElButtonStub,
        },
      },
    })

    expect(wrapper.text()).toContain('一面结果评定')
    expect(wrapper.text()).toContain('Pass')
    expect(wrapper.get('[aria-label="评定部门"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.text()).not.toContain('Waiting')

    await wrapper.get('[data-test="confirm-decision"]').trigger('click')
    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })
})
