export interface ServiceTimeSlot {
  startAt: string
  endAt: string
}

export interface ReservationRecord {
  id: string
  workspace: string
  submittedAt: string
  slots: ServiceTimeSlot[]
}

export interface ReservationDisplayCard {
  sourceId: string
  workspace: string
  submittedAt: string
  timeLabel: string
}

const CANCEL_WINDOW_MS = 8 * 60 * 60 * 1000
const FULL_DAY_PERIODS = ['07:00-12:00', '12:00-17:00', '17:00-22:00']

const pad = (value: number) => String(value).padStart(2, '0')
const localDateKey = (value: Date) => `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
const localTimeKey = (value: Date) => `${pad(value.getHours())}:${pad(value.getMinutes())}`
const displayDate = (key: string) => {
  const [year, month, day] = key.split('-').map(Number)
  return `${year}年${month}月${day}日`
}

const localDayNumber = (key: string) => {
  const [year, month, day] = key.split('-').map(Number)
  return Date.UTC(year, month - 1, day) / 86400000
}

const periodLabel = (start: Date, end: Date) => {
  const hour = start.getHours()
  const prefix = hour < 12 ? '上午' : hour < 17 ? '下午' : '晚上'
  return `${prefix}${hour}:${pad(start.getMinutes())}~${end.getHours()}:${pad(end.getMinutes())}`
}

export const canCancelReservation = (record: ReservationRecord, now = new Date()) => {
  if (!record.slots.length) return false
  const earliest = Math.min(...record.slots.map((slot) => new Date(slot.startAt).getTime()))
  return Number.isFinite(earliest) && earliest - now.getTime() > CANCEL_WINDOW_MS
}

export const buildReservationCards = (record: ReservationRecord): ReservationDisplayCard[] => {
  const slotsByDate = new Map<string, ServiceTimeSlot[]>()
  record.slots.forEach((slot) => {
    const date = localDateKey(new Date(slot.startAt))
    slotsByDate.set(date, [...(slotsByDate.get(date) || []), slot])
  })

  const dates = [...slotsByDate.keys()].sort()
  const isFullDay = (date: string) => {
    const periods = (slotsByDate.get(date) || []).map((slot) => {
      const start = new Date(slot.startAt)
      const end = new Date(slot.endAt)
      return `${localTimeKey(start)}-${localTimeKey(end)}`
    })
    return FULL_DAY_PERIODS.every((period) => periods.includes(period))
  }

  const cards: ReservationDisplayCard[] = []
  for (let index = 0; index < dates.length;) {
    const date = dates[index]
    if (isFullDay(date)) {
      let endIndex = index
      while (
        endIndex + 1 < dates.length &&
        isFullDay(dates[endIndex + 1]) &&
        localDayNumber(dates[endIndex + 1]) - localDayNumber(dates[endIndex]) === 1
      ) endIndex += 1

      const endDate = dates[endIndex]
      cards.push({
        sourceId: record.id,
        workspace: record.workspace,
        submittedAt: record.submittedAt,
        timeLabel: index === endIndex ? `${displayDate(date)} 全天` : `${displayDate(date)}~${displayDate(endDate)}`,
      })
      index = endIndex + 1
      continue
    }

    const labels = (slotsByDate.get(date) || [])
      .slice()
      .sort((left, right) => new Date(left.startAt).getTime() - new Date(right.startAt).getTime())
      .map((slot) => periodLabel(new Date(slot.startAt), new Date(slot.endAt)))
    cards.push({
      sourceId: record.id,
      workspace: record.workspace,
      submittedAt: record.submittedAt,
      timeLabel: `${displayDate(date)} ${labels.join('、')}`,
    })
    index += 1
  }
  return cards
}

export const formatServiceGroupLabel = (submittedAt: string, now = new Date()) => {
  const submitted = new Date(submittedAt)
  const today = localDateKey(now)
  const submittedDay = localDateKey(submitted)
  const dayDifference = localDayNumber(today) - localDayNumber(submittedDay)
  if (dayDifference === 0) {
    const prefix = submitted.getHours() < 12 ? '上午' : '下午'
    return `${prefix} ${submitted.getHours()}:${pad(submitted.getMinutes())}`
  }
  if (dayDifference === 1) return '昨天'
  if (submitted.getFullYear() === now.getFullYear()) return `${submitted.getMonth() + 1}月${submitted.getDate()}日`
  return `${submitted.getFullYear()}年${submitted.getMonth() + 1}月${submitted.getDate()}日`
}
