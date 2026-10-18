import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { batchRemoveMaterials } from '../materials'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('material batch delete contract', () => {
  beforeEach(() => vi.resetAllMocks())

  it('uses one dedicated POST and accepts complete, disjoint result IDs', async () => {
    request.mockResolvedValue({ code: 200, data: { deletedIds: [1], alreadyDeletedIds: [2] } })
    expect(await batchRemoveMaterials([1, 2])).toEqual({ deletedIds: [1], alreadyDeletedIds: [2] })
    expect(request).toHaveBeenCalledExactlyOnceWith({ url: '/qt/materials/batch-delete', method: 'post', data: { materialIds: [1, 2] } })
  })

  it.each([[], [1, 1], [0], [-1], ['1'], [1.2], [Number.MAX_SAFE_INTEGER + 1], Array.from({ length: 21 }, (_, i) => i + 1)].map(ids => ({ ids })))('rejects invalid target IDs: $ids', async ({ ids }) => {
    await expect(batchRemoveMaterials(ids)).rejects.toThrow('请选择当前页内有效的资料（最多 20 份）')
    expect(request).not.toHaveBeenCalled()
  })

  it.each([undefined, {}, { deletedIds: [1] }, { deletedIds: [1], alreadyDeletedIds: [] }, { deletedIds: [1, 2, 3], alreadyDeletedIds: [] }, { deletedIds: [1, 2], alreadyDeletedIds: [2] }, { deletedIds: ['1', 2], alreadyDeletedIds: [] }])('never reports an incomplete or unexpected response as success: %j', async data => {
    request.mockResolvedValue({ code: 200, data })
    await expect(batchRemoveMaterials([1, 2])).rejects.toMatchObject({ uncertain: true })
  })

  it.each([404, 405, 501])('explains an unavailable endpoint (%i), with no single-delete fallback', async code => {
    request.mockRejectedValue(Object.assign(new Error('Not implemented'), { code }))
    await expect(batchRemoveMaterials([1])).rejects.toThrow('批量删除接口尚未接通')
    expect(request).toHaveBeenCalledTimes(1)
  })

  it('preserves a server permission error', async () => {
    request.mockRejectedValue(Object.assign(new Error('仅 CEO 可执行该操作'), { code: 403 }))
    await expect(batchRemoveMaterials([1])).rejects.toMatchObject({ code: 403, message: '仅 CEO 可执行该操作' })
  })

  it('marks transport failures as unconfirmed, never as a successful delete', async () => {
    request.mockRejectedValue(Object.assign(new Error('timeout'), { code: 0 }))
    await expect(batchRemoveMaterials([1])).rejects.toMatchObject({ uncertain: true })
  })
})
