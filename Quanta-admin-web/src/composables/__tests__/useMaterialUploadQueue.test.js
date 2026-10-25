import { describe, expect, it, vi } from 'vitest'
import { useMaterialUploadQueue } from '../useMaterialUploadQueue'

describe('material upload queue', () => {
  const file = name => new File(['test'], name, { type: 'application/pdf' })
  it('rejects invalid files, limits the queue, and never uploads before confirmation', () => {
    const upload = vi.fn()
    const queue = useMaterialUploadQueue({ upload })
    expect(queue.add(file('bad.exe'))).toContain('仅支持')
    for (let i = 0; i < 20; i++) expect(queue.add(file(`${i}.pdf`))).toBe('')
    expect(queue.add(file('21.pdf'))).toContain('20')
    expect(upload).not.toHaveBeenCalled()
    queue.files.value.forEach(item => { item.status = 'success' })
    expect(queue.add(file('more.pdf'))).toBe('')
    expect(queue.pending.value).toHaveLength(1)
  })
  it('continues after failure and retries only failed files, strictly serially', async () => {
    let active = 0, maximum = 0, fail = true
    const upload = vi.fn(async raw => {
      active++; maximum = Math.max(maximum, active)
      await Promise.resolve()
      active--
      if (raw.name === '2.pdf' && fail) throw new Error('服务暂不可用')
    })
    const resolveFolder = vi.fn().mockResolvedValue(7)
    const queue = useMaterialUploadQueue({ upload, resolveFolder })
    for (const name of ['1.pdf', '2.pdf', '3.pdf']) queue.add(file(name))
    await queue.confirm({ category: 'PRODUCT', type: 'TRAINING' })
    expect(queue.files.value.map(item => item.status)).toEqual(['success', 'error', 'success'])
    expect(queue.files.value[0].raw).toBeUndefined()
    expect(queue.files.value[1].raw).toBeInstanceOf(File)
    expect(queue.files.value[1].error).toBe('服务暂不可用')
    expect(maximum).toBe(1)
    fail = false
    await queue.confirm({ category: 'PRODUCT', type: 'TRAINING' })
    expect(upload.mock.calls.map(([raw]) => raw.name)).toEqual(['1.pdf', '2.pdf', '3.pdf', '2.pdf'])
    expect(upload.mock.lastCall[1]).toMatchObject({ category: 'PRODUCT', folderId: 7 })
    expect(queue.pending.value).toHaveLength(0)
  })
  it('blocks duplicate confirmation and keeps the queue when preparing a type fails', async () => {
    let finish
    const resolveFolder = vi.fn(() => new Promise(resolve => { finish = resolve }))
    const upload = vi.fn().mockResolvedValue({})
    const queue = useMaterialUploadQueue({ upload, resolveFolder })
    queue.add(file('1.pdf'))
    const first = queue.confirm({ category: 'FRONTEND', type: 'TRAINING' })
    expect(await queue.confirm({ category: 'FRONTEND', type: 'TRAINING' })).toBeNull()
    expect(queue.add(file('2.pdf'))).toContain('正在上传')
    queue.remove(queue.files.value[0].id)
    expect(queue.files.value).toHaveLength(1)
    finish(12)
    await first
    expect(upload).toHaveBeenCalledTimes(1)
    const failed = useMaterialUploadQueue({ resolveFolder: vi.fn().mockRejectedValue(new Error('没有分类权限')) })
    failed.add(file('1.pdf'))
    await failed.confirm({ category: 'FRONTEND', type: 'TRAINING' })
    expect(failed.files.value[0].status).toBe('waiting')
    expect(failed.feedback.value).toBe('没有分类权限')
    expect(failed.uploading.value).toBe(false)
  })
})
