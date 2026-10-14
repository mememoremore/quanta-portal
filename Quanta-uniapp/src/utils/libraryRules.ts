export type BookCategory = 'textbook' | 'general'
export type PaginationItem = number | 'ellipsis'

export const calculateBookDueDate = (borrowedOn: Date, category: BookCategory) => {
  const months = category === 'textbook' ? 5 : 3
  const targetMonth = borrowedOn.getMonth() + months
  const lastDay = new Date(borrowedOn.getFullYear(), targetMonth + 1, 0).getDate()
  return new Date(
    borrowedOn.getFullYear(),
    targetMonth,
    Math.min(borrowedOn.getDate(), lastDay),
  )
}

export const buildBorrowConfirmationCopy = (code: string, category: BookCategory, borrowedOn: Date) => {
  const dueOn = calculateBookDueDate(borrowedOn, category)
  const categoryLabel = category === 'textbook' ? '教材类' : '非教材类'
  const dueLabel = `${dueOn.getFullYear()}年${dueOn.getMonth() + 1}月${dueOn.getDate()}日`
  return `确认借阅「编号${code}」${categoryLabel}书籍吗？借阅后请在${dueLabel}前归还。`
}

export const filterLibraryBooks = <T extends { code: string; cabinet: string }>(books: T[], keyword: string) => {
  const query = keyword.trim().toLocaleLowerCase()
  if (!query) return books.slice()
  return books.filter((book) => `${book.code} ${book.cabinet}`.toLocaleLowerCase().includes(query))
}

export const paginate = <T>(items: T[], requestedPage: number, pageSize = 45) => {
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize))
  const page = Math.min(totalPages, Math.max(1, Math.trunc(requestedPage) || 1))
  const start = (page - 1) * pageSize
  return { items: items.slice(start, start + pageSize), page, totalPages }
}

export const buildPaginationItems = (requestedPage: number, totalPages: number): PaginationItem[] => {
  if (totalPages <= 7) return Array.from({ length: Math.max(0, totalPages) }, (_, index) => index + 1)
  const page = Math.min(totalPages, Math.max(1, requestedPage))
  if (page <= 3) return [1, 2, 3, 4, 'ellipsis', totalPages]
  if (page >= totalPages - 2) return [1, 'ellipsis', totalPages - 3, totalPages - 2, totalPages - 1, totalPages]
  return [1, 'ellipsis', page - 1, page, page + 1, 'ellipsis', totalPages]
}
