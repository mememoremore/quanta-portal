import { describe, expect, it } from 'vitest'
import { canManageForumContent, groupForumComments } from '../comment-tree'

describe('forum comment presentation and permissions', () => {
  it('flattens nested replies under one root', () => {
    const grouped = groupForumComments([
      { commentId: 1, parentId: null },
      { commentId: 2, parentId: 1 },
      { commentId: 3, parentId: 2 },
    ])
    expect(grouped).toHaveLength(1)
    expect(grouped[0].replies.map((item) => item.commentId)).toEqual([2, 3])
  })

  it('limits delete controls to the author or forum moderator', () => {
    const item = { authorId: 8 }
    expect(canManageForumContent(item, 8, [])).toBe(true)
    expect(canManageForumContent(item, 9, [])).toBe(false)
    expect(canManageForumContent(item, 9, ['qt:forum:moderate'])).toBe(true)
  })
})
