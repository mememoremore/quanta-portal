<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deployPageSite, isDeploymentActive, rollbackPageSite } from '@/api/portal/pages'

const props = defineProps({
  site: { type: Object, required: true },
  rows: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
})
const emit = defineEmits(['changed', 'view-log'])

const working = ref(false)

const statusLabels = {
  PENDING: '排队中', RESOLVING: '解析分支', DOWNLOADING: '下载归档',
  VALIDATING: '校验产物', PUBLISHING: '发布中', SUCCESS: '发布成功',
  FAILED: '发布失败', SUPERSEDED: '已被新目标取代',
}
const triggerLabels = {
  CREATE: '首次创建', CONFIG: '配置更新', POLL: '自动检查', MANUAL: '手动部署', ROLLBACK: '回滚',
}

function shortSha(sha) {
  return sha ? sha.slice(0, 7) : '待解析'
}

function isCurrent(row) {
  return props.site.currentDeploymentId === row.deploymentId
}

function canRollback(row) {
  return row.status === 'SUCCESS' && row.releaseAvailable && !isCurrent(row)
}

async function runAction(action) {
  working.value = true
  try {
    await action()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '操作失败，请稍后重试')
  } finally {
    working.value = false
  }
}

function deploy() {
  runAction(async () => {
    await deployPageSite(props.site.siteId)
    ElMessage.success('已排队部署分支最新提交')
  })
}

function rollback(row) {
  ElMessageBox.confirm(
    `确认回滚到 ${shortSha(row.commitSha)}？回滚不重新下载，下一次新提交仍会自动部署。`,
    '回滚确认',
    { type: 'warning' },
  ).then(() => runAction(async () => {
    await rollbackPageSite(props.site.siteId, row.deploymentId)
    ElMessage.success('回滚已排队，稍后生效')
  })).catch(() => {})
}
</script>

<template>
  <div class="pages-history" data-testid="pages-history">
    <header>
      <h2>部署历史</h2>
      <button
        class="member-primary-button"
        type="button"
        :disabled="working"
        data-testid="pages-deploy-button"
        @click="deploy"
      >
        部署最新提交
      </button>
    </header>
    <p v-if="rows.some((row) => isDeploymentActive(row.status))" class="member-feedback" data-testid="pages-active-hint">
      部署进行中；旧版本继续提供访问，完成后自动切换。
    </p>
    <ol class="pages-history__list">
      <li v-for="row in rows" :key="row.deploymentId" data-testid="pages-deployment-row">
        <div class="pages-history__main">
          <strong>
            {{ shortSha(row.commitSha) }}
            <span v-if="isCurrent(row)" class="pages-history__tag is-current">当前在线</span>
            <span class="pages-history__tag" :class="{ 'is-failed': row.status === 'FAILED' }">{{ statusLabels[row.status] || row.status }}</span>
          </strong>
          <small>
            {{ triggerLabels[row.triggerType] || row.triggerType }} · 分支 {{ row.branch }} · 目录 {{ row.publishDir }}
            <template v-if="row.createTime"> · {{ row.createTime }}</template>
          </small>
          <p v-if="row.errorMessage" class="pages-history__error" data-testid="pages-row-error">{{ row.errorMessage }}</p>
        </div>
        <div class="pages-history__actions">
          <button class="member-secondary-button" type="button" @click="emit('view-log', row)">查看日志</button>
          <button
            class="member-secondary-button"
            type="button"
            :disabled="!canRollback(row) || working"
            data-testid="pages-rollback-button"
            @click="rollback(row)"
          >
            回滚到此版本
          </button>
        </div>
      </li>
    </ol>
    <p v-if="!rows.length" class="member-empty">还没有部署记录</p>
  </div>
</template>
