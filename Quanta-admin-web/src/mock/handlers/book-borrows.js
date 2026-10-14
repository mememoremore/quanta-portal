import { findAccountByToken } from '../data/accounts'
import { mockBookBorrowRecords } from '../data/book-borrows'

export const bookBorrowHandlers = [{
  method: 'get',
  path: '/qt/borrow/detailList',
  handle(config) {
    const account = findAccountByToken(config.headers?.Authorization || '')
    if (!account) return { code: 401, msg: '登录状态已失效' }
    if (!account.permissions.includes('*:*:*') && !account.permissions.includes('qt:borrow:list')) {
      return { code: 403, msg: '没有查看图书借阅记录的权限' }
    }
    return { code: 200, rows: mockBookBorrowRecords, total: mockBookBorrowRecords.length }
  },
}]
