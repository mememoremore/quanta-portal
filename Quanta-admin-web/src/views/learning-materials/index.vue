<script setup>
import { computed, onMounted, ref } from 'vue'
import { FolderOpened } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PermissionButton from '@/components/PermissionButton.vue'
import { downloadMaterial, getMaterials, removeMaterial, uploadMaterial } from '@/api/materials'
import { saveBlob } from '@/utils/download'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'
import {
  MATERIAL_CATEGORIES, MATERIAL_EXTENSIONS, getMaterialUploadPolicy,
  isMaterialCategory, materialCategoryLabel, materialFileError,
} from '@/utils/material-categories'
import './learning-materials.css'

const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const category = ref('')
const keyword = ref('')
const searchText = ref('')
const uploadCategory = ref('')
const uploadRef = ref(null)
const selectedFile = ref(null)
const loading = ref(false)
const failed = ref(false)
const uploading = ref(false)
const userStore = useUserStore()
const { hasAny, hasRole } = usePermission()
const uploadPolicy = computed(() => getMaterialUploadPolicy({
  roles: userStore.roles,
  permissions: userStore.permissions,
  departmentCode: userStore.departmentCode,
  canAccessAdmin: userStore.canAccessAdmin,
}))
const canDelete = computed(() =>
  (hasRole('qt_mgmt') || hasRole('ceo') || hasRole('admin')) && hasAny('qt:material:remove'))
const selectedUploadCategory = computed(() => uploadPolicy.value.canChoose
  ? uploadCategory.value
  : uploadPolicy.value.category)
const visibilityLabels = { ALL: '所有人可见', MEMBER: '仅塔员可见', DEPT: '仅本部门可见' }

async function loadList() {
  loading.value = true
  failed.value = false
  try {
    const result = await getMaterials({
      pageNum: pageNum.value,
      pageSize,
      ...(category.value ? { category: category.value } : {}),
      ...(keyword.value ? { fileName: keyword.value } : {}),
    })
    rows.value = result.rows.filter((row) => isMaterialCategory(row.category))
    total.value = result.total
  } catch (error) {
    failed.value = true
    rows.value = []
    ElMessage.error(error.message || '学习资料加载失败')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  keyword.value = searchText.value.trim()
  pageNum.value = 1
  loadList()
}

function changeCategory(value) {
  category.value = value
  pageNum.value = 1
  loadList()
}

function clearSelectedFile() {
  selectedFile.value = null
  uploadRef.value?.clearFiles()
}

function selectFile(uploadFile) {
  const file = uploadFile?.raw
  if (!file) return
  const error = materialFileError(file)
  if (error) {
    ElMessage.warning(error)
    clearSelectedFile()
    return
  }
  selectedFile.value = file
}

function replaceFile(files) {
  const file = files?.[0]
  if (!file) return
  uploadRef.value?.clearFiles()
  uploadRef.value?.handleStart(file)
}

function fileSize(size) {
  return size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.ceil(size / 1024))} KB`
}

async function confirmUpload() {
  if (uploading.value || !uploadPolicy.value.allowed || !selectedUploadCategory.value || !selectedFile.value) return
  uploading.value = true
  try {
    await uploadMaterial(selectedFile.value, { category: selectedUploadCategory.value })
    ElMessage.success('上传成功')
    clearSelectedFile()
    pageNum.value = 1
    await loadList()
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除“${row.fileName}”吗？`, '删除学习资料', {
      type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消',
    })
    await removeMaterial(row.materialId)
    ElMessage.success('删除成功')
    await loadList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除失败')
  }
}

async function handleDownload(row) {
  try {
    const result = await downloadMaterial(row)
    saveBlob(result.blob, result.fileName)
    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.error(error.message || '下载失败')
  }
}

onMounted(loadList)
</script>

