import { describe, expect, it } from 'vitest'
import { calculateMemberNavigationMetrics } from './memberNavigation'

describe('member navigation metrics', () => {
  it('aligns an ordinary iPhone header to the menu capsule', () => {
    expect(calculateMemberNavigationMetrics(
      { windowWidth: 390, statusBarHeight: 44, safeArea: { top: 44 } },
      { top: 50, left: 296, width: 87, height: 32 },
    )).toEqual({ statusBarHeight: 44, navigationHeight: 44, totalHeight: 88, capsuleInsetRight: 104 })
  })

  it('places the navigation row below a Dynamic Island safe area', () => {
    const metrics = calculateMemberNavigationMetrics(
      { windowWidth: 393, statusBarHeight: 54, safeArea: { top: 59 } },
      { top: 59, left: 298, width: 87, height: 32 },
    )
    expect(metrics.statusBarHeight).toBe(59)
    expect(metrics.totalHeight).toBe(103)
  })

  it('supports Android status bars', () => {
    const metrics = calculateMemberNavigationMetrics(
      { windowWidth: 360, statusBarHeight: 24, safeArea: { top: 24 } },
      { top: 28, left: 266, width: 87, height: 32 },
    )
    expect(metrics).toEqual({ statusBarHeight: 24, navigationHeight: 44, totalHeight: 68, capsuleInsetRight: 104 })
  })

  it('uses conservative defaults for invalid capsule data', () => {
    expect(calculateMemberNavigationMetrics(
      { windowWidth: 375, statusBarHeight: 47, safeArea: { top: 47 } },
      { top: 0, left: 0, width: 0, height: 0 },
    )).toEqual({ statusBarHeight: 47, navigationHeight: 44, totalHeight: 91, capsuleInsetRight: 104 })
  })
})
