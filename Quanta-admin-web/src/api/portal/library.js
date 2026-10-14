import { request } from '@/utils/request'

function formatDateOnly(value) {
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  const match = String(value || '').match(/^(\d{4}-\d{2}-\d{2})/)
  if (!match) throw new Error('应还日期格式无效')
  return match[1]
}

function mapBook(row = {}) {
  const availableCount = Number(row.availableCount || 0)
  return {
    bookId: Number(row.bookId),
    isbn: row.isbn || '',
    bookName: row.bookName || '',
    author: row.author || '',
    publisher: row.publisher || '',
    publishDate: row.publishDate || '',
    totalCount: Number(row.totalCount || 0),
    availableCount,
    locationDesc: row.locationDesc || '',
    status: row.status || '',
    bookType: row.bookType || '',
    available: row.status === '0' && availableCount > 0,
  }
}

export async function getBooks(params = {}) {
  const response = await request({
    url: '/qt/book/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 20, ...params },
  })
  return {
    rows: (response.rows || []).map(mapBook),
    total: Number(response.total || 0),
  }
}

export function borrowBook(bookId, dueTime) {
  return request({
    url: '/qt/borrow',
    method: 'post',
    data: { bookId: Number(bookId), dueTime: formatDateOnly(dueTime) },
  })
}
