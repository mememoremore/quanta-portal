import type { ReservationRecord, ServiceTimeSlot } from './memberServiceRules'

export type WorkstationPeriodKey = 'morning' | 'afternoon' | 'evening'
export type WorkstationSlotStatus = 'available' | 'booked' | 'mine' | 'unavailable'

export interface WorkstationPeriod {
  key: WorkstationPeriodKey
  label: string
  startHour: number
  endHour: number
}

export const WORKSTATION_PERIODS: WorkstationPeriod[] = [
  { key: 'morning', label: '上午 7:00~12:00', startHour: 7, endHour: 12 },
  { key: 'afternoon', label: '下午 12:00~17:00', startHour: 12, endHour: 17 },
  { key: 'evening', label: '晚上 17:00~22:00', startHour: 17, endHour: 22 },
]

const pad = (value: number) => String(value).padStart(2, '0')

export const toLocalDateKey = (date: Date) => (
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
)

const parseLocalDate = (dateKey: string) => {
  const [year, month, day] = dateKey.split('-').map(Number)
  return new Date(year, month - 1, day)
}

const localDayNumber = (date: Date) => Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86400000

export const isWorkstationDateAllowed = (dateKey: string, today = new Date()) => {
  const target = parseLocalDate(dateKey)
  if (Number.isNaN(target.getTime()) || toLocalDateKey(target) !== dateKey) return false
  const difference = localDayNumber(target) - localDayNumber(today)
  return difference >= 0 && difference <= 14
}

export const shiftWorkstationDate = (dateKey: string, delta: number, today = new Date()) => {
  const current = parseLocalDate(dateKey)
  const min = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const max = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 14)
  const shifted = new Date(current.getFullYear(), current.getMonth(), current.getDate() + delta)
  if (shifted < min) return toLocalDateKey(min)
  if (shifted > max) return toLocalDateKey(max)
  return toLocalDateKey(shifted)
}

const slotFor = (dateKey: string, period: WorkstationPeriod): ServiceTimeSlot => {
  const date = parseLocalDate(dateKey)
  const start = new Date(date.getFullYear(), date.getMonth(), date.getDate(), period.startHour)
  const end = new Date(date.getFullYear(), date.getMonth(), date.getDate(), period.endHour)
  return { startAt: start.toISOString(), endAt: end.toISOString() }
}

export const buildReservationRecord = (
  workstationName: string,
  dateKey: string,
  periods: WorkstationPeriodKey[],
  now = new Date(),
): ReservationRecord => {
  const uniquePeriods = [...new Set(periods)]
  const slots = uniquePeriods.map((key) => {
    const period = WORKSTATION_PERIODS.find((item) => item.key === key)
    if (!period) throw new Error('预约时段无效')
    return slotFor(dateKey, period)
  })
  return {
    id: `reservation-${workstationName}-${dateKey}-${uniquePeriods.join('-')}-${now.getTime()}`,
    workspace: workstationName,
    submittedAt: now.toISOString(),
    slots,
  }
}
