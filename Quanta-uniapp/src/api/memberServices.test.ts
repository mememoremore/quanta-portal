import { describe, expect, it } from 'vitest'
import { mapBorrowRecord, mapOrderRecord, mapReservationRecord } from './memberServices'

describe('member service backend mapping', () => {
  it('maps date-only reservations conservatively as a full day', () => {
    expect(mapReservationRecord({ reservationId: 2, workstationCode: 'WS-A01', reserveStart: '2026-09-14', reserveEnd: '2026-09-14' }).slots).toHaveLength(3)
  })

  it('maps borrow status', () => {
    expect(mapBorrowRecord({ borrowId: 3, bookName: '设计模式', status: 'BORROWED' }).status).toBe('borrowed')
  })

  it('maps approved clothing orders', () => {
    expect(mapOrderRecord({ orderId: 4, status: 'APPROVED' }).status).toBe('ordered')
  })
})
