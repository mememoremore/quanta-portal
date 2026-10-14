import { describe, expect, it } from 'vitest'
import {
  buildReservationCards,
  canCancelReservation,
  formatServiceGroupLabel,
  type ReservationRecord,
  type ServiceTimeSlot,
} from './memberServiceRules'

const slot = (date: string, start: string, end: string): ServiceTimeSlot => ({
  startAt: `${date}T${start}:00+08:00`,
  endAt: `${date}T${end}:00+08:00`,
})

const record = (slots: ServiceTimeSlot[]): ReservationRecord => ({
  id: 'reservation-1',
  workspace: '工位5',
  submittedAt: '2026-03-28T09:20:00+08:00',
  slots,
})

const fullDay = (date: string) => [
  slot(date, '07:00', '12:00'),
  slot(date, '12:00', '17:00'),
  slot(date, '17:00', '22:00'),
]

describe('reservation cancellation rule', () => {
  it('uses the earliest slot and locks cancellation at the eight-hour boundary', () => {
    const value = record([
      slot('2026-03-29', '13:00', '18:00'),
      slot('2026-03-29', '07:00', '12:00'),
    ])

    expect(canCancelReservation(value, new Date('2026-03-28T22:59:59+08:00'))).toBe(true)
    expect(canCancelReservation(value, new Date('2026-03-28T23:00:00+08:00'))).toBe(false)
    expect(canCancelReservation(value, new Date('2026-03-29T08:00:00+08:00'))).toBe(false)
  })

  it('does not allow an empty reservation to be cancelled', () => {
    expect(canCancelReservation(record([]), new Date('2026-03-28T00:00:00+08:00'))).toBe(false)
  })
})

describe('reservation display cards', () => {
  it('labels a complete single day as all day', () => {
    expect(buildReservationCards(record(fullDay('2026-03-29')))).toMatchObject([
      { timeLabel: '2026年3月29日 全天' },
    ])
  })

  it('merges consecutive complete days into a date range', () => {
    expect(buildReservationCards(record([
      ...fullDay('2026-03-21'),
      ...fullDay('2026-03-22'),
      ...fullDay('2026-03-23'),
    ]))).toMatchObject([
      { timeLabel: '2026年3月21日~2026年3月23日' },
    ])
  })

  it('splits partial multi-day reservations into one card per day', () => {
    const cards = buildReservationCards(record([
      slot('2026-04-26', '07:00', '12:00'),
      slot('2026-04-27', '07:00', '12:00'),
      slot('2026-04-28', '07:00', '12:00'),
    ]))

    expect(cards).toHaveLength(3)
    expect(cards.map((item) => item.timeLabel)).toEqual([
      '2026年4月26日 上午7:00~12:00',
      '2026年4月27日 上午7:00~12:00',
      '2026年4月28日 上午7:00~12:00',
    ])
  })

  it('uses the latest afternoon and evening labels', () => {
    expect(buildReservationCards(record([
      slot('2026-04-26', '12:00', '17:00'),
      slot('2026-04-26', '17:00', '22:00'),
    ]))[0].timeLabel).toBe('2026年4月26日 下午12:00~17:00、晚上17:00~22:00')
  })
})

describe('service group label', () => {
  it('formats today, yesterday and older dates', () => {
    const now = new Date('2026-03-29T12:00:00+08:00')
    expect(formatServiceGroupLabel('2026-03-29T09:20:00+08:00', now)).toBe('上午 9:20')
    expect(formatServiceGroupLabel('2026-03-28T09:20:00+08:00', now)).toBe('昨天')
    expect(formatServiceGroupLabel('2026-03-20T09:20:00+08:00', now)).toBe('3月20日')
    expect(formatServiceGroupLabel('2025-12-01T09:20:00+08:00', now)).toBe('2025年12月1日')
  })
})
