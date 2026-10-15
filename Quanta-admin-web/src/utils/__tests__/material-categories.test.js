import { describe, expect, it } from 'vitest'
import {
  getMaterialUploadPolicy, materialCategoryLabel, materialFileError, MATERIAL_MAX_SIZE,
} from '../material-categories'

describe('learning material rules', () => {
  it('locks a department manager to their own category', () => {
    expect(getMaterialUploadPolicy({
      roles: ['qt_mgmt'], departmentCode: 'FRONTEND',
    })).toMatchObject({ allowed: true, canChoose: false, category: 'FRONTEND' })
    expect(materialCategoryLabel('FRONTEND')).toBe('前端')
  })

  it('allows every backoffice role without a separate upload permission', () => {
    expect(getMaterialUploadPolicy({ roles: ['qt_manager'], departmentCode: 'PRODUCT' })).toMatchObject({ allowed: true, category: 'PRODUCT' })
    expect(getMaterialUploadPolicy({ roles: ['ceo'] }).canChoose).toBe(true)
    expect(getMaterialUploadPolicy({ roles: ['admin'] }).canChoose).toBe(true)
  })

  it('rejects ordinary members and unknown manager departments', () => {
    expect(getMaterialUploadPolicy({ roles: ['qt_member'], departmentCode: 'FRONTEND' }).allowed).toBe(false)
    expect(getMaterialUploadPolicy({ roles: ['qt_member'], permissions: ['qt:material:add'] }).allowed).toBe(false)
    expect(getMaterialUploadPolicy({ roles: ['qt_mgmt'] }).allowed).toBe(false)
  })

  it('matches the backend-supported file types and 20 MiB limit', () => {
    expect(materialFileError({ name: 'guide.docx', size: MATERIAL_MAX_SIZE })).toBe('')
    expect(materialFileError({ name: 'guide.md', size: 10 })).toBe('')
    expect(materialFileError({ name: 'guide.exe', size: 10 })).toContain('仅支持')
    expect(materialFileError({ name: 'guide.pdf', size: MATERIAL_MAX_SIZE + 1 })).toContain('20 MB')
  })
})
