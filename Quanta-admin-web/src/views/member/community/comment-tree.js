export function groupForumComments(comments = []) {
  const byId = new Map(comments.map((comment) => [comment.commentId, comment]))
  const roots = []
  const rootRows = new Map()

  for (const comment of comments) {
    if (comment.parentId && byId.has(comment.parentId)) continue
    const root = { ...comment, replies: [] }
    roots.push(root)
    rootRows.set(root.commentId, root)
  }

  for (const comment of comments) {
    if (!comment.parentId || !byId.has(comment.parentId)) continue
    let parent = comment
    const seen = new Set([comment.commentId])
    while (parent.parentId && byId.has(parent.parentId) && !seen.has(parent.parentId)) {
      seen.add(parent.parentId)
      parent = byId.get(parent.parentId)
    }
    const root = rootRows.get(parent.commentId)
    if (root) root.replies.push(comment)
  }
  return roots
}

export function canManageForumContent(item, userId, permissions = []) {
  return Boolean(item && (Number(item.authorId) === Number(userId) || permissions.includes('qt:forum:moderate') || permissions.includes('*:*:*')))
}
