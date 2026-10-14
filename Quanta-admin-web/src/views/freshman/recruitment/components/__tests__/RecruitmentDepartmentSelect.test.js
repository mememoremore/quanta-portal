import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RecruitmentDepartmentSelect from '../RecruitmentDepartmentSelect.vue'

const options = [
  { value: 'PRODUCT', label: '产品' },
  { value: 'DESIGN', label: '设计' },
  { value: 'FRONTEND', label: '全栈（前端）' },
]

function mountSelect(props = {}) {
  return mount(RecruitmentDepartmentSelect, {
    attachTo: document.body,
    props: {
      modelValue: '',
      options,
      name: 'firstChoice',
      ...props,
    },
  })
}

describe('RecruitmentDepartmentSelect', () => {
  it('opens a branded list and emits the selected department', async () => {
    const wrapper = mountSelect()
    const trigger = wrapper.get('[data-testid="department-select-trigger"]')

    expect(trigger.text()).toContain('请选择')
    expect(trigger.attributes('aria-expanded')).toBe('false')

    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(wrapper.get('[role="listbox"]').isVisible()).toBe(true)

    await wrapper.get('[data-value="DESIGN"]').trigger('click')
    expect(wrapper.emitted('update:modelValue')).toEqual([['DESIGN']])
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('marks the current value and closes on escape or outside click', async () => {
    const wrapper = mountSelect({ modelValue: 'FRONTEND' })
    const trigger = wrapper.get('[data-testid="department-select-trigger"]')

    await trigger.trigger('click')
    expect(wrapper.get('[data-value="FRONTEND"]').attributes('aria-selected')).toBe('true')
    expect(wrapper.get('[data-value="FRONTEND"]').classes()).toContain('is-selected')

    await trigger.trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)

    await trigger.trigger('click')
    document.body.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true }))
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('supports arrow keys and enter selection', async () => {
    const wrapper = mountSelect()
    const trigger = wrapper.get('[data-testid="department-select-trigger"]')

    await trigger.trigger('keydown', { key: 'ArrowDown' })
    await trigger.trigger('keydown', { key: 'ArrowDown' })
    expect(wrapper.get('[data-value="DESIGN"]').classes()).toContain('is-active')

    await trigger.trigger('keydown', { key: 'Enter' })
    expect(wrapper.emitted('update:modelValue')).toEqual([['DESIGN']])
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)
    wrapper.unmount()
  })
})
