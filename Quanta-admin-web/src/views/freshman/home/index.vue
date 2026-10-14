<script setup>
import { onMounted, ref } from 'vue'
import { getPublishedActivities } from '@/api/portal/activities'
import { getMyApplication } from '@/api/portal/recruitment'

const application = ref(null)
const activities = ref([])
const loading = ref(true)
const errorMessage = ref('')

async function loadHome() {
  try {
    const [applicationData, activityRows] = await Promise.all([
      getMyApplication(),
      getPublishedActivities(),
    ])
    application.value = applicationData
    activities.value = activityRows.slice(0, 2)
  } catch (error) {
    errorMessage.value = error.message || '首页信息加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>

<template>
  <section class="freshman-home">
    <div class="freshman-home__hero">
      <div>
        <p class="freshman-eyebrow">WELCOME TO QUANTA</p>
        <h1>让想法被看见，<br />让创造真正发生。</h1>
        <p>Quanta 是一个由学生共同建设的数字创新社群。</p>
        <RouterLink class="portal-primary-button freshman-home__cta" to="/freshman/recruitment">加入我们</RouterLink>
      </div>
      <div class="freshman-home__mark" aria-hidden="true">Q</div>
    </div>

    <p v-if="errorMessage" class="freshman-feedback is-error" role="alert">{{ errorMessage }}</p>
    <div v-if="loading" class="freshman-loading portal-card">正在加载首页…</div>
    <div v-else class="freshman-home__dashboard">
      <article class="freshman-home__status portal-card">
        <p class="freshman-eyebrow">APPLICATION</p>
        <h2>{{ application ? '报名进度' : '开启你的 Quanta 旅程' }}</h2>
        <p>{{ application ? `当前状态：${application.applyStatus || '处理中'}` : '选择感兴趣的方向，提交你的报名表。' }}</p>
        <RouterLink to="/freshman/recruitment">{{ application ? '查看面试进度' : '前往报名' }} →</RouterLink>
      </article>
      <article class="freshman-home__activities portal-card">
        <div class="freshman-home__section-title">
          <div><p class="freshman-eyebrow">UPCOMING</p><h2>近期活动</h2></div>
          <RouterLink to="/freshman/events">查看全部</RouterLink>
        </div>
        <ul v-if="activities.length">
          <li v-for="activity in activities" :key="activity.activityId">
            <time>{{ activity.activityStart || '待定' }}</time>
            <div><strong>{{ activity.title }}</strong><span>{{ activity.locationDesc || '地点待通知' }}</span></div>
          </li>
        </ul>
        <p v-else class="freshman-home__empty">暂无已发布活动</p>
      </article>
    </div>
  </section>
</template>

<style src="../freshman.css"></style>
<style src="./home.css"></style>
