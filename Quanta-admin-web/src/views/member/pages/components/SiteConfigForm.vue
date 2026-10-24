<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createPageSite, updatePageSite } from '@/api/portal/pages'

const props = defineProps({
  site: { type: Object, default: null },
})
const emit = defineEmits(['saved'])

const isCreate = () => !props.site?.siteId
const form = reactive({
  slug: props.site?.slug || '',
  repoUrl: props.site?.repoUrl || '',
  branch: props.site?.branch || 'pages',
  publishDir: props.site?.publishDir || '/',
})
const submitting = ref(false)
const errorMessage = ref('')

async function submit() {
  submitting.value = true
  errorMessage.value = ''
  try {
    if (isCreate()) {
      const body = { slug: form.slug.trim(), repoUrl: form.repoUrl.trim(), branch: form.branch.trim(), publishDir: form.publishDir.trim() }
      if (!body.slug) delete body.slug
      const result = await createPageSite(body)
      ElMessage.success('站点已创建，首次部署已在排队')
      emit('saved', result)
    } else {
      const result = await updatePageSite(props.site.siteId, {
        repoUrl: form.repoUrl.trim(),
        branch: form.branch.trim(),
        publishDir: form.publishDir.trim(),
      })
      ElMessage.success('配置已保存，已按新配置排队部署')
      emit('saved', result)
    }
  } catch (error) {
    errorMessage.value = error.message || '保存失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="pages-form" data-testid="pages-config-form" @submit.prevent="submit">
    <label class="pages-form__field">
      <span>站点标识（创建后不可修改，留空随机分配）</span>
      <input v-model="form.slug" type="text" :disabled="!isCreate()" data-testid="pages-slug" maxlength="63" placeholder="my-lab" />
    </label>
    <label class="pages-form__field">
      <span>GitHub 仓库地址（公开仓库）</span>
      <input v-model="form.repoUrl" type="url" required data-testid="pages-repo-url" placeholder="https://github.com/octo/hello" />
    </label>
    <div class="pages-form__row">
      <label class="pages-form__field">
        <span>发布分支</span>
        <input v-model="form.branch" type="text" data-testid="pages-branch" placeholder="pages" />
      </label>
      <label class="pages-form__field">
        <span>发布目录</span>
        <input v-model="form.publishDir" type="text" data-testid="pages-publish-dir" placeholder="/" />
      </label>
    </div>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert" data-testid="pages-form-error">{{ errorMessage }}</p>
    <button class="member-primary-button" type="submit" :disabled="submitting" data-testid="pages-form-submit">
      {{ isCreate() ? '创建站点' : '保存配置并重新部署' }}
    </button>
  </form>
</template>
