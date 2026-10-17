import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { useMaterialLibrary } from '../useMaterialLibrary'
import { getMaterialFolders } from '@/api/material-folders'

vi.mock('@/api/material-folders', () => ({ getMaterialFolders: vi.fn() }))

function library(fetcher = vi.fn().mockResolvedValue({ rows: [], total: 0 }), options) {
  let state
  const wrapper = mount(defineComponent({ setup() { state = useMaterialLibrary(fetcher, options); return () => null } }))
  return { state, fetcher, wrapper }
}

describe('material library state', () => {
  it('supports the original member all-departments tab without affecting admin defaults', async () => {
    const { state, fetcher } = library(undefined, { allowAll: true })
    await state.load()
    expect(fetcher).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 20 })
    await state.chooseCategory('FRONTEND')
    await state.chooseType('TRAINING')
    expect(fetcher.mock.lastCall[0].folderId).toBe(12)
    await state.chooseCategory('')
    expect(state.materialType.value).toBe('')
    expect(fetcher).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 20 })
  })
  beforeEach(() => {
    vi.clearAllMocks()
    getMaterialFolders.mockResolvedValue([{ folderId: 12, parentId: 0, category: 'FRONTEND', folderName: '培训资料' }])
  })

  it('loads all department files by default, including legacy root files', async () => {
    const { state, fetcher } = library()
    await state.load()
    expect(fetcher).toHaveBeenLastCalledWith({ category: 'FRONTEND', pageNum: 1, pageSize: 20 })
    expect(getMaterialFolders).not.toHaveBeenCalled()
  })

  it('clearing the input restores the full list without submitting again', async () => {
    const { state, fetcher } = library()
    state.searchText.value = 'Vue'
    await state.search()
    expect(fetcher).toHaveBeenLastCalledWith(expect.objectContaining({ fileName: 'Vue' }))
    state.searchText.value = ''
    await flushPromises()
    expect(state.keyword.value).toBe('')
    expect(fetcher).toHaveBeenLastCalledWith({ category: 'FRONTEND', pageNum: 1, pageSize: 20 })
  })

  it('searches across the department as contracted, then restores the previous type', async () => {
    const { state, fetcher } = library()
    await state.chooseType('TRAINING')
    expect(fetcher).toHaveBeenLastCalledWith(expect.objectContaining({ folderId: 12 }))
    state.searchText.value = 'Vue'
    await state.search()
    expect(fetcher.mock.lastCall[0]).not.toHaveProperty('folderId')
    expect(state.materialType.value).toBe('')
    await state.clearSearch()
    expect(state.materialType.value).toBe('TRAINING')
    expect(fetcher).toHaveBeenLastCalledWith({ category: 'FRONTEND', folderId: 12, pageNum: 1, pageSize: 20 })
  })

  it('a missing fixed type is empty, never another type or the whole department', async () => {
    const { state, fetcher } = library()
    await state.chooseType('PROJECT')
    expect(state.rows.value).toEqual([])
    expect(state.total.value).toBe(0)
    expect(fetcher).not.toHaveBeenCalled()
  })

  it('switching departments clears old search and resets pagination', async () => {
    const { state, fetcher } = library()
    state.searchText.value = 'Vue'
    await state.search()
    state.pageNum.value = 3
    await state.chooseCategory('BACKEND')
    expect(state.searchText.value).toBe('')
    expect(fetcher).toHaveBeenLastCalledWith({ category: 'BACKEND', pageNum: 1, pageSize: 20 })
  })

  it('does not let a late old response overwrite the current result', async () => {
    let finishOld
    const fetcher = vi.fn().mockImplementationOnce(() => new Promise(resolve => { finishOld = resolve }))
      .mockResolvedValue({ rows: [{ materialId: 2, category: 'BACKEND' }], total: 1 })
    const { state } = library(fetcher)
    const old = state.load()
    await state.chooseCategory('BACKEND')
    finishOld({ rows: [{ materialId: 1, category: 'FRONTEND' }], total: 1 })
    await old
    expect(state.rows.value[0].materialId).toBe(2)
  })

  it('surfaces folder API failures and can recover to the department list', async () => {
    getMaterialFolders.mockRejectedValueOnce(new Error('分类接口未部署'))
    const { state, fetcher } = library()
    await state.chooseType('TRAINING')
    expect(state.errorMessage.value).toBe('分类接口未部署')
    await state.chooseType('')
    expect(state.errorMessage.value).toBe('')
    expect(fetcher).toHaveBeenCalledTimes(1)
  })
})
