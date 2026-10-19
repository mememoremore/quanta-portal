<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElOption, ElSelect } from 'element-plus'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-select.css'
import 'element-plus/theme-chalk/el-option.css'
import 'element-plus/theme-chalk/el-option-group.css'
import 'element-plus/theme-chalk/el-popper.css'
import 'element-plus/theme-chalk/el-scrollbar.css'
import 'element-plus/theme-chalk/el-tag.css'
import { useUserStore } from '@/stores/user'
import { createForumPost, FORUM_CATEGORIES, getForumPost, updateForumPost, uploadForumImage } from '@/api/portal/forum'
import MarkdownContent from './MarkdownContent.vue'
import { useForumDraft } from './draft'
import { restoreForumMarkdown } from './markdown'
import { prepareForumImage } from './image-upload'
import './community.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const editing = computed(() => Boolean(route.params.postId))
const title = ref('')
const category = ref('PROJECT')
const content = ref('')
const activePane = ref('edit')
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const errorMessage = ref('')
const editAuthorized = ref(false)
const fileInput = ref(null)
const textArea = ref(null)
const draft = useForumDraft({ userId: () => userStore.user?.userId, postId: () => route.params.postId, title, category, content })
const draftStatus = draft.status

onMounted(async () => {
  if (!editing.value) { draft.initialize(); return }
  loading.value = true
  try {
    const post = await getForumPost(route.params.postId)
    if (post.authorId !== Number(userStore.user?.userId)) {
      errorMessage.value = '只能编辑自己发布的帖子'
      return
    }
    title.value = post.title
    category.value = post.category
    content.value = restoreForumMarkdown(post.content)
    editAuthorized.value = true
    draft.initialize()
  } catch (error) {
    errorMessage.value = error.message || '帖子加载失败'
  } finally {
    loading.value = false
  }
})

async function insertText(before, after = '', placeholder = '') {
  const textarea = textArea.value
  const start = textarea?.selectionStart ?? content.value.length
  const end = textarea?.selectionEnd ?? start
  const selection = content.value.slice(start, end) || placeholder
  content.value = `${content.value.slice(0, start)}${before}${selection}${after}${content.value.slice(end)}`
  activePane.value = 'edit'
  await nextTick()
  textArea.value?.focus()
  textArea.value?.setSelectionRange(start + before.length, start + before.length + selection.length)
}

async function onImageSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    const prepared = await prepareForumImage(file)
    const url = await uploadForumImage(prepared)
    const alt = file.name.replaceAll('[', '').replaceAll(']', '').replaceAll('(', '').replaceAll(')', '')
    await insertText(`![${alt}](${url})`, '', '')
    ElMessage.success('图片已插入正文')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}

