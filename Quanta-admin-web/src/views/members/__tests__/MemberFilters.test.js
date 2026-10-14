import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import MemberFilters from '../components/MemberFilters.vue'

describe('MemberFilters', () => {
  it('debounces name searches', async () => {
    vi.useFakeTimers()
    const wrapper = mount(MemberFilters, {
      global: { plugins: [ElementPlus] },
    })

    await wrapper.find('input').setValue('赵明')
    expect(wrapper.emitted('search')).toBeUndefined()
    await vi.advanceTimersByTimeAsync(300)
    expect(wrapper.emitted('search')?.[0]).toEqual(['赵明'])
    vi.useRealTimers()
  })
})
