import { effectScope, ref } from 'vue'
import { describe, expect, it } from 'vitest'
import { useMaterialSelection } from '../useMaterialSelection'

function setup() {
  const scope = effectScope()
  const rows = ref([{ materialId: 1 }, { materialId: 2 }])
  const enabled = ref(true), locked = ref(false), context = ref('FRONTEND::1')
  const selection = scope.run(() => useMaterialSelection(rows, { enabled, locked, context }))
  return { ...selection, scope, rows, enabled, locked, context }
}

describe('current-page material selection', () => {
  it('selects visible IDs only and shares all/partial state', () => {
    const s = setup()
    s.toggle(99, true)
    expect(s.count.value).toBe(0)
    s.toggle(1, true)
    expect(s.selectedRows.value).toEqual([{ materialId: 1 }])
    expect(s.indeterminate.value).toBe(true)
    s.toggleAll(true)
    expect(s.selectedIds.value).toEqual([1, 2])
    expect(s.allSelected.value).toBe(true)
    s.toggle(1, false)
    expect(s.selectedIds.value).toEqual([2])
    s.toggleAll(false)
    expect(s.count.value).toBe(0)
    s.scope.stop()
  })

  it('clears synchronously on context, refresh or privilege changes', () => {
    const s = setup()
    s.toggleAll(true)
    s.context.value = 'BACKEND::1'
    expect(s.count.value).toBe(0)
    s.toggleAll(true)
    s.rows.value = [{ materialId: 3 }]
    expect(s.count.value).toBe(0)
    s.toggle(3, true)
    s.enabled.value = false
    expect(s.count.value).toBe(0)
    s.toggleAll(true)
    expect(s.count.value).toBe(0)
    s.scope.stop()
  })

  it('locks changes while confirmation or deletion is in progress', () => {
    const s = setup()
    s.toggle(1, true)
    s.locked.value = true
    s.toggle(1, false)
    s.toggleAll(true)
    expect(s.selectedIds.value).toEqual([1])
    s.clear()
    expect(s.count.value).toBe(0)
    s.scope.stop()
  })
})