<template>
  <div class="learning-materials-view">
    <section v-if="uploadPolicy.allowed" class="page-card learning-upload-card">
      <header><h2>上传学习资料</h2><span>分享给全部塔员 · 单文件不超过 20 MB</span></header>
      <div class="learning-upload-card__category">
        <span>资料分类</span>
        <ElSelect v-if="uploadPolicy.canChoose" v-model="uploadCategory" placeholder="选择资料所属部门" aria-label="资料所属部门">
          <ElOption v-for="item in MATERIAL_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
        </ElSelect>
        <strong v-else>{{ materialCategoryLabel(uploadPolicy.category) }}</strong>
      </div>
      <ElUpload ref="uploadRef" drag action="#" :auto-upload="false" :show-file-list="false" :limit="1" :on-change="selectFile" :on-exceed="replaceFile" :disabled="uploading || !selectedUploadCategory" :accept="MATERIAL_EXTENSIONS.map((item) => `.${item}`).join(',')">
        <ElIcon class="learning-upload-card__icon"><FolderOpened /></ElIcon>
        <p>{{ selectedFile ? '重新选择文件，或拖拽文件替换' : '点击或拖拽文件到此处选择' }}</p>
        <span>PDF、Word、PPT、Excel、TXT、Markdown、图片 · 最大 20 MB</span>
      </ElUpload>
      <div v-if="selectedFile" class="learning-upload-card__selected" role="status">
        <span class="learning-upload-card__file-icon" aria-hidden="true">{{ selectedFile.name.split('.').pop()?.toUpperCase() }}</span>
        <div class="learning-upload-card__file-info"><strong :title="selectedFile.name">{{ selectedFile.name }}</strong><span>{{ fileSize(selectedFile.size) }} · {{ materialCategoryLabel(selectedUploadCategory) }} · 待上传</span></div>
        <button type="button" :disabled="uploading" @click="clearSelectedFile">移除</button>
      </div>
      <div class="learning-upload-card__actions"><ElButton type="primary" class="quanta-primary-button" data-testid="confirm-material-upload" :loading="uploading" :disabled="!selectedFile || !selectedUploadCategory" @click="confirmUpload">确认上传</ElButton></div>
    </section>
    <p v-else-if="uploadPolicy.reason" class="learning-upload-warning" role="alert">{{ uploadPolicy.reason }}</p>

    <section class="page-card learning-list-card">
      <header><h2>资料列表</h2><span>按分类浏览和下载</span></header>
      <form class="learning-list-card__filters" @submit.prevent="applyFilters">
        <ElSelect :model-value="category" aria-label="筛选资料分类" @update:model-value="changeCategory">
          <ElOption label="全部" value="" />
          <ElOption v-for="item in MATERIAL_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
        </ElSelect>
        <ElInput v-model="searchText" clearable placeholder="搜索文件名称" aria-label="搜索文件名称" />
        <ElButton native-type="submit" type="primary">搜索</ElButton>
      </form>
      <div v-if="failed && !loading" class="learning-list-card__error"><span>资料暂时无法加载。</span><ElButton type="primary" link @click="loadList">重新加载</ElButton></div>
      <ElTable v-else v-loading="loading" class="learning-materials-table" :data="rows" size="small" row-key="materialId" empty-text="暂无学习资料">
        <ElTableColumn prop="fileName" label="文件名" min-width="260"><template #default="{ row }"><ElButton link class="learning-materials-table__download" @click="handleDownload(row)">{{ row.fileName }}</ElButton></template></ElTableColumn>
        <ElTableColumn label="分类" min-width="100"><template #default="{ row }"><span class="learning-materials-table__tag">{{ materialCategoryLabel(row.category) }}</span></template></ElTableColumn>
        <ElTableColumn prop="fileSize" label="大小" min-width="100" />
        <ElTableColumn prop="uploader" label="上传者" min-width="120" />
        <ElTableColumn prop="uploadedAt" label="上传时间" min-width="170" />
        <ElTableColumn label="可见性" min-width="130"><template #default="{ row }"><span class="learning-materials-table__visibility" :class="{ 'is-restricted': row.visibility !== 'ALL' }">{{ visibilityLabels[row.visibility] || row.visibility }}</span></template></ElTableColumn>
        <ElTableColumn v-if="canDelete" label="操作" min-width="90"><template #default="{ row }"><PermissionButton link type="danger" permissions="qt:material:remove" :roles="['qt_mgmt', 'ceo', 'admin']" @click="handleDelete(row)">删除</PermissionButton></template></ElTableColumn>
      </ElTable>
      <ElPagination v-if="total > pageSize" v-model:current-page="pageNum" class="learning-list-card__pagination" background layout="prev, pager, next, total" :page-size="pageSize" :total="total" @current-change="loadList" />
    </section>
  </div>
</template>
