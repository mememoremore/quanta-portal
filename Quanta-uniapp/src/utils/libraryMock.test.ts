import { describe, expect, it } from 'vitest'
import { createLibraryStore, type LibraryBook } from './libraryMock'

const books: LibraryBook[] = [
  { id: 'required-book', code: '065', cabinet: 'A-02', category: 'textbook', required: true, status: 'available' },
  { id: 'general-book', code: '073', cabinet: 'B-01', category: 'general', required: false, status: 'available' },
  { id: 'already-borrowed', code: '051', cabinet: 'A-01', category: 'textbook', required: false, status: 'borrowed' },
]

describe('library store', () => {
  it('borrows an available required book and creates a five-month service record', async () => {
    const records: any[] = []
    const store = createLibraryStore(books, async (record) => { records.push(record) })
    const result = await store.borrowBook('required-book', new Date(2026, 7, 29, 10, 0))

    expect(result.book.status).toBe('borrowed')
    expect(result.dueOn).toBe('2027-01-29')
    expect(records[0]).toMatchObject({ bookCode: '编号065', category: '教材类', dueOn: '2027-01-29' })
  })

  it('uses a three-month loan period for general books', async () => {
    const store = createLibraryStore(books, async () => undefined)
    expect((await store.borrowBook('general-book', new Date(2026, 7, 29))).dueOn).toBe('2026-11-29')
  })

  it('rejects books that are already borrowed without creating a service record', async () => {
    let calls = 0
    const store = createLibraryStore(books, async () => { calls += 1 })
    await expect(store.borrowBook('already-borrowed', new Date(2026, 7, 29))).rejects.toThrow('该书已借出')
    expect(calls).toBe(0)
  })
})
