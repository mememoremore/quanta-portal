import { describe, expect, it } from 'vitest'
import { displayPersonName } from '../person-name'

describe('displayPersonName', () => {
  it('prefers a real name and otherwise keeps the existing nickname or login name', () => {
    expect(displayPersonName({ realName: ' 张三 ', nickName: '20251003142', userName: '20251003142' })).toBe('张三')
    expect(displayPersonName({ realName: '   ', nickName: '学长', userName: 'senior' })).toBe('学长')
    expect(displayPersonName({ userName: '2024001' }, '未登录')).toBe('2024001')
    expect(displayPersonName({}, '未登录')).toBe('未登录')
  })
})
