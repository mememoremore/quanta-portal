import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { ensureMaterialTypeFolder, getMaterialFolders } from '../material-folders'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))
const folder = { folderId: 42, parentId: 0, category: 'FRONTEND', folderName: '培训资料' }

describe('fixed material type folder adapter', () => {
  beforeEach(() => vi.resetAllMocks())
  it('only reads root folders of the selected department', async () => {
    request.mockResolvedValue({ data: [folder, { ...folder, category: 'BACKEND' }, { ...folder, parentId: 7 }] })
    expect(await getMaterialFolders('FRONTEND')).toEqual([folder])
    expect(request).toHaveBeenCalledWith({ url: '/qt/materials/folders', method: 'get', params: { category: 'FRONTEND', parentId: 0 } })
  })
  it('reuses a matching existing folder without any write', async () => {
    request.mockResolvedValue({ data: [folder] })
    expect(await ensureMaterialTypeFolder('FRONTEND', 'TRAINING')).toBe(42)
    expect(request).toHaveBeenCalledTimes(1)
  })
  it('creates a missing type only on upload and re-reads the ID', async () => {
    request.mockResolvedValueOnce({ data: [] }).mockResolvedValueOnce({ code: 200 }).mockResolvedValueOnce({ data: [folder] })
    expect(await ensureMaterialTypeFolder('FRONTEND', 'TRAINING')).toBe(42)
    expect(request).toHaveBeenNthCalledWith(2, { url: '/qt/materials/folders', method: 'post', data: { category: 'FRONTEND', parentId: 0, folderName: '培训资料' } })
  })
  it('recovers if another uploader concurrently creates the same type', async () => {
    request.mockResolvedValueOnce({ data: [] }).mockRejectedValueOnce(new Error('同级重名')).mockResolvedValueOnce({ data: [folder] })
    expect(await ensureMaterialTypeFolder('FRONTEND', 'TRAINING')).toBe(42)
  })
  it('keeps permission errors when no matching folder was created', async () => {
    request.mockResolvedValueOnce({ data: [] }).mockRejectedValueOnce(new Error('没有权限')).mockResolvedValueOnce({ data: [] })
    await expect(ensureMaterialTypeFolder('FRONTEND', 'TRAINING')).rejects.toThrow('没有权限')
  })
  it('rejects unrecognized departments and types without sending requests', async () => {
    await expect(ensureMaterialTypeFolder('OTHER', 'TRAINING')).rejects.toThrow()
    await expect(ensureMaterialTypeFolder('FRONTEND', 'CUSTOM')).rejects.toThrow()
    expect(request).not.toHaveBeenCalled()
  })
})
