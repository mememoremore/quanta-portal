import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { createForumComment, createForumPost, getForumPost, listForumComments, listForumPosts, mapForumPost, uploadForumImage, listMyForumReactions, setForumReaction, getForumReactions } from '../forum'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('member forum API contract', () => {
  it('uses dedicated private history endpoints rather than an unsupported scope', async () => {
    request.mockResolvedValue({ rows: [], total: 0 })
    await listMyForumReactions('liked', { category: 'TECH', pageNum: 2 })
    expect(request).toHaveBeenLastCalledWith({ url: '/qt/forum/me/likes', method: 'get', params: { pageNum: 2, pageSize: 10, category: 'TECH' } })
    await listMyForumReactions('favorites')
    expect(request.mock.lastCall[0].url).toBe('/qt/forum/me/favorites')
  })

  it('sets idempotent target states and rejects unconfirmed or mismatched results', async () => {
    const state = { postId: 8, likeCount: 1, liked: true, favorited: false }
    request.mockResolvedValue({ data: state })
    await expect(setForumReaction(8, 'liked', true)).resolves.toEqual(state)
    expect(request).toHaveBeenLastCalledWith({ url: '/qt/forum/posts/8/like', method: 'put' })
    await setForumReaction(8, 'favorites', false)
    expect(request.mock.lastCall[0].method).toBe('delete')
    await getForumReactions(8)
    expect(request.mock.lastCall[0].url).toBe('/qt/forum/posts/8/reactions')
    request.mockResolvedValue({ data: { ...state, postId: 9 } })
    await expect(setForumReaction(8, 'liked', true)).rejects.toThrow('未确认')
    expect(mapForumPost({}).reactionsReady).toBe(false)
    expect(mapForumPost(state).reactionsReady).toBe(true)
    expect(mapForumPost({ ...state, reactionsReady: false }).reactionsReady).toBe(false)
  })
  it('uses the server viewCount and does not invent a count for old responses', () => {
    expect(mapForumPost({ viewCount: '23' }).viewCount).toBe(23)
    expect(mapForumPost({ viewCount: 0 }).viewCount).toBe(0)
    expect(mapForumPost({}).viewCount).toBeNull()
    expect(mapForumPost({ viewCount: -1 }).viewCount).toBeNull()
  })
  beforeEach(() => request.mockReset())

  it('loads paginated posts and comments with the expected routes', async () => {
    request.mockResolvedValueOnce({ rows: [{ postId: 5, title: '技术帖' }], total: 1 })
    await expect(listForumPosts({ pageNum: 2, scope: 'mine' })).resolves.toMatchObject({ total: 1 })
    expect(request).toHaveBeenCalledWith({ url: '/qt/forum/posts', method: 'get', params: { pageNum: 2, pageSize: 10, scope: 'mine' } })
    request.mockResolvedValueOnce({ rows: [{ commentId: 9, parentId: 8 }], total: 1 })
    await listForumComments(5)
    expect(request).toHaveBeenCalledWith({ url: '/qt/forum/posts/5/comments', method: 'get', params: { pageNum: 1, pageSize: 100 } })
  })

  it('keeps fenced code out of the feed excerpt when no summary is returned', async () => {
    request.mockResolvedValueOnce({ rows: [{ postId: 5, content: '现场笔记\n\n```js\nconst secret = 1\n```\n欢迎讨论' }], total: 1 })
    const result = await listForumPosts()
    expect(result.rows[0].summary).toBe('现场笔记 欢迎讨论')
  })

  it('creates a post and a reply without local-only success', async () => {
    request.mockResolvedValueOnce({ data: { postId: 8, title: '分享', content: '内容' } })
    await expect(createForumPost({ title: '分享', content: '内容', category: 'TECH' })).resolves.toMatchObject({ postId: 8 })
    request.mockResolvedValueOnce({ data: { commentId: 4, parentId: 3 } })
    await createForumComment(8, { content: '谢谢', parentId: 3 })
    expect(request).toHaveBeenLastCalledWith({ url: '/qt/forum/posts/8/comments', method: 'post', data: { content: '谢谢', parentId: 3 } })
  })

  it('propagates backend failures and requires an image URL', async () => {
    request.mockRejectedValueOnce(new Error('服务不可用'))
    await expect(getForumPost(8)).rejects.toThrow('服务不可用')
    request.mockResolvedValueOnce({ data: {} })
    await expect(uploadForumImage(new File(['x'], 'a.png', { type: 'image/png' }))).rejects.toThrow('未返回图片地址')
  })
})
