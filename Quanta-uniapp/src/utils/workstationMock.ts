import type { ReservationRecord } from './memberServiceRules'
import { addReservation } from './memberServiceMock'
import {
  WORKSTATION_PERIODS,
  buildReservationRecord,
  isWorkstationDateAllowed,
  toLocalDateKey,
  type WorkstationPeriodKey,
  type WorkstationSlotStatus,
} from './workstationRules'

export interface WorkstationSlot {
  period: WorkstationPeriodKey
  status: WorkstationSlotStatus
}

export interface Workstation {
  id: string
  name: string
  slots: WorkstationSlot[]
}

export interface WorkstationDay {
  dateKey: string
  workstations: Workstation[]
}

export type WorkstationSeed = Record<string, WorkstationDay>

const STORAGE_KEY = 'mockWorkstationReservationsV1'
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))

const statusesFor = (seatNumber: number): WorkstationSlotStatus[] => {
  if (seatNumber === 2) return ['booked', 'booked', 'booked']
  if (seatNumber === 5) return ['available', 'booked', 'mine']
  if (seatNumber === 6) return ['available', 'available', 'booked']
  if (seatNumber === 9) return ['available', 'available', 'unavailable']
  return ['available', 'available', 'available']
}

const buildDay = (dateKey: string): WorkstationDay => ({
  dateKey,
  workstations: Array.from({ length: 11 }, (_, index) => {
    const seatNumber = index + 1
    const statuses = statusesFor(seatNumber)
    return {
      id: `seat-${seatNumber}`,
      name: `工位${seatNumber}`,
      slots: WORKSTATION_PERIODS.map((period, slotIndex) => ({ period: period.key, status: statuses[slotIndex] })),
    }
  }),
})

export const createWorkstationSeed = (today = new Date()): WorkstationSeed => {
  const seed: WorkstationSeed = {}
  for (let offset = 0; offset <= 14; offset += 1) {
    const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() + offset)
    const dateKey = toLocalDateKey(date)
    seed[dateKey] = buildDay(dateKey)
  }
  return seed
}

export const createWorkstationStore = (
  initial: WorkstationSeed,
  writeReservation: (record: ReservationRecord) => Promise<void>,
  persist?: (seed: WorkstationSeed) => void,
  today = new Date(),
) => {
  let state = clone(initial)
  const getDay = (dateKey: string) => {
    if (!isWorkstationDateAllowed(dateKey, today)) throw new Error('该日期暂不可预约')
    if (!state[dateKey]) state[dateKey] = buildDay(dateKey)
    return state[dateKey]
  }
  return {
    getWorkstationDay: async (dateKey: string) => clone(getDay(dateKey)),
    releaseReservation: async (record: ReservationRecord) => {
      let changed = false
      record.slots.forEach((serviceSlot) => {
        const start = new Date(serviceSlot.startAt)
        const dateKey = toLocalDateKey(start)
        const day = state[dateKey]
        if (!day) return
        const workstation = day.workstations.find((item) => item.name === record.workspace)
        const period = WORKSTATION_PERIODS.find((item) => item.startHour === start.getHours())
        const slot = workstation?.slots.find((item) => item.period === period?.key)
        if (slot?.status === 'mine') { slot.status = 'available'; changed = true }
      })
      if (changed) persist?.(clone(state))
    },
    reserveWorkstation: async (
      workstationId: string,
      dateKey: string,
      requestedPeriods: WorkstationPeriodKey[],
      now = new Date(),
    ) => {
      if (!isWorkstationDateAllowed(dateKey, now)) throw new Error('该日期暂不可预约')
      const periods = [...new Set(requestedPeriods)]
      if (!periods.length) throw new Error('请选择预约时段')
      const day = getDay(dateKey)
      const workstation = day.workstations.find((item) => item.id === workstationId)
      if (!workstation) throw new Error('工位不存在')
      const slots = periods.map((period) => workstation.slots.find((slot) => slot.period === period))
      if (slots.some((slot) => !slot)) throw new Error('预约时段无效')
      if (slots.some((slot) => slot?.status === 'booked')) throw new Error('该时段已被预约')
      if (slots.some((slot) => slot?.status === 'mine')) throw new Error('该时段已经是你的预约')
      if (slots.some((slot) => slot?.status === 'unavailable')) throw new Error('该时段暂不可预约')

      const record = buildReservationRecord(workstation.name, dateKey, periods, now)
      await writeReservation(record)
      slots.forEach((slot) => { if (slot) slot.status = 'mine' })
      persist?.(clone(state))
      return clone(record)
    },
  }
}

let productionStore: ReturnType<typeof createWorkstationStore> | null = null
const getProductionStore = () => {
  if (productionStore) return productionStore
  const stored = uni.getStorageSync(STORAGE_KEY)
  let seed = createWorkstationSeed()
  if (stored) {
    try { seed = typeof stored === 'string' ? JSON.parse(stored) : stored } catch { /* keep seed */ }
  }
  productionStore = createWorkstationStore(seed, addReservation, (next) => uni.setStorageSync(STORAGE_KEY, JSON.stringify(next)))
  return productionStore
}

export const getWorkstationDay = (dateKey: string) => getProductionStore().getWorkstationDay(dateKey)
export const releaseWorkstationReservation = (record: ReservationRecord) => getProductionStore().releaseReservation(record)
export const reserveWorkstation = (
  workstationId: string,
  dateKey: string,
  periods: WorkstationPeriodKey[],
) => getProductionStore().reserveWorkstation(workstationId, dateKey, periods)
