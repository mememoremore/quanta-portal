const initialPosts = [
  {
    postId: 101, title: '这周项目组想找前端同学一起做活动页面', category: 'PROJECT',
    content: '目前有一版设计稿，想讨论一下移动端交互和报名流程。感兴趣的塔友欢迎留言。',
    authorId: 11, authorName: '苏秉博', authorDepartment: '产品部', createdAt: '2026-10-14 10:20:00', commentCount: 2,
  },
  {
    postId: 102, title: '分享一下上周技术沙龙的笔记', category: 'TECH',
    content: '整理了现场讨论的几个问题，也欢迎大家补充。\n\n```js\nconst idea = "build together"\n```',
    authorId: 12, authorName: '苏炳添', authorDepartment: '设计部', createdAt: '2026-10-13 18:35:00', commentCount: 1,
  },
]

const initialComments = [
  { commentId: 201, postId: 101, parentId: null, content: '我可以帮忙看看移动端交互。', authorId: 12, authorName: '苏炳添', createdAt: '2026-10-14 11:00:00' },
  { commentId: 202, postId: 101, parentId: 201, content: '太好了，等我整理一下流程。', authorId: 11, authorName: '苏秉博', replyToUserName: '苏炳添', createdAt: '2026-10-14 11:15:00' },
  { commentId: 203, postId: 102, parentId: null, content: '这段代码的例子很清晰。', authorId: 11, authorName: '苏秉博', createdAt: '2026-10-13 19:00:00' },
]

export let mockForumPosts = structuredClone(initialPosts)
export let mockForumComments = structuredClone(initialComments)
export let mockForumReactions = []

export function resetMockForum() {
  mockForumPosts = structuredClone(initialPosts)
  mockForumComments = structuredClone(initialComments)
  mockForumReactions = []
}
