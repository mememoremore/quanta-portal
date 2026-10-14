import { describe, expect, it } from 'vitest'
import {
  getMaterialUploadPolicy, materialCategoryLabel, materialFileError, MATERIAL_MAX_SIZE,
} from '../material-categories'

describe('learning material rules', () => {
  it('locks a department manager to their own category', () => {
    expect(getMaterialUploadPolicy({
      roles: ['qt_mgmt'], permissions: ['qt:material:add'], departmentCode: 'FRONTEND',
    })).toMatchObject({ allowed: true, canChoose: false, category: 'FRONTEND' })
    expect(materialCategoryLabel('FRONTEND')).toBe('前端')
  })

  it('lets CEO choose and rejects unknown department', () => {
    expect(getMaterialUploadPolicy({ roles: ['ceo'], permissions: ['qt:material:add'] }).canChoose).toBe(true)
    expect(getMaterialUploadPolicy({ roles: ['qt_mgmt'], permissions: ['qt:material:add'] }).allowed).toBe(false)
  })

  it('matches the backend-supported file types and 20 MiB limit', () => {
    expect(materialFileError({ name: 'guide.docx', size: MATERIAL_MAX_SIZE })).toBe('')
    expect(materialFileError({ name: 'guide.md', size: 10 })).toContain('仅支持')
    expect(materialFileError({ name: 'guide.pdf', size: MATERIAL_MAX_SIZE + 1 })).toContain('20 MB')
  })
})
