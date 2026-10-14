import { findAccountByToken } from '../data/accounts'
import { mockForumPosts, mockForumComments } from '../data/forum'

const account = (config) => findAccountByToken(config.headers?.Authorization || '')
const unauthenticated = { code: 401, msg: '请先登录塔员账号' }
const forbidden = { code: 403, msg: '只能操作自己发布的内容' }
const canManage = (current, item) => Number(current.user.userId) === Number(item.authorId) || current.permissions.includes('qt:forum:moderate') || current.permissions.includes('*:*:*')
const nextId = () => Date.now() + Math.floor(Math.random() * 1000)
const now = () => new Date().toLocaleString('zh-CN', { hour12: false }).replaceAll('/', '-')

function scopedRows(rows, params = {}) {
  const pageNum = Math.max(1, Number(params.pageNum) || 1)
  const pageSize = Math.max(1, Number(params.pageSize) || 10)
  return { code: 200, rows: rows.slice((pageNum - 1) * pageSize, pageNum * pageSize), total: rows.length }
}

function findPost(config) { return mockForumPosts.find((item) => item.postId === Number(config.path.match(/\/posts\/(\d+)/)?.[1])) }
function findComment(config) { return mockForumComments.find((item) => item.commentId === Number(config.path.match(/\/comments\/(\d+)/)?.[1])) }

export const forumHandlers = [
  { method: 'get', path: '/qt/forum/posts', handle(config) {
    const current = account(config)
    if (!current) return unauthenticated
    const scoped = config.params?.scope === 'mine'
      ? mockForumPosts.filter((item) => item.authorId === current.user.userId)
      : mockForumPosts
    const rows = config.params?.category
      ? scoped.filter((item) => item.category === config.params.category)
      : scoped
    return scopedRows(rows, config.params)
  } },
  { method: 'post', path: '/qt/forum/posts', handle(config) {
    const current = account(config)
    if (!current) return unauthenticated
    const { title, content, category } = config.data || {}
    if (!String(title || '').trim() || !String(content || '').trim()) return { code: 400, msg: '标题和正文不能为空' }
    if (!['PROJECT', 'TECH', 'LIFE'].includes(category)) return { code: 400, msg: '请选择帖子分类' }
    const post = { postId: nextId(), title: title.trim(), content: content.trim(), category,
      authorId: current.user.userId, authorName: current.user.nickName,
      authorDepartment: current.user.memberDepartment, createdAt: now(), commentCount: 0 }
    mockForumPosts.unshift(post)
    return { code: 200, data: post }
  } },
  { method: 'post', path: '/qt/forum/images', async handle(config) {
    if (!account(config)) return unauthenticated
    const file = config.data instanceof FormData ? config.data.get('file') : null
    if (!file || !/^image\/(png|jpeg|webp|gif)$/.test(file.type) || file.size > 5 * 1024 * 1024) return { code: 400, msg: '仅支持 5 MB 内的 PNG、JPG、WEBP、GIF 图片' }
    const dataUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result)
      reader.onerror = () => reject(new Error('图片读取失败'))
      reader.readAsDataURL(file)
    })
    return { code: 200, data: { url: dataUrl } }
  } },
  { method: 'get', match: (path) => /^\/qt\/forum\/posts\/\d+$/.test(path), handle(config) {
    if (!account(config)) return unauthenticated
    const post = findPost(config)
    return post ? { code: 200, data: post } : { code: 404, msg: '帖子不存在或已删除' }
  } },
  { method: 'put', match: (path) => /^\/qt\/forum\/posts\/\d+$/.test(path), handle(config) {
    const current = account(config)
    if (!current) return unauthenticated
    const post = findPost(config)
    if (!post) return { code: 404, msg: '帖子不存在或已删除' }
    if (Number(current.user.userId) !== Number(post.authorId)) return forbidden
    const { title, content, category } = config.data || {}
    if (!String(title || '').trim() || !String(content || '').trim()) return { code: 400, msg: '标题和正文不能为空' }
    Object.assign(post, { title: title.trim(), content: content.trim(), category })
    return { code: 200, data: post }
  } },
  { method: 'delete', match: (path) => /^\/qt\/forum\/posts\/\d+$/.test(path), handle(config) {
    const current = account(config)
    if (!current) return unauthenticated
    const post = findPost(config)
    if (!post) return { code: 404, msg: '帖子不存在或已删除' }
    if (!canManage(current, post)) return forbidden
    mockForumPosts.splice(mockForumPosts.indexOf(post), 1)
    for (let index = mockForumComments.length - 1; index >= 0; index -= 1) {
      if (mockForumComments[index].postId === post.postId) mockForumComments.splice(index, 1)
    }
    return { code: 200 }
  } },
  { method: 'get', match: (path) => /^\/qt\/forum\/posts\/\d+\/comments$/.test(path), handle(config) {
    if (!account(config)) return unauthenticated
    const post = findPost(config)
    if (!post) return { code: 404, msg: '帖子不存在或已删除' }
    return scopedRows(mockForumComments.filter((item) => item.postId === post.postId), config.params)
  } },
  { method: 'post', match: (path) => /^\/qt\/forum\/posts\/\d+\/comments$/.test(path), handle(config) {
    const current = account(config)
    if (!current) return unauthenticated
    const post = findPost(config)
    if (!post) return { code: 404, msg: '帖子不存在或已删除' }
    const content = String(config.data?.content || '').trim()
    if (!content) return { code: 400, msg: '评论不能为空' }
    const target = config.data?.parentId ? mockForumComments.find((item) => item.commentId === Number(config.data.parentId) && item.postId === post.postId) : null
    if (config.data?.parentId && !target) return { code: 400, msg: '回复目标不存在' }
    const comment = { commentId: nextId(), postId: post.postId, parentId: target?.commentId || null,
      content, authorId: current.user.userId, authorName: current.user.nickName,
      replyToUserName: target?.authorName || '', createdAt: now() }
    mockForumComments.push(comment)
    post.commentCount += 1
    return { code: 200, data: comment }
  } },
  { method: 'put', match: (path) => /^\/qt\/forum\/comments\/\d+$/.test(path), handle(config) {
    const current = account(config)
    if (!current) return unauthenticated
    const comment = findComment(config)
    if (!comment) return { code: 404, msg: '评论不存在或已删除' }
    if (Number(current.user.userId) !== Number(comment.authorId)) return forbidden
    const content = String(config.data?.content || '').trim()
    if (!content) return { code: 400, msg: '评论不能为空' }
    comment.content = content
    return { code: 200, data: comment }
  } },
  { method: 'delete', match: (path) => /^\/qt\/forum\/comments\/\d+$/.test(path), handle(config) {
    const current = account(config)
    if (!current) return unauthenticated
    const comment = findComment(config)
    if (!comment) return { code: 404, msg: '评论不存在或已删除' }
    if (!canManage(current, comment)) return forbidden
    mockForumComments.splice(mockForumComments.indexOf(comment), 1)
    const post = mockForumPosts.find((item) => item.postId === comment.postId)
    if (post) post.commentCount = Math.max(0, post.commentCount - 1)
    return { code: 200 }
  } },
]
