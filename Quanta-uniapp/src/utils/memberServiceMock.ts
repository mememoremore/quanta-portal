import type { ReservationRecord, ServiceTimeSlot } from './memberServiceRules'

export type BorrowStatus = 'borrowed' | 'return-pending' | 'returned'
export type ShirtOrderStatus = 'pending-review' | 'ordered'

export interface BorrowRecord {
  id: string
  bookCode: string
  category: string
  borrowedOn: string
  dueOn: string
  submittedAt: string
  status: BorrowStatus
  returnedOn?: string
}

export interface ShirtOrderRecord {
  id: string
  color: string
  size: string
  paymentImage: string
  submittedAt: string
  status: ShirtOrderStatus
}

export interface MemberServicesSnapshot {
  reservations: ReservationRecord[]
  borrows: BorrowRecord[]
  orders: ShirtOrderRecord[]
}

export const ORDER_REVIEW_MESSAGE = '“待管理员确认”环节是管理员还在审核你提交的付款截图确认你已付款。一般在截止订购的时间前后会处理完毕。请耐心等候～'

const STORAGE_KEY = 'mockMemberServicesV1'
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))
const pad = (value: number) => String(value).padStart(2, '0')
const dateOnly = (value: Date) => `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
const at = (origin: Date, dayOffset: number, hour: number, minute = 0) => {
  const value = new Date(origin.getFullYear(), origin.getMonth(), origin.getDate() + dayOffset, hour, minute, 0, 0)
  return value.toISOString()
}
const day = (origin: Date, dayOffset: number) => dateOnly(new Date(origin.getFullYear(), origin.getMonth(), origin.getDate() + dayOffset))
const slotsFor = (origin: Date, dayOffset: number, periods: Array<[number, number]>) => periods.map(([start, end]): ServiceTimeSlot => ({
  startAt: at(origin, dayOffset, start),
  endAt: at(origin, dayOffset, end),
}))

export const createMemberServiceSeed = (now = new Date()): MemberServicesSnapshot => ({
  reservations: [
    {
      id: 'reservation-upcoming', workspace: '工位5', submittedAt: at(now, 0, 9, 20),
      slots: slotsFor(now, 3, [[7, 12]]),
    },
    {
      id: 'reservation-yesterday', workspace: '工位6', submittedAt: at(now, -1, 10),
      slots: slotsFor(now, -1, [[7, 12], [12, 17], [17, 22]]),
    },
    {
      id: 'reservation-range', workspace: '工位6', submittedAt: at(now, -8, 14),
      slots: [
        ...slotsFor(now, -7, [[7, 12], [12, 17], [17, 22]]),
        ...slotsFor(now, -6, [[7, 12], [12, 17], [17, 22]]),
        ...slotsFor(now, -5, [[7, 12], [12, 17], [17, 22]]),
      ],
    },
  ],
  borrows: [
    { id: 'book-current', bookCode: '编号012', category: '教材类', borrowedOn: day(now, 0), dueOn: day(now, 120), submittedAt: at(now, 0, 9, 20), status: 'borrowed' },
    { id: 'book-returned', bookCode: '编号132', category: '非教材类', borrowedOn: day(now, -1), dueOn: day(now, 90), submittedAt: at(now, -1, 11), status: 'returned', returnedOn: day(now, -1) },
    { id: 'book-old', bookCode: '编号262', category: '非教材类', borrowedOn: day(now, -160), dueOn: day(now, -70), submittedAt: at(now, -160, 15), status: 'borrowed' },
  ],
  orders: [
    { id: 'shirt-pending', color: '电光白', size: 'XL', paymentImage: '/static/picture/payment-receipt.svg', submittedAt: at(now, 0, 9, 20), status: 'pending-review' },
    { id: 'shirt-ordered', color: '电光白', size: 'XL', paymentImage: '/static/picture/payment-receipt.svg', submittedAt: at(now, -120, 10), status: 'ordered' },
  ],
})

export const createMemberServiceStore = (
  initial: MemberServicesSnapshot,
  persist?: (snapshot: MemberServicesSnapshot) => void,
) => {
  let state = clone(initial)
  const commit = () => persist?.(clone(state))
  return {
    getMemberServices: async () => clone(state),
    addReservation: async (record: ReservationRecord) => {
      if (state.reservations.some((reservation) => reservation.id === record.id)) throw new Error('预约记录已存在')
      state.reservations.unshift(clone(record))
      commit()
    },
    cancelReservation: async (id: string) => {
      if (!state.reservations.some((record) => record.id === id)) throw new Error('预约记录不存在')
      state.reservations = state.reservations.filter((record) => record.id !== id)
      commit()
    },
    confirmBookReturn: async (id: string, returnedOn: string) => {
      const record = state.borrows.find((book) => book.id === id)
      if (!record) throw new Error('借阅记录不存在')
      record.status = 'return-pending'
      record.returnedOn = returnedOn
      commit()
    },
    addBorrowRecord: async (record: BorrowRecord) => {
      if (state.borrows.some((book) => book.id === record.id)) throw new Error('借阅记录已存在')
      state.borrows.unshift(clone(record))
      commit()
    },
  }
}

let productionStore: ReturnType<typeof createMemberServiceStore> | null = null
const getProductionStore = () => {
  if (productionStore) return productionStore
  const stored = uni.getStorageSync(STORAGE_KEY)
  let initial = createMemberServiceSeed()
  if (stored) {
    try { initial = typeof stored === 'string' ? JSON.parse(stored) : stored } catch { /* keep seed */ }
  }
  productionStore = createMemberServiceStore(initial, (snapshot) => uni.setStorageSync(STORAGE_KEY, JSON.stringify(snapshot)))
  return productionStore
}

export const getMemberServices = () => getProductionStore().getMemberServices()
export const addReservation = (record: ReservationRecord) => getProductionStore().addReservation(record)
export const cancelReservation = (id: string) => getProductionStore().cancelReservation(id)
export const confirmBookReturn = (id: string, returnedOn: string) => getProductionStore().confirmBookReturn(id, returnedOn)
export const addBorrowRecord = (record: BorrowRecord) => getProductionStore().addBorrowRecord(record)
