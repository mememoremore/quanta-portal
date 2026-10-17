<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import PermissionButton from '@/components/PermissionButton.vue'
import MaterialFilters from '@/components/materials/MaterialFilters.vue'
import MaterialUploadDrawer from '@/components/materials/MaterialUploadDrawer.vue'
import { downloadMaterial, getMaterials, removeMaterial } from '@/api/materials'
import { useMaterialLibrary } from '@/composables/useMaterialLibrary'
import { saveBlob } from '@/utils/download'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'
import { getMaterialUploadPolicy, materialCategoryLabel } from '@/utils/material-categories'
import { materialRowTypeLabel, materialTypeLabel } from '@/utils/material-types'
import './learning-materials.css'

const library = useMaterialLibrary(getMaterials)
const { rows, total, pageNum, pageSize, category, materialType, keyword, searchText, loading, errorMessage, load, chooseCategory, chooseType, clearSearch, search, goToPage } = library
const userStore = useUserStore()
const { hasAny, hasRole } = usePermission()
const uploadOpen = ref(false)
const uploadPolicy = computed(() => getMaterialUploadPolicy({
  roles: userStore.roles, permissions: userStore.permissions,
  departmentCode: userStore.departmentCode, canAccessAdmin: userStore.canAccessAdmin,
}))
const canDelete = computed(() => (hasRole('qt_mgmt') || hasRole('ceo') || hasRole('admin')) && hasAny('qt:material:remove'))
const downloadingId = ref(null)

function fileSize(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return value || '未知大小'
  return number >= 1048576 ? `${(number / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.ceil(number / 1024))} KB`
}
function fileType(name) { return String(name || '').split('.').pop()?.slice(0, 4).toUpperCase() || 'DOC' }

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除“${row.fileName}”吗？`, '删除学习资料', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' })
    await removeMaterial(row.materialId)
    ElMessage.success('删除成功')
    if (rows.value.length === 1 && pageNum.value > 1) pageNum.value -= 1
    await load()
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除失败') }
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
function uploaded() { pageNum.value = 1; load() }
onMounted(load)
</script>

<template>
  <div class="learning-materials-view">
    <header class="learning-materials-view__heading">
      <div><p>QUANTA KNOWLEDGE</p><h1>学习资料</h1><span>把经验归档，让知识在塔员之间流动。</span></div>
      <ElButton v-if="uploadPolicy.allowed" type="primary" :icon="Upload" data-testid="open-material-upload" @click="uploadOpen = true">上传资料</ElButton>
    </header>
    <p v-if="uploadPolicy.reason" class="learning-upload-warning" role="alert">{{ uploadPolicy.reason }}</p>
    <section class="page-card learning-list-card">
      <MaterialFilters v-model:search-text="searchText" :category="category" :material-type="materialType" :keyword="keyword" @category="chooseCategory" @type="chooseType" @search="search" @clear="clearSearch" />
      <div class="learning-materials-view__list-label"><strong>{{ materialTypeLabel(materialType) || (keyword ? '搜索结果' : '全部资料') }} <span>{{ total }}</span></strong><small>全部塔员均可查看</small></div>
      <div v-if="errorMessage" class="learning-list-card__error" role="alert"><span>{{ errorMessage }}</span><ElButton link type="primary" @click="load">重试</ElButton><ElButton link @click="chooseType('')">查看全部资料</ElButton></div>
      <template v-else>
        <ElTable v-loading="loading" class="learning-materials-table" :data="rows" row-key="materialId" empty-text="该类型暂时没有资料">
          <ElTableColumn label="资料名称" min-width="280"><template #default="{ row }"><div class="learning-materials-table__name"><span class="learning-materials-table__icon">{{ fileType(row.fileName) }}</span><div><ElButton link class="learning-materials-table__download" @click="handleDownload(row)">{{ row.fileName }}</ElButton><small>{{ fileSize(row.fileSize) }} · {{ materialCategoryLabel(row.category) }}{{ materialRowTypeLabel(row, materialType) ? ' · ' + materialRowTypeLabel(row, materialType) : '' }}</small></div></div></template></ElTableColumn>
          <ElTableColumn prop="uploader" label="上传人" min-width="100" />
          <ElTableColumn prop="uploadedAt" label="上传时间" min-width="165" />
          <ElTableColumn label="操作" :min-width="canDelete ? 120 : 80"><template #default="{ row }"><ElButton link type="primary" :loading="downloadingId === row.materialId" @click="handleDownload(row)">下载</ElButton><PermissionButton v-if="canDelete" link type="danger" permissions="qt:material:remove" :roles="['qt_mgmt', 'ceo', 'admin']" @click="handleDelete(row)">删除</PermissionButton></template></ElTableColumn>
        </ElTable>
        <div v-loading="loading" class="learning-materials-mobile">
          <article v-for="row in rows" :key="row.materialId"><div class="learning-materials-table__name"><span class="learning-materials-table__icon">{{ fileType(row.fileName) }}</span><div><strong>{{ row.fileName }}</strong><small>{{ fileSize(row.fileSize) }} · {{ row.uploader }}</small><small>{{ materialCategoryLabel(row.category) }}{{ materialRowTypeLabel(row, materialType) ? ' · ' + materialRowTypeLabel(row, materialType) : '' }}</small></div></div><div class="learning-materials-mobile__actions"><ElButton link type="primary" :loading="downloadingId === row.materialId" @click="handleDownload(row)">下载</ElButton><PermissionButton v-if="canDelete" link type="danger" permissions="qt:material:remove" :roles="['qt_mgmt', 'ceo', 'admin']" @click="handleDelete(row)">删除</PermissionButton></div></article>
          <p v-if="!rows.length && !loading" class="learning-materials-mobile__empty">该类型暂时没有资料</p>
        </div>
      </template>
      <footer v-if="!errorMessage" class="learning-list-card__footer"><span>共 {{ total }} 份资料</span><ElPagination :current-page="pageNum" background layout="prev, pager, next" :page-size="pageSize" :total="total" :pager-count="5" @current-change="goToPage" /></footer>
    </section>
    <MaterialUploadDrawer v-if="uploadPolicy.allowed" v-model="uploadOpen" :policy="uploadPolicy" :initial-category="category" :initial-type="materialType" @uploaded="uploaded" />
  </div>
</template>
