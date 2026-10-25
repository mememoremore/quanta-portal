<script setup>
import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Upload } from '@element-plus/icons-vue'
import PermissionButton from '@/components/PermissionButton.vue'
import MaterialFilters from '@/components/materials/MaterialFilters.vue'
import MaterialUploadDrawer from '@/components/materials/MaterialUploadDrawer.vue'
import MaterialFileIcon from '@/components/materials/MaterialFileIcon.vue'
import { batchRemoveMaterials, downloadMaterial, getMaterials, removeMaterial } from '@/api/materials'
import { useMaterialLibrary } from '@/composables/useMaterialLibrary'
import { useMaterialSelection } from '@/composables/useMaterialSelection'
import { saveBlob } from '@/utils/download'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'
import { getMaterialUploadPolicy, materialCategoryLabel } from '@/utils/material-categories'
import { materialRowTypeLabel, materialTypeLabel } from '@/utils/material-types'
import './learning-materials.css'

const library = useMaterialLibrary(getMaterials)
const { rows, total, pageNum, pageSize, category, materialType, keyword, searchText, loading, errorMessage, load, chooseCategory, chooseType, clearSearch, search, goToPage, showUploadTarget } = library
const userStore = useUserStore()
const { hasAny, hasRole } = usePermission()
const uploadOpen = ref(false)
const uploadPolicy = computed(() => getMaterialUploadPolicy({
  roles: userStore.roles, permissions: userStore.permissions,
  departmentCode: userStore.departmentCode, canAccessAdmin: userStore.canAccessAdmin,
}))
// Deliberately do not use hasRole('ceo'): its legacy alias also admits admin accounts.
const batchDeleteRoles = ['ceo', 'qt_mgmt', 'qt_manager']
const canBatchDelete = computed(() => batchDeleteRoles.some(role => userStore.roles.includes(role)) && hasAny('qt:material:remove'))
const canDelete = computed(() => (canBatchDelete.value || hasRole('admin')) && hasAny('qt:material:remove'))
const downloadingId = ref(null)
const mutationBusy = ref(false), batchSubmitting = ref(false)
const selectionLocked = computed(() => loading.value || mutationBusy.value)
const selectionContext = computed(() => JSON.stringify([category.value, materialType.value, keyword.value, pageNum.value]))
const { selectedIds, selectedRows, count: selectedCount, allSelected, indeterminate, toggle, toggleAll, clear: clearSelection } = useMaterialSelection(rows, { enabled: canBatchDelete, locked: selectionLocked, context: selectionContext })
let disposed = false
onBeforeUnmount(() => { disposed = true })

function fileSize(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return value || '未知大小'
  return number >= 1048576 ? `${(number / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.ceil(number / 1024))} KB`
}

