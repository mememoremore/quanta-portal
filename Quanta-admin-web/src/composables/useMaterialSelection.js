import { computed, ref, watch } from 'vue'

// Desktop and mobile controls share this current-page selection; never retain hidden targets.
export function useMaterialSelection(rows, { enabled, locked, context }) {
  const selected = ref(new Set())
  const selectedRows = computed(() => rows.value.filter(row => selected.value.has(row.materialId)))
  const selectedIds = computed(() => selectedRows.value.map(row => row.materialId))
  const count = computed(() => selectedRows.value.length)
  const allSelected = computed(() => rows.value.length > 0 && count.value === rows.value.length)
  const indeterminate = computed(() => count.value > 0 && !allSelected.value)

  function clear() { selected.value = new Set() }
  function toggle(id, checked) {
    if (!enabled.value || locked.value || !rows.value.some(row => row.materialId === id)) return
    const next = new Set(selected.value)
    if (checked) next.add(id)
    else next.delete(id)
    selected.value = next
  }
  function toggleAll(checked) {
    if (!enabled.value || locked.value) return
    selected.value = new Set(checked ? rows.value.map(row => row.materialId) : [])
  }
  watch([rows, enabled, context], clear, { flush: 'sync' })
  return { selectedIds, selectedRows, count, allSelected, indeterminate, toggle, toggleAll, clear }
}
