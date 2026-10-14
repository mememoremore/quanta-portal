import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const read = (path: string) => readFileSync(resolve(process.cwd(), 'src', path), 'utf8')

describe('production page backend integration', () => {
  it('uses real APIs for supported business reads and writes', () => {
    expect(read('pages/freshman/join-us/submit.vue')).toContain("api/recruitment")
    expect(read('pages/freshman/join-us/process.vue')).toContain("api/recruitment")
    expect(read('pages/freshman/talk/talk.vue')).toContain("api/activity")
    expect(read('pages/freshman/talk/talk.vue')).toContain('signupActivity')
    expect(read('pages/freshman/elite-share/elite-share.vue')).toContain('getMyActivitySignup')
    expect(read('pages/member/contact/contact.vue')).toContain("api/member")
    expect(read('pages/member/profile/profile.vue')).toContain("api/user")
    expect(read('pages/member/library/library.vue')).toContain("api/library")
    expect(read('pages/member/services/services.vue')).toContain("api/memberServices")
    expect(read('pages/member/shirt-order/shirt-order.vue')).toContain("api/clothing")
    expect(read('pages/member/function/function.vue')).toContain("api/material")
    expect(read('pages/member/function/function.vue')).toContain("api/notice")
  })

  it('retains mocks only for confirmed endpoint gaps', () => {
    expect(read('pages/member/workstation/workstation.vue')).toContain('workstationMock')
    expect(read('pages/member/business-card/edit.vue')).toContain('memberMock')
    expect(read('pages/freshman/elite-share/elite-share.vue')).toContain('getMockEliteShareGuests')
    expect(read('pages/freshman/talk/talk.vue')).not.toContain('saveActivitySignup')
    expect(read('pages/freshman/elite-share/elite-share.vue')).not.toContain('getActivitySignup')
  })

  it('matches the hardened backend upload and timeout limits', () => {
    expect(read('utils/request.ts')).toContain('timeout = 10000')
    expect(read('api/recruitment.ts')).toContain('timeout: 10000')
    expect(read('api/recruitment.ts')).toContain('handleUnauthorized()')
    expect(read('pages/freshman/join-us/submit.vue')).toContain('uni.compressImage')
    expect(read('pages/freshman/join-us/submit.vue')).toContain("submitting ? '投递中'")
  })
})