async function handleDelete(row) {
  if (mutationBusy.value || loading.value || !canDelete.value) return
  mutationBusy.value = true
  try {
    await ElMessageBox.confirm(`确认删除“${row.fileName}”吗？`, '删除学习资料', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' })
    if (disposed) return
    if (!canDelete.value) throw new Error('当前账号无删除权限，请刷新后重试')
    await removeMaterial(row.materialId)
    if (disposed) return
    ElMessage.success('删除成功')
    clearSelection()
    await refreshAfterRemoval()
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除失败') }
  finally { mutationBusy.value = false }
}

async function refreshAfterRemoval() {
  await load()
  if (disposed) return
  if (!errorMessage.value && !rows.value.length && pageNum.value > 1) {
    pageNum.value = Math.min(pageNum.value - 1, Math.max(1, Math.ceil(total.value / pageSize)))
    await load()
  }
  if (errorMessage.value) ElMessage.warning('列表刷新失败，请点击重试核对资料。')
}

async function handleBatchDelete() {
  if (!canBatchDelete.value || mutationBusy.value || loading.value || !selectedCount.value) return
  const ids = [...selectedIds.value]
  const names = selectedRows.value.map(row => row.fileName)
  mutationBusy.value = true
  try {
    await ElMessageBox.confirm(h('div', { class: 'material-batch-confirm' }, [
      h('p', `即将删除以下 ${ids.length} 份资料，删除后无法恢复。`),
      h('ul', names.map((name, i) => h('li', { key: ids[i] }, name))),
    ]), '批量删除资料', {
      type: 'warning', confirmButtonText: `确认删除 ${ids.length} 份`, cancelButtonText: '取消',
      confirmButtonClass: 'material-batch-confirm__submit', closeOnClickModal: false,
    })
    if (disposed) return
    if (!canBatchDelete.value) throw new Error('当前账号无批量删除权限，请刷新后重试')
    batchSubmitting.value = true
    const result = await batchRemoveMaterials(ids)
    if (disposed) return
    clearSelection()
    ElMessage.success(`已删除 ${result.deletedIds.length} 份资料${result.alreadyDeletedIds.length ? `；另 ${result.alreadyDeletedIds.length} 份此前已删除` : ''}`)
    await refreshAfterRemoval()
  } catch (error) {
    if (disposed || error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || '批量删除失败，请重试')
    if (error.uncertain) { clearSelection(); await refreshAfterRemoval() }
  } finally {
    batchSubmitting.value = false
    mutationBusy.value = false
  }
}
async function handleDownload(row) {
  downloadingId.value = row.materialId
  try {
    const result = await downloadMaterial(row)
    saveBlob(result.blob, result.fileName)
    ElMessage.success('下载已开始')
  } catch (error) { ElMessage.error(error.message || '下载失败') }
  finally { downloadingId.value = null }
}
function uploaded(result) { clearSelection(); return showUploadTarget(result?.category, result?.type) }
onMounted(load)
</script>

<template>
  <div class="learning-materials-view">
    <header class="learning-materials-view__heading">
      <div><p>QUANTA KNOWLEDGE</p><h1>学习资料</h1><span>把经验归档，让知识在塔员之间流动。</span></div>
      <ElButton v-if="uploadPolicy.allowed" type="primary" :icon="Upload" :disabled="mutationBusy" data-testid="open-material-upload" @click="uploadOpen = true">上传资料</ElButton>
    </header>
    <p v-if="uploadPolicy.reason" class="learning-upload-warning" role="alert">{{ uploadPolicy.reason }}</p>
    <section class="page-card learning-list-card">
      <MaterialFilters v-model:search-text="searchText" :category="category" :material-type="materialType" :keyword="keyword" :disabled="mutationBusy" @category="chooseCategory" @type="chooseType" @search="search" @clear="clearSearch" />
      <div class="learning-materials-view__list-label"><strong>{{ materialTypeLabel(materialType) || (keyword ? '搜索结果' : '全部资料') }} <span>{{ total }}</span></strong><small>全部塔员均可查看</small></div>
      <div v-if="errorMessage" class="learning-list-card__error" role="alert"><span>{{ errorMessage }}</span><ElButton link type="primary" @click="load">重试</ElButton><ElButton link @click="chooseType('')">查看全部资料</ElButton></div>
      <template v-else>
        <div v-if="canBatchDelete" class="learning-materials-batch" aria-label="批量操作">
          <div class="learning-materials-batch__selection">
            <ElCheckbox :model-value="allSelected" :indeterminate="indeterminate" :disabled="selectionLocked || !rows.length" @change="toggleAll">全选本页</ElCheckbox>
            <span role="status" aria-live="polite">已选 {{ selectedCount }} 项</span>
            <ElButton v-if="selectedCount" link :disabled="selectionLocked" aria-label="取消选择" @click="clearSelection"><span class="learning-materials-desktop-label">取消选择</span><span class="learning-materials-mobile-label" aria-hidden="true">清空</span></ElButton>
          </div>
          <ElButton type="danger" plain :icon="Delete" :disabled="selectionLocked || !selectedCount" :loading="batchSubmitting" :aria-label="`批量删除${selectedCount ? `（${selectedCount}）` : ''}`" data-testid="material-batch-delete" @click="handleBatchDelete"><span class="learning-materials-desktop-label">批量</span>删除<span v-if="selectedCount">（{{ selectedCount }}）</span></ElButton>
        </div>
        <ElTable v-loading="loading" class="learning-materials-table" :data="rows" row-key="materialId" empty-text="该类型暂时没有资料">
          <ElTableColumn v-if="canBatchDelete" width="64" label="选择"><template #default="{ row }"><ElCheckbox :model-value="selectedIds.includes(row.materialId)" :disabled="selectionLocked" :aria-label="`选择资料：${row.fileName}`" @change="toggle(row.materialId, $event)" /></template></ElTableColumn>
          <ElTableColumn label="资料名称" min-width="280"><template #default="{ row }"><div class="learning-materials-table__name"><MaterialFileIcon :name="row.fileName" /><div><ElButton link class="learning-materials-table__download" @click="handleDownload(row)">{{ row.fileName }}</ElButton><small>{{ fileSize(row.fileSize) }} · {{ materialCategoryLabel(row.category) }}{{ materialRowTypeLabel(row, materialType) ? ' · ' + materialRowTypeLabel(row, materialType) : '' }}</small></div></div></template></ElTableColumn>
          <ElTableColumn prop="uploader" label="上传人" min-width="100" />
          <ElTableColumn prop="uploadedAt" label="上传时间" min-width="165" />
          <ElTableColumn label="操作" :min-width="canDelete ? 120 : 80"><template #default="{ row }"><ElButton link type="primary" :loading="downloadingId === row.materialId" @click="handleDownload(row)">下载</ElButton><PermissionButton v-if="canDelete" link type="danger" :disabled="selectionLocked" permissions="qt:material:remove" :roles="['qt_mgmt', 'qt_manager', 'ceo', 'admin']" @click="handleDelete(row)">删除</PermissionButton></template></ElTableColumn>
        </ElTable>
        <div v-loading="loading" class="learning-materials-mobile">
          <article v-for="row in rows" :key="row.materialId" :class="{ 'is-selected': selectedIds.includes(row.materialId) }">
            <ElCheckbox v-if="canBatchDelete" :model-value="selectedIds.includes(row.materialId)" :disabled="selectionLocked" :aria-label="`选择资料：${row.fileName}`" @change="toggle(row.materialId, $event)" />
            <div class="learning-materials-table__name"><MaterialFileIcon :name="row.fileName" /><div><strong>{{ row.fileName }}</strong><small>{{ fileSize(row.fileSize) }} · {{ row.uploader }}</small><small>{{ materialCategoryLabel(row.category) }}{{ materialRowTypeLabel(row, materialType) ? ' · ' + materialRowTypeLabel(row, materialType) : '' }}</small></div></div>
            <div class="learning-materials-mobile__actions"><ElButton link type="primary" :loading="downloadingId === row.materialId" @click="handleDownload(row)">下载</ElButton><PermissionButton v-if="canDelete" link type="danger" :disabled="selectionLocked" permissions="qt:material:remove" :roles="['qt_mgmt', 'qt_manager', 'ceo', 'admin']" @click="handleDelete(row)">删除</PermissionButton></div>
          </article>
          <p v-if="!rows.length && !loading" class="learning-materials-mobile__empty">该类型暂时没有资料</p>
        </div>
      </template>
      <footer v-if="!errorMessage" class="learning-list-card__footer"><span>共 {{ total }} 份资料</span><ElPagination :current-page="pageNum" :disabled="mutationBusy" background layout="prev, pager, next" :page-size="pageSize" :total="total" :pager-count="5" @current-change="goToPage" /></footer>
    </section>
    <MaterialUploadDrawer v-if="uploadPolicy.allowed" v-model="uploadOpen" :policy="uploadPolicy" :initial-category="category" :initial-type="materialType" @uploaded="uploaded" />
  </div>
</template>
