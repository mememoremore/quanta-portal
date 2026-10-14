import { describe, expect, it } from 'vitest'
import { createMemberServiceStore, type MemberServicesSnapshot } from './memberServiceMock'

const seed: MemberServicesSnapshot = {
  reservations: [{
    id: 'reservation-1',
    workspace: '工位5',
    submittedAt: '2026-03-28T09:20:00+08:00',
    slots: [{ startAt: '2026-03-29T07:00:00+08:00', endAt: '2026-03-29T12:00:00+08:00' }],
  }],
  borrows: [{
    id: 'book-1',
    bookCode: '编号012',
    category: '教材类',
    borrowedOn: '2026-03-29',
    dueOn: '2026-07-31',
    submittedAt: '2026-03-29T09:20:00+08:00',
    status: 'borrowed',
  }],
  orders: [],
}

describe('member service store', () => {
  it('removes a cancelled reservation', async () => {
    const store = createMemberServiceStore(seed)
    await store.cancelReservation('reservation-1')
    expect((await store.getMemberServices()).reservations).toHaveLength(0)
  })

  it('marks a book return pending review with the selected date', async () => {
    const store = createMemberServiceStore(seed)
    await store.confirmBookReturn('book-1', '2026-07-30')
    const book = (await store.getMemberServices()).borrows[0]
    expect(book.status).toBe('return-pending')
    expect(book.returnedOn).toBe('2026-07-30')
  })

  it('returns cloned snapshots that cannot mutate repository state', async () => {
    const store = createMemberServiceStore(seed)
    const first = await store.getMemberServices()
    first.reservations.length = 0
    expect((await store.getMemberServices()).reservations).toHaveLength(1)
  })

  it('adds a borrow record exactly once', async () => {
    const store = createMemberServiceStore(seed)
    const record = {
      id: 'library-065', bookCode: '编号065', category: '教材类', borrowedOn: '2026-08-29',
      dueOn: '2027-01-29', submittedAt: '2026-08-29T09:20:00+08:00', status: 'borrowed' as const,
    }
    await store.addBorrowRecord(record)
    expect((await store.getMemberServices()).borrows[0]).toMatchObject({ id: 'library-065' })
    await expect(store.addBorrowRecord(record)).rejects.toThrow('借阅记录已存在')
  })

  it('adds a reservation record exactly once', async () => {
    const store = createMemberServiceStore(seed)
    const reservation = {
      id: 'reservation-new', workspace: '工位8', submittedAt: '2026-08-29T09:20:00+08:00',
      slots: [{ startAt: '2026-08-30T12:00:00+08:00', endAt: '2026-08-30T17:00:00+08:00' }],
    }
    await store.addReservation(reservation)
    expect((await store.getMemberServices()).reservations[0]).toMatchObject({ id: 'reservation-new' })
    await expect(store.addReservation(reservation)).rejects.toThrow('预约记录已存在')
  })
})
