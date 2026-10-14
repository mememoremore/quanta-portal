import { describe, expect, it } from 'vitest'
import {
  WORKSTATION_PERIODS,
  buildReservationRecord,
  isWorkstationDateAllowed,
  shiftWorkstationDate,
  toLocalDateKey,
} from './workstationRules'

describe('workstation periods and date boundaries', () => {
  const now = new Date(2026, 7, 29, 9, 20)

  it('uses the latest morning, afternoon and evening periods', () => {
    expect(WORKSTATION_PERIODS.map((period) => period.label)).toEqual([
      '上午 7:00~12:00',
      '下午 12:00~17:00',
      '晚上 17:00~22:00',
    ])
  })

  it('allows today through day 14 and clamps date navigation', () => {
    expect(isWorkstationDateAllowed('2026-08-29', now)).toBe(true)
    expect(isWorkstationDateAllowed('2026-09-12', now)).toBe(true)
    expect(isWorkstationDateAllowed('2026-09-13', now)).toBe(false)
    expect(shiftWorkstationDate('2026-08-29', -1, now)).toBe('2026-08-29')
    expect(shiftWorkstationDate('2026-09-12', 1, now)).toBe('2026-09-12')
  })

  it('builds a service record from selected periods', () => {
    const record = buildReservationRecord('工位5', '2026-08-30', ['afternoon', 'evening'], now)
    expect(record.workspace).toBe('工位5')
    expect(record.slots).toHaveLength(2)
    expect(new Date(record.slots[0].startAt).getHours()).toBe(12)
    expect(new Date(record.slots[0].endAt).getHours()).toBe(17)
    expect(new Date(record.slots[1].startAt).getHours()).toBe(17)
    expect(new Date(record.slots[1].endAt).getHours()).toBe(22)
    expect(toLocalDateKey(new Date(record.slots[0].startAt))).toBe('2026-08-30')
  })
})
