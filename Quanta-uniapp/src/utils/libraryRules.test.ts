import { describe, expect, it } from 'vitest'
import { buildBorrowConfirmationCopy, buildPaginationItems, calculateBookDueDate, filterLibraryBooks, paginate } from './libraryRules'

const format = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

describe('book due date', () => {
  it('adds category months and clamps to the last day of the target month', () => {
    expect(format(calculateBookDueDate(new Date(2026, 0, 31), 'general'))).toBe('2026-04-30')
    expect(format(calculateBookDueDate(new Date(2026, 7, 29), 'textbook'))).toBe('2027-01-29')
  })

  it('builds the confirmation copy before borrowing', () => {
    expect(buildBorrowConfirmationCopy('065', 'textbook', new Date(2026, 7, 29))).toBe(
      '确认借阅「编号065」教材类书籍吗？借阅后请在2027年1月29日前归还。',
    )
  })
})

describe('library search and pagination', () => {
  const books = [
    { code: '051', cabinet: 'A-02' },
    { code: '052', cabinet: 'a-02' },
    { code: '105', cabinet: 'B-01' },
  ]

  it('searches book code or cabinet and ignores casing and outer whitespace', () => {
    expect(filterLibraryBooks(books, ' A-02 ')).toHaveLength(2)
    expect(filterLibraryBooks(books, '051').map((book) => book.code)).toEqual(['051'])
    expect(filterLibraryBooks(books, '')).toHaveLength(3)
  })

  it('paginates 45 books and clamps invalid page numbers', () => {
    const values = Array.from({ length: 95 }, (_, index) => index + 1)
    expect(paginate(values, 2)).toMatchObject({ page: 2, totalPages: 3 })
    expect(paginate(values, 2).items).toHaveLength(45)
    expect(paginate(values, 99)).toMatchObject({ page: 3, items: [91, 92, 93, 94, 95] })
  })

  it('builds compact pagination items with ellipsis', () => {
    expect(buildPaginationItems(2, 10)).toEqual([1, 2, 3, 4, 'ellipsis', 10])
    expect(buildPaginationItems(9, 10)).toEqual([1, 'ellipsis', 7, 8, 9, 10])
    expect(buildPaginationItems(2, 4)).toEqual([1, 2, 3, 4])
  })
})
