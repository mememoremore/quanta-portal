<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import MemberAuthor from '@/components/MemberAuthor.vue'
import { safeProfileReturn } from '@/utils/member-profile'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { createForumComment, deleteForumComment, deleteForumPost, forumCategoryLabel, getForumPost, listForumComments, updateForumComment } from '@/api/portal/forum'
import { groupForumComments, canManageForumContent } from './comment-tree'
import MarkdownContent from './MarkdownContent.vue'
import './community.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const post = ref(null)
const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = 100
const loading = ref(true)
const commentLoading = ref(false)
const submitting = ref(false)
const deleting = ref(false)
const errorMessage = ref('')
const commentError = ref('')
const commentText = ref('')
const replyTarget = ref(null)
const editingCommentId = ref(null)
const editingText = ref('')
const threads = computed(() => groupForumComments(comments.value))
const currentUserId = computed(() => Number(userStore.user?.userId))
const permissions = computed(() => userStore.permissions || [])
const canEditPost = computed(() => post.value?.authorId === currentUserId.value)
const canDeletePost = computed(() => canManageForumContent(post.value, currentUserId.value, permissions.value))
const returnPath = computed(() => safeProfileReturn(router.options?.history.state?.postReturn))
function goBack(event) {
  if (event?.metaKey || event?.ctrlKey || event?.shiftKey || event?.altKey) return
  event?.preventDefault()
  if (router.options?.history.state?.back === returnPath.value) router.back()
  else router.replace(returnPath.value)
}
let postRequestId = 0, commentRequestId = 0

async function loadPost() {
  const current = ++postRequestId
  post.value = null; replyTarget.value = null; editingCommentId.value = null; commentText.value = ''
  loading.value = true
  errorMessage.value = ''
  const commentsRequest = loadComments(true)
  try {
    const result = await getForumPost(route.params.postId)
    if (current !== postRequestId) return
    post.value = result
    await commentsRequest
  } catch (error) {
    if (current === postRequestId) errorMessage.value = error.message || '帖子加载失败'
  } finally {
    if (current === postRequestId) loading.value = false
  }
}

async function loadComments(reset = false) {
  const current = ++commentRequestId
  if (reset) { commentPage.value = 1; comments.value = [] }
  commentLoading.value = true
  commentError.value = ''
  try {
    const result = await listForumComments(route.params.postId, { pageNum: commentPage.value, pageSize: commentPageSize })
    if (current !== commentRequestId) return
    comments.value = reset ? result.rows : [...comments.value, ...result.rows]
    commentTotal.value = result.total
  } catch (error) {
    if (current === commentRequestId) commentError.value = error.message || '评论加载失败'
  } finally {
    if (current === commentRequestId) commentLoading.value = false
  }
}

async function loadMore() {
  commentPage.value += 1
  await loadComments()
  if (commentError.value) commentPage.value -= 1
}

async function submitComment() {
  commentError.value = ''
  const content = commentText.value.trim()
  if (!content) { commentError.value = '请先填写评论'; return }
  if (content.length > 1000) { commentError.value = '评论最多 1000 字'; return }
  submitting.value = true
  try {
    await createForumComment(route.params.postId, { content, parentId: replyTarget.value?.commentId || null })
    commentText.value = ''
    replyTarget.value = null
    await loadComments(true)
    ElMessage.success('评论已发布')
  } catch (error) {
    commentError.value = error.message || '评论发布失败'
  } finally {
    submitting.value = false
  }
}

function startReply(comment) {
  replyTarget.value = comment
  document.getElementById('community-comment-input')?.focus()
}

function startEdit(comment) {
  editingCommentId.value = comment.commentId
  editingText.value = comment.content
}

async function saveCommentEdit(comment) {
  const content = editingText.value.trim()
  if (!content || content.length > 1000) { commentError.value = '评论需在 1–1000 字以内'; return }
  try {
    await updateForumComment(comment.commentId, { content })
    editingCommentId.value = null
    await loadComments(true)
    ElMessage.success('评论已更新')
  } catch (error) {
    commentError.value = error.message || '修改失败'
  }
}

async function removeComment(comment) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch { return }
  try {
    await deleteForumComment(comment.commentId)
    await loadComments(true)
    ElMessage.success('评论已删除')
  } catch (error) { commentError.value = error.message || '删除失败' }
}

