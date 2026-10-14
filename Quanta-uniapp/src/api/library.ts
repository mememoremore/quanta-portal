import request from '../utils/request'
import { calculateBookDueDate, type BookCategory } from '../utils/libraryRules'
import type { TableResponse } from './contracts'

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

export interface BookDto {
  bookId: number
  isbn?: string
  bookName?: string
  locationDesc?: string
  availableCount?: number
  status?: string
  bookType?: string
}

const pad = (value: number) => String(value).padStart(2, '0')
const dateOnly = (value: Date) => `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`

export const mapBook = (row: BookDto): LibraryBook => {
  const category: BookCategory = row.bookType?.toUpperCase() === 'TEXTBOOK' ? 'textbook' : 'general'
  return {
    id: String(row.bookId),
    code: String(row.bookId).padStart(3, '0'),
    cabinet: row.locationDesc || row.isbn || row.bookName || '位置待确认',
    category,
    required: category === 'textbook',
    status: row.status === '0' && Number(row.availableCount || 0) > 0 ? 'available' : 'borrowed',
  }
}

export const getLibraryBooks = async () => {
  const response = await request<TableResponse<BookDto>>({ url: '/qt/book/list', data: { pageNum: 1, pageSize: 1000 } })
  return (response.rows || []).map(mapBook)
}

export const borrowBook = async (bookId: string, now = new Date()): Promise<BorrowBookResult> => {
  const books = await getLibraryBooks()
  const book = books.find((item) => item.id === bookId)
  if (!book) throw new Error('书籍不存在')
  if (book.status !== 'available') throw new Error('该书当前不可借阅')
  const dueDate = calculateBookDueDate(now, book.category)
  await request({ url: '/qt/borrow', method: 'POST', data: { bookId: Number(bookId), dueTime: `${dateOnly(dueDate)} 23:59:59` } })
  return {
    book: { ...book, status: 'borrowed' },
    borrowedOn: dateOnly(now),
    dueOn: dateOnly(dueDate),
    categoryLabel: book.category === 'textbook' ? '教材类' : '非教材类',
  }
}
