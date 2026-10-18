import { describe, expect, it } from 'vitest'
import { mapForumComment, mapForumPost } from '../forum'
import { mapMember } from '../members'

describe('forum-only community nickname', () => {
  it('uses the community nickname for posts and comments, not directory identity', () => {
    const row = { userId: 1, authorName: '实名', nickName: '实名', authorCommunityNickname: '橘猫', communityNickname: '橘猫' }
    expect(mapForumPost(row).authorName).toBe('橘猫')
    expect(mapForumComment(row).authorName).toBe('橘猫')
    expect(mapMember(row).name).toBe('实名')
    expect(mapMember({ ...row, realName: '报名姓名', nickName: '20251003142' }).name).toBe('报名姓名')
    expect(mapForumPost({ ...row, authorCommunityNickname: '  ' }).authorName).toBe('实名')
    expect(mapForumComment({ ...row, replyToUserName: '另一实名', replyToCommunityNickname: '塔友' }).replyToUserName).toBe('塔友')
  })
})
