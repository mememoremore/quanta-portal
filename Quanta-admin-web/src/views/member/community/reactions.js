import { reactive, ref, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getForumReactions, setForumReaction } from '@/api/portal/forum'

// Only session memory: never persist another member's private history in localStorage.
export function createForumReactions({ write = setForumReaction, read = getForumReactions } = {}) {
  const states = reactive(new Map())
  const pending = reactive(new Set())
  const revision = ref(0)
  let owner = '', epoch = 0

  function syncSession(key) {
    if (owner === key) return
    owner = key; epoch += 1
    states.clear(); pending.clear(); revision.value += 1
  }
  function apply(post) {
    states.set(Number(post.postId), {
      postId: Number(post.postId), likeCount: post.likeCount, liked: post.liked,
      favorited: post.favorited, reactionsReady: true, revision: ++revision.value,
    })
  }
  function ingest(posts, startedAt = revision.value) {
    for (const post of posts) {
      const id = Number(post.postId)
      if (pending.has(id) || (states.get(id)?.revision || 0) > startedAt) continue
      if (!post.reactionsReady) { states.delete(id); continue }
      apply(post)
    }
  }
  async function refresh(postId) {
    const id = Number(postId), generation = epoch, startedAt = revision.value
    if (pending.has(id)) return
    const state = await read(id)
    if (epoch === generation && !pending.has(id) && (states.get(id)?.revision || 0) <= startedAt) apply(state)
  }
  async function toggle(postId, kind) {
    const id = Number(postId), previous = states.get(id), generation = epoch
    if (pending.has(id)) return false
    if (!previous?.reactionsReady || !['liked', 'favorites'].includes(kind)) throw new Error('互动功能尚未就绪，请刷新后重试')
    const field = kind === 'liked' ? 'liked' : 'favorited'
    pending.add(id)
    try {
      let state
      try {
        state = await write(id, kind, !previous[field])
      } catch (error) {
        if (generation !== epoch) return false
        // Known business rejection is final. Network/invalid response is unknown: read, never reverse/retry the write.
        if (Number(error.code) >= 400 && Number(error.code) < 500) throw error
        try { state = await read(id) }
        catch (cause) {
          if (generation !== epoch) return false
          throw new Error('互动结果未确认，请稍后刷新列表核对', { cause })
        }
        if (generation !== epoch) return false
        if (state[field] !== !previous[field]) {
          if (epoch === generation) apply(state)
          throw new Error('操作未完成，请稍后重试', { cause: error })
        }
      }
      if (generation !== epoch) return false
      apply(state)
      return true
    } finally {
      if (generation === epoch) pending.delete(id)
    }
  }
  return { states, revision, syncSession, ingest, refresh, toggle, busy: id => pending.has(Number(id)) }
}

const shared = createForumReactions()
export function useForumReactions() {
  const user = useUserStore()
  // A refresh rotates the token but does not change the member or invalidate the page's loaded snapshots.
  watch(() => `${user.user?.userId || ''}:${Boolean(user.token)}`, key => shared.syncSession(key), { immediate: true, flush: 'sync' })
  return shared
}
