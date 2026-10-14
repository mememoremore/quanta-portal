import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import RetainConfirmDialog from '../components/RetainConfirmDialog.vue'

const ElDialogStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<section><slot name="header" /><slot /><slot name="footer" /></section>',
}

const ElButtonStub = {
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
}

function mountDialog(props = {}) {
  return mount(RetainConfirmDialog, {
    props: {
      modelValue: true,
      member: { id: 1, name: '陈思思', cohort: '20' },
      ...props,
    },
    global: {
      stubs: { ElDialog: ElDialogStub, ElButton: ElButtonStub },
    },
  })
}

describe('RetainConfirmDialog', () => {
  it('shows the member and emits confirmation', async () => {
    const wrapper = mountDialog()

    expect(wrapper.text()).toContain('成员留任确认')
    expect(wrapper.text()).toContain('确认 陈思思 留任并同步到下一届名单吗？')

    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })

  it('closes without confirming when cancelled', async () => {
    const wrapper = mountDialog()

    await wrapper.findAll('button')[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
    expect(wrapper.emitted('confirm')).toBeUndefined()
  })
})
