import { describe, expect, it } from 'vitest'
import { createWorkstationSeed, createWorkstationStore } from './workstationMock'

describe('workstation reservation store', () => {
  const now = new Date(2026, 7, 29, 9, 20)
  const dateKey = '2026-08-30'

  it('books multiple available periods and marks them as mine', async () => {
    const added: any[] = []
    const store = createWorkstationStore(createWorkstationSeed(now), async (record) => { added.push(record) }, undefined, now)
    const record = await store.reserveWorkstation('seat-8', dateKey, ['morning', 'evening'], now)
    expect(record.slots).toHaveLength(2)
    expect(added).toHaveLength(1)
    const seat = (await store.getWorkstationDay(dateKey)).workstations.find((item) => item.id === 'seat-8')!
    expect(seat.slots.map((slot) => slot.status)).toEqual(['mine', 'available', 'mine'])
  })

  it('rejects booked, mine and unavailable periods without writing service data', async () => {
    const added: any[] = []
    const store = createWorkstationStore(createWorkstationSeed(now), async (record) => { added.push(record) }, undefined, now)
    await expect(store.reserveWorkstation('seat-2', dateKey, ['morning'], now)).rejects.toThrow('该时段已被预约')
    await expect(store.reserveWorkstation('seat-5', dateKey, ['evening'], now)).rejects.toThrow('该时段已经是你的预约')
    await expect(store.reserveWorkstation('seat-9', dateKey, ['evening'], now)).rejects.toThrow('该时段暂不可预约')
    expect(added).toHaveLength(0)
  })

  it('does not mutate slot state when the service write fails', async () => {
    const store = createWorkstationStore(createWorkstationSeed(now), async () => { throw new Error('服务写入失败') }, undefined, now)
    await expect(store.reserveWorkstation('seat-8', dateKey, ['morning'], now)).rejects.toThrow('服务写入失败')
    const seat = (await store.getWorkstationDay(dateKey)).workstations.find((item) => item.id === 'seat-8')!
    expect(seat.slots[0].status).toBe('available')
  })

  it('releases mine periods after the service reservation is cancelled', async () => {
    const store = createWorkstationStore(createWorkstationSeed(now), async () => {}, undefined, now)
    const record = await store.reserveWorkstation('seat-8', dateKey, ['morning', 'evening'], now)
    await store.releaseReservation(record)
    const seat = (await store.getWorkstationDay(dateKey)).workstations.find((item) => item.id === 'seat-8')!
    expect(seat.slots.map((slot) => slot.status)).toEqual(['available', 'available', 'available'])
  })
})