async function removePost() {
  try {
    await ElMessageBox.confirm('删除后帖子及评论将不可恢复，确定继续吗？', '删除帖子', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch { return }
  deleting.value = true
  try {
    await deleteForumPost(route.params.postId)
    ElMessage.success('帖子已删除')
    await router.replace('/member/community')
  } catch (error) { errorMessage.value = error.message || '删除失败' }
  finally { deleting.value = false }
}

watch(() => route.params.postId, loadPost, { immediate:true })
onBeforeUnmount(() => { postRequestId++; commentRequestId++ })
</script>

<template>
  <section class="member-page community-page community-detail">
    <a class="community-back" :href="returnPath" @click="goBack">← {{ returnPath.startsWith('/member/people/') ? '返回成员主页' : '返回社区' }}</a>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="loadPost">重试</button></p>
    <div v-if="loading" class="member-loading portal-card">正在加载帖子…</div>
    <template v-else-if="post">
      <article class="community-detail__post portal-card">
        <span class="community-tag">{{ forumCategoryLabel(post.category) }}</span>
        <h1>{{ post.title }}</h1>
        <div class="community-detail__author"><MemberAuthor :member-id="post.authorId" :name="post.authorName" :avatar="post.authorAvatar" :department="post.authorDepartment" /><time :datetime="post.createdAt">{{ post.createdAt }}</time></div>
        <p v-if="post.viewCount != null" class="community-detail__views">{{ post.viewCount }} 次浏览</p>
        <MarkdownContent :source="post.content" />
        <div v-if="canEditPost || canDeletePost" class="community-detail__actions"><RouterLink v-if="canEditPost" class="member-secondary-button" :to="`/member/community/${post.postId}/edit`">编辑帖子</RouterLink><button v-if="canDeletePost" class="member-secondary-button is-danger" type="button" :disabled="deleting" @click="removePost">删除帖子</button></div>
      </article>

      <section class="community-comments portal-card" aria-label="评论区">
        <h2>讨论 <small>{{ commentTotal }} 条</small></h2>
        <p v-if="commentError" class="member-feedback is-error" role="alert">{{ commentError }}</p>
        <form class="community-comments__form" @submit.prevent="submitComment">
          <div v-if="replyTarget" class="community-comments__reply-target">回复 {{ replyTarget.authorName }} <button type="button" @click="replyTarget = null">取消回复 ×</button></div>
          <textarea id="community-comment-input" v-model="commentText" maxlength="1000" placeholder="写下你的想法，友善交流…" aria-label="评论内容"></textarea>
          <div><span>{{ commentText.length }} / 1000 字</span><button class="member-primary-button" type="submit" :disabled="submitting">{{ submitting ? '发送中…' : '发布评论' }}</button></div>
        </form>
        <div v-if="commentLoading && !comments.length" class="member-loading">正在加载评论…</div>
        <div v-else-if="!threads.length" class="community-comments__empty">还没有评论，来开启讨论吧。</div>
        <div v-for="thread in threads" :key="thread.commentId" class="community-comment-thread">
          <div v-for="comment in [thread, ...thread.replies]" :key="comment.commentId" class="community-comment" :class="{ 'is-reply': comment.commentId !== thread.commentId }">
            <MemberAuthor :member-id="comment.authorId" :name="comment.authorName" :avatar="comment.authorAvatar" avatar-only />
            <div class="community-comment__body">
              <div class="community-comment__header"><MemberAuthor class="community-comment__name" name-only :member-id="comment.authorId" :name="comment.authorName" :avatar="comment.authorAvatar" /><span v-if="comment.replyToUserName">回复 {{ comment.replyToUserName }}</span><time>{{ comment.createdAt }}</time></div>
              <template v-if="editingCommentId === comment.commentId"><textarea v-model="editingText" maxlength="1000" aria-label="编辑评论"></textarea><div class="community-comment__actions"><button type="button" @click="editingCommentId = null">取消</button><button type="button" @click="saveCommentEdit(comment)">保存</button></div></template>
              <template v-else><p>{{ comment.content }}</p><div class="community-comment__actions"><button type="button" @click="startReply(comment)">回复</button><button v-if="comment.authorId === currentUserId" type="button" @click="startEdit(comment)">编辑</button><button v-if="canManageForumContent(comment, currentUserId, permissions)" type="button" @click="removeComment(comment)">删除</button></div></template>
            </div>
          </div>
        </div>
        <button v-if="comments.length < commentTotal" class="member-secondary-button community-comments__more" type="button" :disabled="commentLoading" @click="loadMore">{{ commentLoading ? '加载中…' : '查看更多评论' }}</button>
      </section>
    </template>
  </section>
</template>

<style src="../member.css"></style>
