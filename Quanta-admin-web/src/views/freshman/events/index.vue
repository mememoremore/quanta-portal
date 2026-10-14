<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  getMyActivitySignup,
  getPublishedActivities,
  signupActivity,
} from '@/api/portal/activities'

const activities = ref([])
const signups = reactive({})
const submittingIds = ref([])
const loading = ref(true)
const errorMessage = ref('')
const successMessage = ref('')

function isSubmitting(activityId) {
  return submittingIds.value.includes(activityId)
}

async function refreshSignup(activityId) {
  signups[activityId] = await getMyActivitySignup(activityId)
}

async function loadActivities() {
  loading.value = true
  errorMessage.value = ''
  try {
    activities.value = await getPublishedActivities()
    await Promise.all(activities.value.map(({ activityId }) => refreshSignup(activityId)))
  } catch (error) {
    errorMessage.value = error.message || '活动加载失败'
  } finally {
    loading.value = false
  }
}

async function signup(activity) {
  if (isSubmitting(activity.activityId) || signups[activity.activityId]) return
  submittingIds.value = [...submittingIds.value, activity.activityId]
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await signupActivity(activity.activityId, '')
    await refreshSignup(activity.activityId)
    successMessage.value = `已成功报名“${activity.title}”`
  } catch (error) {
    errorMessage.value = error.message || '活动报名失败'
  } finally {
    submittingIds.value = submittingIds.value.filter((id) => id !== activity.activityId)
  }
}

onMounted(loadActivities)
</script>

<template>
  <section class="freshman-events">
    <header class="freshman-page-heading">
      <div>
        <p class="freshman-eyebrow">QUANTA EVENTS</p>
        <h1>活动与分享</h1>
        <span>来线下见面，听听 Quanta 的故事和成员的成长经验。</span>
      </div>
    </header>

    <p v-if="successMessage" class="freshman-feedback is-success" role="status">{{ successMessage }}</p>
    <p v-if="errorMessage" class="freshman-feedback is-error" role="alert">{{ errorMessage }}</p>

    <div v-if="loading" class="freshman-loading portal-card">正在加载活动…</div>
    <div v-else-if="activities.length" class="freshman-events__grid">
      <article v-for="activity in activities" :key="activity.activityId" class="freshman-event-card portal-card">
        <div class="freshman-event-card__type">
          {{ activity.activityType === 'LECTURE' ? '新生宣讲' : activity.activityType === 'SHARING' ? '精英分享' : 'Quanta 活动' }}
        </div>
        <h2>{{ activity.title }}</h2>
        <p>{{ activity.description || '欢迎来到现场，和我们一起交流。' }}</p>
        <dl>
          <div><dt>时间</dt><dd>{{ activity.activityStart || '待通知' }}</dd></div>
          <div><dt>地点</dt><dd>{{ activity.locationDesc || '待通知' }}</dd></div>
          <div><dt>名额</dt><dd>{{ activity.capacity ? `${activity.capacity} 人` : '不限' }}</dd></div>
        </dl>
        <button
          :data-testid="`signup-${activity.activityId}`"
          class="portal-primary-button"
          :disabled="isSubmitting(activity.activityId) || Boolean(signups[activity.activityId])"
          @click="signup(activity)"
        >
          {{ isSubmitting(activity.activityId) ? '报名中…' : signups[activity.activityId] ? '已报名' : '立即报名' }}
        </button>
      </article>
    </div>
    <div v-else class="portal-empty portal-card">
      <strong>暂无已发布活动</strong>
      <p>新活动发布后会第一时间出现在这里。</p>
    </div>
  </section>
</template>

<style src="../freshman.css"></style>
<style src="./events.css"></style>
