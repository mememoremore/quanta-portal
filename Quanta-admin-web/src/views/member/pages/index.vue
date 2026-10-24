<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { getMyPageSite, getPageDeployments, isDeploymentActive } from '@/api/portal/pages'
import SiteConfigForm from './components/SiteConfigForm.vue'
import DeploymentHistory from './components/DeploymentHistory.vue'
import DeploymentLogDialog from './components/DeploymentLogDialog.vue'

const POLL_INTERVAL = 5000

const site = ref(null)
const deployments = ref([])
const total = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const pollError = ref('')
const logDeployment = ref(null)
const logOpen = ref(false)

const siteUrl = computed(() => (site.value?.slug ? `https://${site.value.slug}.page.quantacenter.com` : ''))
const hasActiveDeployment = computed(() => deployments.value.some((row) => isDeploymentActive(row.status)))

async function refresh({ silent = false } = {}) {
  if (!silent) loading.value = true
  try {
    const owned = await getMyPageSite()
    site.value = owned
    pollError.value = ''
    if (owned?.siteId) {
      const page = await getPageDeployments(owned.siteId, { pageNum: 1, pageSize: 20 })
      deployments.value = page.rows
      total.value = page.total
    } else {
      deployments.value = []
      total.value = 0
    }
  } catch (error) {
    const message = error.message || '个人网站信息加载失败'
    if (silent) {
      pollError.value = message
    } else {
      errorMessage.value = message
    }
  } finally {
    loading.value = false
    schedulePolling()
  }
}

let pollTimer = null

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function schedulePolling() {
  stopPolling()
  if (pollError.value) return
  if (!site.value?.siteId || !hasActiveDeployment.value) return
  if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return
  pollTimer = setInterval(() => {
    refresh({ silent: true })
  }, POLL_INTERVAL)
}

function onVisibilityChange() {
  schedulePolling()
}

function onSaved(result) {
  if (result?.site) site.value = result.site
  refresh()
}

function openLog(row) {
  logDeployment.value = row
  logOpen.value = true
}

onMounted(async () => {
  await refresh()
  document.addEventListener('visibilitychange', onVisibilityChange)
  schedulePolling()
})

onBeforeUnmount(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <section class="member-page pages-page">
    <header class="pages-heading">
      <div><p>MY PAGES</p><h1>个人网站</h1><span>绑定 GitHub 公开仓库，推送即自动发布到独立子域名。</span></div>
      <a v-if="siteUrl" class="member-secondary-button pages-heading__link" :href="siteUrl" target="_blank" rel="noopener" data-testid="pages-visit-link">
        访问 {{ siteUrl.replace('https://', '') }}
      </a>
    </header>

    <p v-if="errorMessage" class="member-feedback is-error" role="alert" data-testid="pages-load-error">
      {{ errorMessage }} <button type="button" @click="refresh()">重新加载</button>
    </p>
    <p v-else-if="pollError" class="member-feedback is-error" role="alert" data-testid="pages-poll-error">
      状态刷新失败：{{ pollError }} <button type="button" @click="refresh({ silent: true })">重试</button>
    </p>

    <div v-if="loading" class="pages-state portal-card">正在加载站点信息…</div>

    <template v-else>
      <div v-if="!site" class="pages-guide portal-card" data-testid="pages-guide">
        <h2>还没有个人网站</h2>
        <p>
          创建后会得到 <code>https://你的标识.page.quantacenter.com</code>。
          平台只发布指定分支里<b>已经构建好的静态文件</b>，不执行任何构建脚本；
          分支默认 <code>pages</code>，发布目录默认仓库根目录。
        </p>
        <SiteConfigForm :site="null" @saved="onSaved" />
      </div>

      <template v-else>
        <article class="pages-site portal-card">
          <header>
            <div>
              <p>SITE</p>
              <h2>{{ siteUrl.replace('https://', '') }}</h2>
            </div>
            <div class="pages-site__meta">
              <span v-if="site.currentSha" data-testid="pages-current-sha">在线版本 {{ site.currentSha.slice(0, 7) }}</span>
              <span v-else data-testid="pages-never-published">尚未发布首个版本</span>
              <span v-if="site.lastCheckedTime" data-testid="pages-last-checked">最近检查 {{ site.lastCheckedTime }}</span>
            </div>
          </header>
          <p v-if="site.lastCheckError" class="member-feedback is-error" data-testid="pages-check-error">
            最近一次分支检查失败：{{ site.lastCheckError }}（在线版本不受影响）
          </p>
          <SiteConfigForm :key="site.siteId" :site="site" @saved="onSaved" />
        </article>

        <DeploymentHistory :site="site" :rows="deployments" :total="total" @changed="refresh()" @view-log="openLog" />
      </template>
    </template>

    <DeploymentLogDialog :open="logOpen" :deployment="logDeployment" @close="logOpen = false" />
  </section>
</template>

<style src="./pages.css"></style>
