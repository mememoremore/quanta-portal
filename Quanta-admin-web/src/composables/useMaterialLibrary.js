import { onBeforeUnmount, ref, watch } from 'vue'
import { getMaterialFolders } from '@/api/material-folders'
import { isMaterialCategory } from '@/utils/material-categories'
import { materialFolderForType } from '@/utils/material-types'

export function useMaterialLibrary(fetchMaterials, { allowAll = false } = {}) {
  const rows = ref([]), total = ref(0), pageNum = ref(1)
  const category = ref(allowAll ? '' : 'FRONTEND'), materialType = ref('')
  const keyword = ref(''), searchText = ref(''), loading = ref(false), errorMessage = ref('')
  const pageSize = 20
  let sequence = 0, previousType = ''

  async function load() {
    const current = ++sequence
    const params = { ...(category.value ? { category: category.value } : {}), pageNum: pageNum.value, pageSize }
    const type = materialType.value, term = keyword.value
    loading.value = true
    errorMessage.value = ''
    try {
      if (term) params.fileName = term
      else if (type) {
        const folder = materialFolderForType(await getMaterialFolders(params.category), type)
        if (current !== sequence) return
        if (!folder) { rows.value = []; total.value = 0; return }
        params.folderId = folder.folderId
      }
      const result = await fetchMaterials(params)
      if (current !== sequence) return
      rows.value = result.rows.filter(row => isMaterialCategory(row.category))
      total.value = result.total
    } catch (error) {
      if (current !== sequence) return
      rows.value = []
      total.value = 0
      errorMessage.value = error.message || '学习资料加载失败'
    } finally {
      if (current === sequence) loading.value = false
    }
  }

  function chooseCategory(value) {
    if (!isMaterialCategory(value) && !(allowAll && value === '')) return
    category.value = value
    if (!value) materialType.value = ''
    searchText.value = keyword.value = ''
    previousType = ''
    pageNum.value = 1
    return load()
  }

  function chooseType(value) {
    if (value && !category.value) return
    materialType.value = value
    previousType = ''
    searchText.value = keyword.value = ''
    pageNum.value = 1
    return load()
  }

  function clearSearch() {
    searchText.value = ''
    if (keyword.value) materialType.value = previousType
    keyword.value = ''
    previousType = ''
    pageNum.value = 1
    return load()
  }

  function search() {
    const term = searchText.value.trim()
    if (!term) return clearSearch()
    if (!keyword.value) previousType = materialType.value
    keyword.value = term
    // Backend fileName search explicitly ignores folderId, so show its true scope.
    materialType.value = ''
    pageNum.value = 1
    return load()
  }

  function goToPage(next) { pageNum.value = next; return load() }
  watch(searchText, value => { if (!value.trim() && keyword.value) clearSearch() })
  onBeforeUnmount(() => { sequence += 1 })
  return { rows, total, pageNum, pageSize, category, materialType, keyword, searchText, loading, errorMessage, load, chooseCategory, chooseType, clearSearch, search, goToPage }
}
