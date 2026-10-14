import { describe, expect, it } from 'vitest'
import { lectureSignupHandlers } from '../lecture-signups'
import { bookBorrowHandlers } from '../book-borrows'

const config = (token) => ({ headers: { Authorization: `Bearer ${token}` } })

describe('lecture and book record permissions', () => {
  it('allows authorized users', () => {
    expect(lectureSignupHandlers[0].handle(config('mock-token-ceo')).code).toBe(200)
    expect(bookBorrowHandlers[0].handle(config('mock-token-product-interviewer')).code).toBe(200)
  })

  it('keeps managers out of lecture records while management can view borrows', () => {
    expect(lectureSignupHandlers[0].handle(config('mock-token-product-interviewer')).code).toBe(403)
    expect(bookBorrowHandlers[0].handle(config('mock-token-product-manager')).code).toBe(200)
  })
})
