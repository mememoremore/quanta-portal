import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const functionSource = readFileSync(new URL('./function.vue', import.meta.url), 'utf8')
const homeSource = readFileSync(new URL('./home.vue', import.meta.url), 'utf8')
const scrollAreaRule = functionSource.match(/\.scroll-area\s*\{([^}]*)\}/)?.[1] ?? ''
const circleListRule = functionSource.match(/\.circle-list\s*\{([^}]*)\}/)?.[1] ?? ''
const homeRule = homeSource.match(/\.freshman-home\s*\{([^}]*)\}/)?.[1] ?? ''
const logoStageRule = homeSource.match(/\.logo-stage\s*\{([^}]*)\}/)?.[1] ?? ''
const logoHaloRule = homeSource.match(/\.logo-halo\s*\{([^}]*)\}/)?.[1] ?? ''
const logoCircleRule = homeSource.match(/\.logo-circle\s*\{([^}]*)\}/)?.[1] ?? ''

describe('freshman page bottom spacing', () => {
  it('keeps the final function circle above the floating navigation', () => {
    expect(scrollAreaRule).toContain('padding-bottom: 0')
    expect(circleListRule).toContain('padding: 50rpx 0 calc(250rpx + env(safe-area-inset-bottom))')
  })

  it('keeps the end of the home page above the floating navigation', () => {
    expect(homeRule).toContain('padding-bottom: calc(320rpx + env(safe-area-inset-bottom))')
  })
})

describe('freshman home logo flip', () => {
  it('provides a reversible two-sided slogan card', () => {
    expect(homeSource).toContain('const logoFlipped = ref(false)')
    expect(homeSource).toContain('const toggleLogo = () =>')
    expect(homeSource).toContain('@click="toggleLogo"')
    expect(homeSource.match(/class="logo-face/g)).toHaveLength(2)
    expect(homeSource).toContain('nothing but')
    expect(homeSource).toContain('profes')
    expect(homeSource).toContain('>S</text>')
    expect(homeSource).toContain('>O</text>')
    expect(homeSource).toContain('transform-style: preserve-3d')
    expect(homeSource).toContain('backface-visibility: hidden')
    expect(homeSource).toContain('transition: transform 0.65s')
    expect(homeSource).toContain('transform: rotateY(180deg)')
  })

  it('keeps the halo fixed while only the circular card rotates', () => {
    expect(homeSource).toContain('class="logo-stage"')
    expect(homeSource).toContain('class="logo-halo"')
    expect(logoStageRule).toContain('perspective: 1200rpx')
    expect(logoStageRule).not.toContain('background:')
    expect(logoHaloRule).toContain('inset: -128rpx')
    expect(logoHaloRule).toContain('rgba(255, 102, 0, 0) 74%')
    expect(logoCircleRule).not.toContain('box-shadow:')
    expect(logoCircleRule).toContain('transition: transform 0.65s')
  })
})
