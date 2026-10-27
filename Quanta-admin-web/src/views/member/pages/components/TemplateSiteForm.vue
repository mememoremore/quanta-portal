<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createPageSite,
  DEFAULT_TEMPLATE_KEY,
  PAGE_TEMPLATES,
  TEMPLATE_BRANCH,
  TEMPLATE_REPO_URL,
} from '@/api/portal/pages'

const emit = defineEmits(['saved'])

const form = reactive({ slug: '' })
const selectedKey = ref(DEFAULT_TEMPLATE_KEY)
const submitting = ref(false)
const errorMessage = ref('')

const selectedTemplate = computed(() => PAGE_TEMPLATES.find((template) => template.key === selectedKey.value)
  || PAGE_TEMPLATES[0])

async function submit() {
  submitting.value = true
  errorMessage.value = ''
  const template = selectedTemplate.value
  try {
    const body = {
      repoUrl: TEMPLATE_REPO_URL,
      branch: TEMPLATE_BRANCH,
      publishDir: template.publishDir,
    }
    const slug = form.slug.trim()
    if (slug) body.slug = slug
    const result = await createPageSite(body)
    ElMessage.success(`已用「${template.name}」创建站点，部署已排队`)
    emit('saved', result)
  } catch (error) {
    errorMessage.value = error.message || '创建失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="pages-template" data-testid="pages-template-form">
    <div class="pages-template__head">
      <p>ONE-CLICK TEMPLATE</p>
      <h3>选一套模板，一分钟上线</h3>
      <span>无需 GitHub。先选视觉风格，创建后自动部署；之后随时可以换成自己的仓库。</span>
    </div>

    <div class="pages-template__options" role="radiogroup" aria-label="选择模板" data-testid="pages-template-options">
      <button
        v-for="template in PAGE_TEMPLATES"
        :key="template.key"
        type="button"
        role="radio"
        :aria-checked="selectedKey === template.key"
        :class="['pages-template__option', { 'is-selected': selectedKey === template.key }]"
        :data-testid="`pages-template-option-${template.key}`"
        :data-template-key="template.key"
        @click="selectedKey = template.key"
      >
        <span
          class="pages-template__preview"
          :data-preview="template.preview"
          :style="{ '--template-accent': template.accent }"
          aria-hidden="true"
        ><i></i><i></i><i></i></span>
        <span class="pages-template__option-name">{{ template.name }}</span>
        <span class="pages-template__option-desc">{{ template.description }}</span>
      </button>
    </div>

    <form class="pages-form" @submit.prevent="submit">
      <label class="pages-form__field">
        <span>站点标识（可选，留空随机分配）</span>
        <input
          v-model="form.slug"
          type="text"
          maxlength="63"
          data-testid="pages-template-slug"
          placeholder="my-space"
        />
      </label>
      <p class="pages-template__hint">
        模板会展示你在<RouterLink to="/member/profile">个人资料</RouterLink>中填写的社区昵称、头像、简介、技能和公开链接；
        联系邮箱仅在勾选公开后显示。
      </p>
      <p v-if="errorMessage" class="member-feedback is-error" role="alert" data-testid="pages-template-error">
        {{ errorMessage }}
      </p>
      <button class="member-primary-button" type="submit" :disabled="submitting" data-testid="pages-template-submit">
        {{ submitting ? '创建中…' : `用「${selectedTemplate.name}」创建` }}
      </button>
    </form>
  </section>
</template>
