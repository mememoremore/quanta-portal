import { beforeEach, describe, expect, it } from 'vitest'
import { forumHandlers } from '../forum'
import { resetMockForum } from '../../data/forum'

function call(method, path, token, data, params) {
  const handler = forumHandlers.find((item) => item.method === method && (item.path === path || item.match?.(path)))
  return handler.handle({ path, headers: { Authorization: `Bearer ${token}` }, data, params })
}

describe('forum mock business rules', () => {
  beforeEach(resetMockForum)

  it('keeps reactions idempotent and private to the current member', () => {
    const path = '/qt/forum/posts/101/like'
    expect(call('put', path, 'mock-token-viewer').data.likeCount).toBe(1)
    expect(call('put', path, 'mock-token-viewer').data.likeCount).toBe(1)
    expect(call('get', '/qt/forum/me/likes', 'mock-token-ceo', undefined, { userId: 13 }).total).toBe(0)
    expect(call('get', '/qt/forum/me/likes', 'mock-token-viewer').total).toBe(1)
    call('put', '/qt/forum/posts/101/favorite', 'mock-token-viewer')
    expect(call('get', '/qt/forum/me/favorites', 'mock-token-viewer').total).toBe(1)
    expect(call('get', '/qt/forum/me/favorites', 'mock-token-ceo').total).toBe(0)
    call('delete', path, 'mock-token-viewer')
    expect(call('get', '/qt/forum/me/likes', 'mock-token-viewer').total).toBe(0)
    call('delete', '/qt/forum/posts/101', 'mock-token-ceo')
    expect(call('get', '/qt/forum/me/favorites', 'mock-token-viewer').total).toBe(0)
  })

  it('allows logged-in members to view and publish posts', () => {
    expect(call('get', '/qt/forum/posts', 'mock-token-viewer').total).toBe(2)
    const created = call('post', '/qt/forum/posts', 'mock-token-viewer', { title: '讨论', content: '内容', category: 'TECH' })
    expect(created.code).toBe(200)
    expect(call('get', '/qt/forum/posts', 'mock-token-viewer').total).toBe(3)
  })

  it('filters categories before paginating the forum feed', () => {
    const result = call('get', '/qt/forum/posts', 'mock-token-viewer', undefined, { category: 'TECH', pageNum: 1, pageSize: 10 })
    expect(result.total).toBe(1)
    expect(result.rows[0].category).toBe('TECH')
  })

  it('blocks edits by other users while letting them reply', () => {
    expect(call('put', '/qt/forum/posts/101', 'mock-token-viewer', { title: '改名', content: '内容', category: 'TECH' }).code).toBe(403)
    expect(call('post', '/qt/forum/posts/101/comments', 'mock-token-viewer', { parentId: 201, content: '回复' }).code).toBe(200)
  })

  it('lets the assigned moderator delete but not edit another author’s post', () => {
    expect(call('put', '/qt/forum/posts/101', 'mock-token-ceo', { title: '改名', content: '内容', category: 'TECH' }).code).toBe(403)
    expect(call('delete', '/qt/forum/posts/101', 'mock-token-ceo').code).toBe(200)
    expect(call('get', '/qt/forum/posts/101', 'mock-token-viewer').code).toBe(404)
  })

  it('requires authentication for forum reads', () => {
    expect(call('get', '/qt/forum/posts', 'invalid').code).toBe(401)
  })
})
