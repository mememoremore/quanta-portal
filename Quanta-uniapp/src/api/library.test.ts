import { describe, expect, it } from 'vitest'
import { mapBook } from './library'

describe('library backend mapping', () => {
  it('maps available inventory to the library grid', () => {
    expect(mapBook({ bookId: 8, isbn: '9787', bookName: '设计模式', locationDesc: '书架C-02', availableCount: 1, status: '0' })).toMatchObject({ id: '8', code: '008', cabinet: '书架C-02', status: 'available', category: 'general' })
  })

  it('marks textbook inventory as required', () => {
    expect(mapBook({ bookId: 9, availableCount: 2, status: '0', bookType: 'TEXTBOOK' })).toMatchObject({ category: 'textbook', required: true })
  })
})
