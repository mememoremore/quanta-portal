import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { request } from '@/utils/request'
import { createProtectedImages, isProtectedForumImage } from '../protected-images'
import { markdownToHtml } from '../markdown'
import { reconcileMarkdown } from '../markdown-dom'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))
afterEach(() => vi.unstubAllGlobals())
const path = '/qt/forum/images/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'

describe('protected forum images', () => {
  it('only sends credentials to canonical API paths', () => {
    expect(isProtectedForumImage(path)).toBe(true)
    for (const source of ['https://example.com' + path, '//example.com' + path, path + '?token=secret', path + '/..']) expect(isProtectedForumImage(source)).toBe(false)
    const html = markdownToHtml(`![图](${path})`)
    expect(html).toContain('data-forum-image=')
    expect(html).not.toContain('src=')
  })

  it('uses authenticated blobs, preserves them on prose edits and releases on removal', async () => {
    const create = vi.fn(() => 'blob:unit-image'), revoke = vi.fn()
    vi.stubGlobal('URL', { createObjectURL: create, revokeObjectURL: revoke })
    request.mockResolvedValue({ data: new Blob(['png'], { type: 'image/png' }) })
    const root = document.createElement('div'), loader = createProtectedImages()
    reconcileMarkdown(root, markdownToHtml(`![图](${path})\n\n原文`))
    loader.sync(root)
    await flushPromises()
    expect(request).toHaveBeenCalledWith(expect.objectContaining({ url: path, responseType: 'blob', signal: expect.any(AbortSignal) }))
    const image = root.querySelector('img')
    expect(image.getAttribute('src')).toBe('blob:unit-image')
    reconcileMarkdown(root, markdownToHtml(`![图](${path})\n\n修改了正文`))
    loader.sync(root)
    expect(root.querySelector('img')).toBe(image)
    expect(root.querySelector('img').getAttribute('src')).toBe('blob:unit-image')
    expect(create).toHaveBeenCalledOnce()
    root.replaceChildren(); loader.sync(root)
    expect(revoke).toHaveBeenCalledWith('blob:unit-image')
    loader.destroy()
  })

  it('discards a late response after navigation and never renders unsafe MIME', async () => {
    const create = vi.fn(), revoke = vi.fn()
    vi.stubGlobal('URL', { createObjectURL: create, revokeObjectURL: revoke })
    let finish
    request.mockReturnValueOnce(new Promise(resolve => { finish = resolve }))
    const root = document.createElement('div'), loader = createProtectedImages()
    root.innerHTML = `<img data-forum-image="${path}">`
    loader.sync(root); loader.destroy()
    finish({ data: new Blob(['png'], { type: 'image/png' }) })
    await flushPromises()
    expect(create).not.toHaveBeenCalled()
    const second = createProtectedImages()
    request.mockResolvedValueOnce({ data: new Blob(['<svg/>'], { type: 'image/svg+xml' }) })
    second.sync(root); await flushPromises()
    expect(root.querySelector('img').hasAttribute('src')).toBe(false)
    expect(root.querySelector('img').getAttribute('title')).toContain('类型无效')
    second.destroy()
  })
})
