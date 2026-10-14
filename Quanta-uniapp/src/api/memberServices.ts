import request from '../utils/request'
import type { ReservationRecord } from '../utils/memberServiceRules'
import type { BorrowRecord, MemberServicesSnapshot, ShirtOrderRecord } from '../utils/memberServiceMock'
import type { TableResponse } from './contracts'
import { resolveApiAssetUrl } from './mappers'

interface ReservationDto {
  reservationId: number
  workstationCode?: string
  reserveStart: string
  reserveEnd: string
  status?: string
  createTime?: string
}

interface BorrowDto {
  borrowId: number
  bookId?: number
  bookName?: string
  bookType?: string
  borrowTime?: string
  dueTime?: string
  returnTime?: string
  status?: string
  createTime?: string
}

interface OrderDto {
  orderId: number
  selectedColor?: string
  selectedSize?: string
  paymentProofUrl?: string
  paymentProofPath?: string
  status?: string
  orderTime?: string
  createTime?: string
}

const dateOnly = (value?: string) => value ? value.slice(0, 10) : ''
const safeDateTime = (value?: string) => value || new Date(0).toISOString()

export const mapReservationRecord = (row: ReservationDto): ReservationRecord => {
  const date = dateOnly(row.reserveStart)
  const slot = (startHour: number, endHour: number) => ({ startAt: `${date}T${String(startHour).padStart(2, '0')}:00:00`, endAt: `${date}T${String(endHour).padStart(2, '0')}:00:00` })
  return {
    id: String(row.reservationId),
    workspace: row.workstationCode || '工位',
    submittedAt: safeDateTime(row.createTime),
    slots: date ? [slot(7, 12), slot(12, 17), slot(17, 22)] : [],
  }
}

export const mapBorrowRecord = (row: BorrowDto): BorrowRecord => ({
  id: String(row.borrowId),
  bookCode: row.bookName || (row.bookId ? `编号${String(row.bookId).padStart(3, '0')}` : '图书'),
  category: row.bookType?.toUpperCase() === 'TEXTBOOK' ? '教材类' : '非教材类',
  borrowedOn: dateOnly(row.borrowTime || row.createTime),
  dueOn: dateOnly(row.dueTime),
  returnedOn: dateOnly(row.returnTime) || undefined,
  submittedAt: safeDateTime(row.createTime || row.borrowTime),
  status: row.status === 'RETURNED' ? 'returned' : row.status === 'BORROWED' || row.status === 'OVERDUE' ? 'borrowed' : 'return-pending',
})

export const mapOrderRecord = (row: OrderDto): ShirtOrderRecord => ({
  id: String(row.orderId),
  color: row.selectedColor || '',
  size: row.selectedSize || '',
  paymentImage: resolveApiAssetUrl(row.paymentProofUrl || row.paymentProofPath || ''),
  submittedAt: safeDateTime(row.orderTime || row.createTime),
  status: row.status === 'APPROVED' ? 'ordered' : 'pending-review',
})

export const getMemberServices = async (): Promise<MemberServicesSnapshot> => {
  const query = { pageNum: 1, pageSize: 500 }
  const [reservations, borrows, orders] = await Promise.all([
    request<TableResponse<ReservationDto>>({ url: '/qt/reservation/myDetailList', data: query }),
    request<TableResponse<BorrowDto>>({ url: '/qt/borrow/myDetailList', data: query }),
    request<TableResponse<OrderDto>>({ url: '/qt/order/myDetailList', data: query }),
  ])
  return {
    reservations: (reservations.rows || []).map(mapReservationRecord),
    borrows: (borrows.rows || []).map(mapBorrowRecord),
    orders: (orders.rows || []).map(mapOrderRecord),
  }
}
