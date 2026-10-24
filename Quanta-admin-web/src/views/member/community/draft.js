import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { FORUM_CATEGORIES } from '@/api/portal/forum'

export function forumDraftKey(userId, postId = '') {
  return userId ? `quanta:forum-draft:v1:${userId}:${postId || 'new'}` : ''
}

export function useForumDraft({ userId, postId, title, category, content, visibility }) {
  const status = ref('')
  let key = ''
  let ready = false
  let timer

  function flush() {
    clearTimeout(timer)
    if (!ready || !key) return
    try {
      if (!title.value.trim() && !content.value.trim()) {
        localStorage.removeItem(key)
        status.value = ''
        return
      }
      const savedAt = new Date().toISOString()
      localStorage.setItem(key, JSON.stringify({ version: 1, title: title.value, category: category.value, content: content.value, visibility: visibility?.value ?? 'MEMBERS', savedAt }))
      status.value = `草稿已保存到此设备 · ${new Date(savedAt).toLocaleTimeString('zh-CN', { hour12: false })}`
    } catch {
      status.value = '本地保存失败，请先复制正文备份'
    }
  }

  function initialize() {
    key = forumDraftKey(userId(), postId())
    if (!key) return
    try {
      const draft = JSON.parse(localStorage.getItem(key) || 'null')
      if (draft?.version === 1 && typeof draft.title === 'string' && typeof draft.content === 'string'
        && draft.title.length <= 80 && draft.content.length <= 20000
        && FORUM_CATEGORIES.some((item) => item.value === draft.category)) {
        title.value = draft.title
        category.value = draft.category
        content.value = draft.content
        // Older drafts never had visibility. Editing a restricted post must not silently declassify it.
        if (visibility && draft.visibility !== undefined) visibility.value = draft.visibility
        status.value = '已恢复此账号在本设备保存的草稿'
      }
    } catch {
      status.value = '本地草稿不可用，请留意备份正文'
    }
    ready = true
  }

  function clear() {
    ready = false
    clearTimeout(timer)
    try { if (key) localStorage.removeItem(key) } catch { /* Do not undo a successful publication. */ }
    status.value = ''
  }

  watch([title, category, content, ...(visibility ? [visibility] : [])], () => {
    if (!ready) return
    status.value = '正在保存本地草稿…'
    clearTimeout(timer)
    timer = setTimeout(flush, 500)
  }, { flush: 'sync' })

  const onVisibilityChange = () => { if (document.visibilityState === 'hidden') flush() }
  onMounted(() => {
    window.addEventListener('pagehide', flush)
    window.addEventListener('beforeunload', flush)
    document.addEventListener('visibilitychange', onVisibilityChange)
  })
  onBeforeUnmount(() => {
    flush()
    window.removeEventListener('pagehide', flush)
    window.removeEventListener('beforeunload', flush)
    document.removeEventListener('visibilitychange', onVisibilityChange)
  })

  return { initialize, flush, clear, status }
}
