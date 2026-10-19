import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import PreviewPage from '../materials/preview.vue'
import { getPortalMaterial } from '@/api/portal/content'

vi.mock('@/api/portal/content', () => ({ getPortalMaterial: vi.fn(), downloadPortalMaterial: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => route }))
const route = reactive({ params: { materialId: '1' } })
const stubs = { RouterLink: { template: '<a><slot /></a>' }, MaterialPreview: { props: ['material'], template: '<div class="reader-stub">{{ material.fileName }}</div>' } }
const mountPage = () => mount(PreviewPage, { global: { stubs } })
enableAutoUnmount(afterEach)

describe('refreshable member preview page', () => {
  beforeEach(() => {
    route.params.materialId = '1'
    getPortalMaterial.mockReset()
    getPortalMaterial.mockResolvedValue({ materialId: 1, fileName: '笔记.md' })
  })

  it('loads metadata from the protected existing endpoint and sets a useful tab title', async () => {
    const wrapper = mountPage()
    await flushPromises()
    expect(getPortalMaterial).toHaveBeenCalledWith('1', { signal: expect.any(AbortSignal) })
    expect(wrapper.get('.reader-stub').text()).toBe('笔记.md')
    expect(document.title).toBe('笔记.md - 资料预览 - Quanta')
  })

  it('shows missing file errors with a retry, not an empty reader', async () => {
    getPortalMaterial.mockRejectedValueOnce(new Error('资料不存在或已删除'))
    const wrapper = mountPage()
    await flushPromises()
    expect(wrapper.get('[role=alert]').text()).toContain('资料不存在或已删除')
    expect(wrapper.find('.reader-stub').exists()).toBe(false)
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.get('.reader-stub').text()).toBe('笔记.md')
  })

  it('cancels metadata on navigation and ignores an obsolete file response', async () => {
    let resolveFirst
    getPortalMaterial.mockImplementationOnce(() => new Promise(resolve => { resolveFirst = resolve }))
    const wrapper = mountPage()
    const firstSignal = getPortalMaterial.mock.lastCall[1].signal
    getPortalMaterial.mockResolvedValue({ materialId: 2, fileName: '新笔记.md' })
    route.params.materialId = '2'
    await flushPromises()
    expect(firstSignal.aborted).toBe(true)
    resolveFirst({ materialId: 1, fileName: '旧笔记.md' })
    await flushPromises()
    expect(wrapper.get('.reader-stub').text()).toBe('新笔记.md')
    const latestSignal = getPortalMaterial.mock.lastCall[1].signal
    wrapper.unmount()
    expect(latestSignal.aborted).toBe(true)
  })
})
