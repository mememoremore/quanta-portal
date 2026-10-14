import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ScoreDialog from '../components/ScoreDialog.vue'

const ElInputNumberStub = { props: ['modelValue'], emits: ['update:modelValue'], template: '<input aria-label="二面分数" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))">' }

describe('ScoreDialog', () => {
  it('submits one integer score for the selected department', async () => {
    const wrapper = mount(ScoreDialog, {
      props: { modelValue: true, candidateName: '李同学', department: 'FRONTEND', options: [{ value: 'FRONTEND', label: '第一志愿 · 前端部' }], score: 88, canEdit: true },
      global: { stubs: {
        ElDialog: { template: '<section><slot name="header"/><slot/><slot name="footer"/></section>' },
        ElSelect: { template: '<select><slot/></select>' }, ElOption: { template: '<option/>' },
        ElInputNumber: ElInputNumberStub,
        ElButton: { emits: ['click'], template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot/></button>' },
      } },
    })
    await wrapper.get('input[aria-label="二面分数"]').setValue('96')
    await wrapper.get('.quanta-primary-button').trigger('click')
    expect(wrapper.emitted('submit')?.[0]).toEqual([96])
  })
})
