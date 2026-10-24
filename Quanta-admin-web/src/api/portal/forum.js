import { request } from '@/utils/request'
import { assetUrl } from '@/utils/member-profile'

export const FORUM_CATEGORIES = Object.freeze([
  { value: 'PROJECT', label: '项目协作' },
  { value: 'TECH', label: '技术分享' },
  { value: 'LIFE', label: '日常交流' },
])

export function forumCategoryLabel(value) {
  return FORUM_CATEGORIES.find((item) => item.value === value)?.label || '交流讨论'
}

export const FORUM_VISIBILITIES = Object.freeze([
  { value: 'MEMBERS', label: '全部塔员' },
  { value: 'MANAGERS', label: '经理及以上' },
])

export async function getForumOptions() {
  const response = await request({ url: '/qt/forum/options', method: 'get' })
  const allowed = response.data?.allowedPostVisibilities
  if (!Array.isArray(allowed) || !allowed.includes('MEMBERS') || allowed.some(value => !FORUM_VISIBILITIES.some(item => item.value === value))) {
    throw new Error('无法确认帖子可见范围权限，请稍后重试')
  }
  return allowed
}

export function mapForumPost(row = {}) {
  return {
    ...row,
    postId: Number(row.postId || row.id),
    title: row.title || '',
    visibility: row.visibility ?? 'MEMBERS',
    content: row.content || '',
    summary: row.summary || row.content?.replace(/```[\s\S]*?```/g, ' ').replace(/!\[[^\]]*\]\([^)]+\)/g, ' ').replace(/[#*`![\]()]/g, '').replace(/\s+/g, ' ').trim().slice(0, 140) || '',
    authorId: Number(row.authorId || row.userId),
    authorName: String(row.authorCommunityNickname || '').trim() || row.authorName || row.nickName || '塔员',
    authorAvatar: assetUrl(row.authorAvatar || ''),
    authorDepartment: row.authorDepartment || row.department || '',
    commentCount: Number(row.commentCount || 0),
    likeCount: hasReactionState(row) ? Number(row.likeCount) : 0,
    liked: row.liked === true,
    favorited: row.favorited === true,
    reactionsReady: hasReactionState(row),
    viewCount: row.viewCount != null && row.viewCount !== '' && Number.isSafeInteger(Number(row.viewCount)) && Number(row.viewCount) >= 0 ? Number(row.viewCount) : null,
    createdAt: row.createdAt || row.createTime || '',
  }
}

function hasReactionState(row) {
  return row.reactionsReady !== false && row.likeCount !== null && row.likeCount !== '' && Number.isSafeInteger(Number(row.likeCount)) && Number(row.likeCount) >= 0
    && typeof row.liked === 'boolean' && typeof row.favorited === 'boolean'
}

function reactionState(row, postId) {
  if (Number(row?.postId) !== Number(postId) || !hasReactionState(row || {})) {
    throw new Error('互动结果未确认，请刷新后核对')
  }
  return { postId: Number(postId), likeCount: Number(row.likeCount), liked: row.liked, favorited: row.favorited }
}

function reactionPath(kind) {
  if (kind === 'liked') return 'like'
  if (kind === 'favorites') return 'favorite'
  throw new Error('不支持的互动类型')
}

export async function listMyForumReactions(kind, params = {}) {
  reactionPath(kind)
  const response = await request({ url: `/qt/forum/me/${kind === 'liked' ? 'likes' : 'favorites'}`, method: 'get', params: { pageNum: 1, pageSize: 10, ...params } })
  return { rows: (response.rows || []).map(mapForumPost), total: Number(response.total) || 0 }
}

export async function setForumReaction(postId, kind, active) {
  const response = await request({ url: `/qt/forum/posts/${postId}/${reactionPath(kind)}`, method: active ? 'put' : 'delete' })
  return reactionState(response.data, postId)
}

export async function getForumReactions(postId) {
  const response = await request({ url: `/qt/forum/posts/${postId}/reactions`, method: 'get' })
  return reactionState(response.data, postId)
}

export function mapForumComment(row = {}) {
  return {
    ...row,
    commentId: Number(row.commentId || row.id),
    parentId: row.parentId == null ? null : Number(row.parentId),
    authorId: Number(row.authorId || row.userId),
    authorName: String(row.authorCommunityNickname || '').trim() || row.authorName || row.nickName || '塔员',
    authorAvatar: assetUrl(row.authorAvatar || ''),
    replyToUserName: String(row.replyToCommunityNickname || '').trim() || row.replyToUserName || '',
    createdAt: row.createdAt || row.createTime || '',
  }
}

export async function listForumPosts(params = {}) {
  const response = await request({ url: '/qt/forum/posts', method: 'get', params: { pageNum: 1, pageSize: 10, ...params } })
  return { rows: (response.rows || []).map(mapForumPost), total: Number(response.total) || 0 }
}

export async function getForumPost(postId) {
  const response = await request({ url: `/qt/forum/posts/${postId}`, method: 'get' })
  return mapForumPost(response.data || {})
}

export async function createForumPost(data) {
  const response = await request({ url: '/qt/forum/posts', method: 'post', data })
  return mapForumPost(response.data || {})
}

export async function updateForumPost(postId, data) {
  const response = await request({ url: `/qt/forum/posts/${postId}`, method: 'put', data })
  return mapForumPost(response.data || {})
}

export function deleteForumPost(postId) {
  return request({ url: `/qt/forum/posts/${postId}`, method: 'delete' })
}

export async function listForumComments(postId, params = {}) {
  const response = await request({ url: `/qt/forum/posts/${postId}/comments`, method: 'get', params: { pageNum: 1, pageSize: 100, ...params } })
  return { rows: (response.rows || []).map(mapForumComment), total: Number(response.total) || 0 }
}

export async function createForumComment(postId, data) {
  const response = await request({ url: `/qt/forum/posts/${postId}/comments`, method: 'post', data })
  return mapForumComment(response.data || {})
}

export async function updateForumComment(commentId, data) {
  const response = await request({ url: `/qt/forum/comments/${commentId}`, method: 'put', data })
  return mapForumComment(response.data || {})
}

export function deleteForumComment(commentId) {
  return request({ url: `/qt/forum/comments/${commentId}`, method: 'delete' })
}

export async function uploadForumImage(file, options = {}) {
  const data = new FormData()
  data.append('file', file)
  const response = await request({ ...options, url: '/qt/forum/images', method: 'post', data })
  const url = response.data?.url || response.url
  if (!url) throw new Error('图片上传成功，但服务端未返回图片地址')
  return url
}
