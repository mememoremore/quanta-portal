import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import MaterialPreview from '../MaterialPreview.vue'
import { downloadPortalMaterial } from '@/api/portal/content'

vi.mock('@/api/portal/content', () => ({ downloadPortalMaterial: vi.fn() }))
enableAutoUnmount(afterEach)

const material = { materialId: 1, fileName: '笔记.md', fileSize: 100 }
const mountDialog = (value = material) => mount(MaterialPreview, {
  props: { material: value },
})

describe('member material preview', () => {
  beforeEach(() => {
    downloadPortalMaterial.mockReset()
    downloadPortalMaterial.mockResolvedValue({ blob: new Blob(['# 示例\n\n正文']) })
  })
  afterEach(() => vi.unstubAllGlobals())

  it('fetches through the authenticated download helper and renders Markdown', async () => {
    const wrapper = mountDialog()
    await flushPromises()
    expect(downloadPortalMaterial).toHaveBeenCalledWith(expect.objectContaining({ ...material, downloadUrl: '/qt/materials/1/download' }), expect.objectContaining({ signal: expect.any(AbortSignal), timeout: 120000 }))
    expect(wrapper.get('.material-preview__markdown h1').text()).toBe('示例')
    expect(wrapper.find('iframe').exists()).toBe(false)
  })

  it('does not fetch unsupported files but keeps download available', async () => {
    const wrapper = mountDialog({ ...material, fileName: '培训.pptx' })
    await flushPromises()
    expect(downloadPortalMaterial).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('暂不支持在线预览')
    await wrapper.get('[data-preview-download]').trigger('click')
    expect(wrapper.emitted('download')[0][0].fileName).toBe('培训.pptx')
  })

  it('shows failures without claiming success and allows retry', async () => {
    downloadPortalMaterial.mockRejectedValueOnce(new Error('资料不存在或已删除'))
    const wrapper = mountDialog()
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('资料不存在或已删除')
    await wrapper.get('[data-preview-retry]').trigger('click')
    await flushPromises()
    expect(wrapper.get('.material-preview__markdown').text()).toContain('正文')
  })

  it('aborts closed requests and ignores obsolete results when switching files', async () => {
    let resolveFirst
    downloadPortalMaterial.mockImplementationOnce(() => new Promise(resolve => { resolveFirst = resolve }))
    const wrapper = mountDialog()
    const signal = downloadPortalMaterial.mock.calls[0][1].signal
    await wrapper.setProps({ material: { ...material, materialId: 2, fileName: 'new.txt' } })
    await flushPromises()
    expect(signal.aborted).toBe(true)
    resolveFirst({ blob: new Blob(['# OLD']) })
    await flushPromises()
    expect(wrapper.find('.material-preview__markdown').exists()).toBe(false)
    expect(wrapper.get('.material-preview__text').text()).not.toContain('OLD')
    const latest = downloadPortalMaterial.mock.lastCall[1].signal
    wrapper.unmount()
    expect(latest.aborted).toBe(true)
  })

  it('rejects oversized metadata before fetching and oversized actual response after fetching', async () => {
    const wrapper = mountDialog({ ...material, fileSize: 3 * 1024 * 1024 })
    await flushPromises()
    expect(downloadPortalMaterial).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toContain('2 MB')
    downloadPortalMaterial.mockResolvedValue({ blob: new Blob(['x'.repeat(2 * 1024 * 1024 + 1)]) })
    await wrapper.setProps({ material })
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('2 MB')
  })

  it('validates image bytes and releases the temporary URL on unmount', async () => {
    const create = vi.fn().mockReturnValue('blob:preview-test'), revoke = vi.fn()
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: create, revokeObjectURL: revoke }))
    downloadPortalMaterial.mockResolvedValue({ blob: new Blob([new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])]) })
    const wrapper = mountDialog({ ...material, fileName: 'image.png' })
    await flushPromises()
    expect(wrapper.get('img').attributes('src')).toBe('blob:preview-test')
    wrapper.unmount()
    expect(revoke).toHaveBeenCalledWith('blob:preview-test')
  })
})
