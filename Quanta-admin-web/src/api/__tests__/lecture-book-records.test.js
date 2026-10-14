import { beforeEach, describe, expect, it } from 'vitest'
import { getLectureRegistrations } from '../lecture-signups'
import { getBookBorrowRecords } from '../book-borrows'
import { setToken } from '@/utils/token'

describe('lecture and book record apis', () => {
  beforeEach(() => setToken('mock-token-ceo'))

  it('loads lecture registrations and quota', async () => {
    expect(await getLectureRegistrations()).toMatchObject({ total: 10, quota: 50 })
  })

  it('loads book borrow records', async () => {
    const result = await getBookBorrowRecords()
    expect(result.rows.some((row) => row.status === 'OVERDUE')).toBe(true)
  })
})
