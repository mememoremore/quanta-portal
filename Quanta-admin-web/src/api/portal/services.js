import { request } from '@/utils/request'

function mapBorrow(row = {}) {
  return {
    borrowId: Number(row.borrowId),
    bookId: Number(row.bookId),
    bookName: row.bookName || '',
    isbn: row.isbn || '',
    bookType: row.bookType || '',
    borrowTime: row.borrowTime || '',
    dueTime: row.dueTime || '',
    returnTime: row.returnTime || '',
    status: row.status || '',
  }
}

function mapOrder(row = {}) {
  return {
    orderId: Number(row.orderId),
    orderNo: row.orderNo || '',
    itemId: Number(row.itemId),
    itemName: row.itemName || '',
    selectedColor: row.selectedColor || '',
    selectedSize: row.selectedSize || '',
    quantity: Number(row.quantity || 0),
    unitPrice: row.unitPrice == null ? null : Number(row.unitPrice),
    totalAmount: row.totalAmount == null ? null : Number(row.totalAmount),
    status: row.status || '',
    orderTime: row.orderTime || row.createTime || '',
    paymentProofUrl: row.paymentProofUrl || row.paymentProofPath || '',
  }
}

export async function getMyServices() {
  const params = { pageNum: 1, pageSize: 500 }
  const [borrowResponse, orderResponse] = await Promise.all([
    request({ url: '/qt/borrow/detailList', method: 'get', params }),
    request({ url: '/qt/order/detailList', method: 'get', params }),
  ])
  return {
    borrows: (borrowResponse.rows || []).map(mapBorrow),
    orders: (orderResponse.rows || []).map(mapOrder),
  }
}