async function save() {
  if (editing.value && !editAuthorized.value) return
  errorMessage.value = ''
  const payload = { title: title.value.trim(), category: category.value, content: content.value.trim() }
  if (!payload.title || !payload.content) { errorMessage.value = '请填写标题和正文'; return }
  if (payload.title.length > 80 || payload.content.length > 20000) { errorMessage.value = '标题最多 80 字，正文最多 20000 字'; return }
  saving.value = true
  try {
    const post = editing.value
      ? await updateForumPost(route.params.postId, payload)
      : await createForumPost(payload)
    if (!Number.isFinite(post.postId)) throw new Error('保存成功，但服务端未返回帖子编号')
    draft.clear()
    ElMessage.success(editing.value ? '帖子已更新' : '帖子已发布')
    await router.replace(`/member/community/${post.postId}`)
  } catch (error) {
    errorMessage.value = error.message || '保存失败，请稍后重试'
    draft.flush()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="member-page community-page community-editor">
    <header class="member-page__heading community-hero">
      <div><RouterLink class="community-back" to="/member/community">← 返回社区</RouterLink><p>SHARE YOUR IDEA</p><h1>{{ editing ? '编辑帖子' : '发布帖子' }}</h1><span>文字、代码和图片都可以按讲述顺序放进正文。</span></div>
    </header>
    <div v-if="loading" class="member-loading portal-card">正在加载帖子…</div>
    <div v-else class="community-editor__card portal-card">
      <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }}</p>
      <div class="community-editor__fields">
        <label class="community-field"><span>标题 <small>最多 80 字</small></span><input v-model="title" maxlength="80" placeholder="给讨论起个清晰的标题" /></label>
        <div class="community-field"><span>话题</span><ElSelect v-model="category" class="community-category-select" popper-class="community-category-popper" aria-label="帖子分类"><ElOption v-for="item in FORUM_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" /></ElSelect></div>
      </div>
      <div class="community-editor__body-label"><strong>正文</strong><span class="community-editor__desktop-hint">Markdown 编辑 · 右侧同步预览</span><span class="community-editor__mobile-hint">Markdown 编辑 · 切换查看预览</span></div>
      <div class="community-editor__composer">
        <div class="community-editor__toolbar" aria-label="正文编辑工具">
          <button type="button" title="插入标题" @click="insertText('## ', '', '小标题')">标题</button>
          <button type="button" title="加粗" @click="insertText('**', '**', '加粗文字')">加粗</button>
          <button type="button" data-tool="list" title="插入列表" @click="insertText('- ', '', '列表项')">列表</button>
          <button type="button" title="插入链接" @click="insertText('[', '](https://)', '链接文字')">链接</button>
          <button type="button" class="is-accent" :disabled="uploading" title="插入图片" aria-label="插入图片" @click="fileInput?.click()"><span class="community-editor__desktop-tool">{{ uploading ? '上传中…' : '＋ 图片' }}</span><span class="community-editor__mobile-tool">{{ uploading ? '上传中…' : '图片' }}</span></button>
          <button type="button" class="is-accent" title="插入代码块" aria-label="插入代码块" @click="insertText('\n```js\n', '\n```\n', '在这里写代码')"><span class="community-editor__desktop-tool">&lt;/&gt; 代码块</span><span class="community-editor__mobile-tool">代码</span></button>
          <input ref="fileInput" class="community-hidden-input" type="file" accept="image/png,image/jpeg,image/webp,image/gif" @change="onImageSelected" />
        </div>
        <div class="community-editor__mobile-tabs" role="tablist" aria-label="编辑模式"><button type="button" :class="{ 'is-active': activePane === 'edit' }" :aria-selected="activePane === 'edit'" role="tab" @click="activePane = 'edit'">编辑</button><button type="button" :class="{ 'is-active': activePane === 'preview' }" :aria-selected="activePane === 'preview'" role="tab" @click="activePane = 'preview'">预览</button></div>
        <div class="community-editor__workspace">
          <div class="community-editor__pane" :class="{ 'is-mobile-hidden': activePane !== 'edit' }"><div class="community-editor__pane-title"><strong>编辑</strong><span>Markdown</span></div><textarea ref="textArea" v-model="content" maxlength="20000" placeholder="写下你的想法。支持 Markdown、代码块和正文图片…" aria-label="帖子正文"></textarea></div>
          <div class="community-editor__pane community-editor__preview" :class="{ 'is-mobile-hidden': activePane !== 'preview' }">
            <div class="community-editor__pane-title"><strong>预览</strong><span>发布后的效果</span></div>
            <MarkdownContent v-if="content.trim()" :source="content" /><div v-else class="community-editor__placeholder">写下正文后，这里会显示排版效果。</div>
          </div>
        </div>
      </div>
      <p v-if="draftStatus" class="community-draft-status" role="status">{{ draftStatus }}</p>
      <div class="community-editor__footer"><span>{{ content.length }} / 20000 字 · 原图 20 MB 内，上传图 5 MB 内</span><div><RouterLink class="member-secondary-button" to="/member/community">取消</RouterLink><button class="member-primary-button" type="button" :disabled="saving || loading || uploading || (editing && !editAuthorized)" @click="save">{{ saving ? '保存中…' : editing ? '保存修改' : '发布帖子' }}</button></div></div>
    </div>
  </section>
</template>

<style src="../member.css"></style>
