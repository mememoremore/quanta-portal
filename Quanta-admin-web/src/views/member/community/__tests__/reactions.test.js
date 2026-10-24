import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createForumReactions } from '../reactions'

const snapshot = { postId: 5, likeCount: 1, liked: false, favorited: false, reactionsReady: true }
describe('forum reaction session state', () => {
  let write, read, store
  beforeEach(() => {
    write = vi.fn(); read = vi.fn()
    store = createForumReactions({ write, read })
    store.syncSession('user8')
    store.ingest([snapshot])
  })
  it('serializes changes per post and only confirms server responses', async () => {
    let finish
    write.mockReturnValue(new Promise(resolve => { finish = resolve }))
    const pending = store.toggle(5, 'liked')
    expect(store.busy(5)).toBe(true)
    expect(store.states.get(5).liked).toBe(false)
    await store.toggle(5, 'favorites')
    expect(write).toHaveBeenCalledTimes(1)
    finish({ ...snapshot, liked: true, likeCount: 2 })
    await pending
    expect(store.states.get(5).liked).toBe(true)
    expect(store.busy(5)).toBe(false)
  })
  it('does not apply late replies after an account switch', async () => {
    let finish
    write.mockReturnValue(new Promise(resolve => { finish = resolve }))
    const pending = store.toggle(5, 'liked')
    store.syncSession('user9')
    finish({ ...snapshot, liked: true })
    await pending
    expect(store.states.size).toBe(0)
    expect(store.busy(5)).toBe(false)
  })
  it('keeps old state after failure; a timeout checks state without reversing the write', async () => {
    write.mockRejectedValueOnce(Object.assign(new Error('禁止'), { code: 403 }))
    await expect(store.toggle(5, 'liked')).rejects.toThrow('禁止')
    expect(store.states.get(5).liked).toBe(false)
    expect(read).not.toHaveBeenCalled()
    write.mockRejectedValueOnce(Object.assign(new Error('超时'), { code: 'ECONNABORTED' }))
    read.mockResolvedValue({ ...snapshot, liked: true, likeCount: 2 })
    await store.toggle(5, 'liked')
    expect(read).toHaveBeenCalledWith(5)
    expect(store.states.get(5).liked).toBe(true)
    expect(write).toHaveBeenCalledTimes(2)
  })
  it('does not let a stale list overwrite a confirmed change', async () => {
    const before = store.revision.value
    write.mockResolvedValue({ ...snapshot, liked: true, likeCount: 2 })
    await store.toggle(5, 'liked')
    store.ingest([snapshot], before)
    expect(store.states.get(5).liked).toBe(true)
  })
  it('clearly reports an unknown timeout result', async () => {
    write.mockRejectedValue(new Error('network'))
    read.mockRejectedValue(new Error('network'))
    await expect(store.toggle(5, 'favorites')).rejects.toThrow('结果未确认')
    expect(store.states.get(5).favorited).toBe(false)
    expect(store.busy(5)).toBe(false)
  })
  it('ignores a late error instead of reading with the next member session', async () => {
    let fail
    write.mockReturnValue(new Promise((resolve, reject) => { fail = reject }))
    const pending = store.toggle(5, 'liked')
    store.syncSession('user9')
    fail(new Error('old network failure'))
    await expect(pending).resolves.toBe(false)
    expect(read).not.toHaveBeenCalled()
  })
  it('disables cached state when the server reports the feature is unavailable', () => {
    store.ingest([{ ...snapshot, reactionsReady: false }])
    expect(store.states.has(5)).toBe(false)
  })
})
