import { addBorrowRecord, type BorrowRecord } from './memberServiceMock'
import { calculateBookDueDate, type BookCategory } from './libraryRules'

export type LibraryBookStatus = 'available' | 'borrowed'

export interface LibraryBook {
  id: string
  code: string
  cabinet: string
  category: BookCategory
  required: boolean
  status: LibraryBookStatus
}

export interface BorrowBookResult {
  book: LibraryBook
  borrowedOn: string
  dueOn: string
  categoryLabel: '教材类' | '非教材类'
}

const STORAGE_KEY = 'mockLibraryBooksV1'
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))
const pad = (value: number) => String(value).padStart(2, '0')
const dateOnly = (value: Date) => `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`

const borrowedCodes = new Set([12, 51, 55, 66, 68, 77, 85, 88, 132, 262])
const requiredCodes = new Set([57, 65, 76, 95, 108, 205])

export const createLibrarySeed = (): LibraryBook[] => Array.from({ length: 450 }, (_, index) => {
  const number = index + 1
  const code = String(number).padStart(3, '0')
  const category: BookCategory = number === 65 || number % 2 === 0 ? 'textbook' : 'general'
  return {
    id: `book-${code}`,
    code,
    cabinet: `${String.fromCharCode(65 + Math.floor(index / 90))}-${String(Math.floor((index % 90) / 15) + 1).padStart(2, '0')}`,
    category,
    required: requiredCodes.has(number),
    status: borrowedCodes.has(number) ? 'borrowed' : 'available',
  }
})

export const createLibraryStore = (
  initial: LibraryBook[],
  createBorrowRecord: (record: BorrowRecord) => Promise<void>,
  persist?: (books: LibraryBook[]) => void,
) => {
  let state = clone(initial)
  return {
    getLibraryBooks: async () => clone(state),
    borrowBook: async (bookId: string, now = new Date()): Promise<BorrowBookResult> => {
      const book = state.find((item) => item.id === bookId)
      if (!book) throw new Error('书籍不存在')
      if (book.status === 'borrowed') throw new Error('该书已借出')

      const categoryLabel = book.category === 'textbook' ? '教材类' : '非教材类'
      const borrowedOn = dateOnly(now)
      const dueOn = dateOnly(calculateBookDueDate(now, book.category))
      const record: BorrowRecord = {
        id: `library-${book.id}-${now.getTime()}`,
        bookCode: `编号${book.code}`,
        category: categoryLabel,
        borrowedOn,
        dueOn,
        submittedAt: now.toISOString(),
        status: 'borrowed',
      }
      await createBorrowRecord(record)
      book.status = 'borrowed'
      persist?.(clone(state))
      return { book: clone(book), borrowedOn, dueOn, categoryLabel }
    },
  }
}

let productionStore: ReturnType<typeof createLibraryStore> | null = null
const getProductionStore = () => {
  if (productionStore) return productionStore
  const stored = uni.getStorageSync(STORAGE_KEY)
  let initial = createLibrarySeed()
  if (stored) {
    try { initial = typeof stored === 'string' ? JSON.parse(stored) : stored } catch { /* keep seed */ }
  }
  productionStore = createLibraryStore(initial, addBorrowRecord, (books) => uni.setStorageSync(STORAGE_KEY, JSON.stringify(books)))
  return productionStore
}

export const getLibraryBooks = () => getProductionStore().getLibraryBooks()
export const borrowBook = (bookId: string, now?: Date) => getProductionStore().borrowBook(bookId, now)
