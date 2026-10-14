<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { createForumPost, FORUM_CATEGORIES, getForumPost, updateForumPost, uploadForumImage } from '@/api/portal/forum'
import { markdownToHtml } from './markdown'
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
const preview = computed(() => markdownToHtml(content.value))

onMounted(async () => {
  if (!editing.value) return
  loading.value = true
  try {
    const post = await getForumPost(route.params.postId)
    if (post.authorId !== Number(userStore.user?.userId)) {
      errorMessage.value = '只能编辑自己发布的帖子'
      return
    }
    title.value = post.title
    category.value = post.category
    content.value = post.content
    editAuthorized.value = true
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
  if (!['image/png', 'image/jpeg', 'image/webp', 'image/gif'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    ElMessage.error('仅支持 5 MB 内的 PNG、JPG、WEBP、GIF 图片')
    return
  }
  uploading.value = true
  try {
    const url = await uploadForumImage(file)
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
    ElMessage.success(editing.value ? '帖子已更新' : '帖子已发布')
    await router.replace(`/member/community/${post.postId}`)
  } catch (error) {
    errorMessage.value = error.message || '保存失败，请稍后重试'
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
        <label class="community-field"><span>话题</span><span class="community-select"><select v-model="category" aria-label="帖子分类"><option v-for="item in FORUM_CATEGORIES" :key="item.value" :value="item.value">{{ item.label }}</option></select><span class="community-select__arrow" aria-hidden="true"></span></span></label>
      </div>
      <div class="community-editor__body-label"><strong>正文</strong><span class="community-editor__desktop-hint">Markdown 编辑 · 右侧同步预览</span><span class="community-editor__mobile-hint">Markdown 编辑 · 切换查看预览</span></div>
      <div class="community-editor__composer">
        <div class="community-editor__toolbar" aria-label="正文编辑工具">
          <button type="button" title="插入标题" @click="insertText('## ', '', '小标题')">标题</button>
          <button type="button" title="加粗" @click="insertText('**', '**', '加粗文字')">加粗</button>
          <button type="button" data-tool="list" title="插入列表" @click="insertText('- ', '', '列表项')">列表</button>
          <button type="button" title="插入链接" @click="insertText('[', '](https://)', '链接文字')">链接</button>
          <button type="button" class="is-accent" :disabled="uploading" title="插入图片" @click="fileInput?.click()">{{ uploading ? '上传中…' : '＋ 图片' }}</button>
          <button type="button" class="is-accent" title="插入代码块" @click="insertText('\n```js\n', '\n```\n', '在这里写代码')">&lt;/&gt; 代码块</button>
          <input ref="fileInput" class="community-hidden-input" type="file" accept="image/png,image/jpeg,image/webp,image/gif" @change="onImageSelected" />
        </div>
        <div class="community-editor__mobile-tabs" role="tablist" aria-label="编辑模式"><button type="button" :class="{ 'is-active': activePane === 'edit' }" :aria-selected="activePane === 'edit'" role="tab" @click="activePane = 'edit'">编辑</button><button type="button" :class="{ 'is-active': activePane === 'preview' }" :aria-selected="activePane === 'preview'" role="tab" @click="activePane = 'preview'">预览</button></div>
        <div class="community-editor__workspace">
          <div class="community-editor__pane" :class="{ 'is-mobile-hidden': activePane !== 'edit' }"><div class="community-editor__pane-title"><strong>编辑</strong><span>Markdown</span></div><textarea ref="textArea" v-model="content" maxlength="20000" placeholder="写下你的想法。支持 Markdown、代码块和正文图片…" aria-label="帖子正文"></textarea></div>
          <div class="community-editor__pane community-editor__preview" :class="{ 'is-mobile-hidden': activePane !== 'preview' }">
            <div class="community-editor__pane-title"><strong>预览</strong><span>发布后的效果</span></div>
            <!-- markdownToHtml escapes raw HTML and rejects unsafe URLs. -->
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div v-if="content.trim()" class="community-markdown" v-html="preview"></div><div v-else class="community-editor__placeholder">写下正文后，这里会显示排版效果。</div>
          </div>
        </div>
      </div>
      <div class="community-editor__footer"><span>{{ content.length }} / 20000 字 · 图片上限 5 MB</span><div><RouterLink class="member-secondary-button" to="/member/community">取消</RouterLink><button class="member-primary-button" type="button" :disabled="saving || loading || uploading || (editing && !editAuthorized)" @click="save">{{ saving ? '保存中…' : editing ? '保存修改' : '发布帖子' }}</button></div></div>
    </div>
  </section>
</template>

<style src="../member.css"></style>
