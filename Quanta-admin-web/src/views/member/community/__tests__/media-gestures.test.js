import { afterEach, describe, expect, it, vi } from 'vitest'
import { attachMediaGestures, createMediaTransform } from '../media-gestures'

afterEach(() => vi.unstubAllGlobals())
describe('bounded fullscreen media gestures', () => {
  it('fits, anchors zoom around the cursor and bounds drag and scale', () => {
    const view = createMediaTransform(1000, 500, 500, 500)
    expect(view.state).toMatchObject({ scale: 0.5, x: 0, y: 125 })
    view.zoomAt(1, 250, 250)
    expect(view.state).toMatchObject({ scale: 1, x: -250, y: 0 })
    view.moveBy(10000, 10000)
    expect(view.state).toMatchObject({ x: 0, y: 0 })
    view.zoomAt(1000, 0, 0)
    expect(view.state.scale).toBeLessThanOrEqual(8)
    view.fit(500, 500)
    expect(view.state.scale).toBe(0.5)
  })

  it('supports two-pointer zoom and drag without treating gesture completion as a tap', () => {
    vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} })
    const stage = document.createElement('div'), content = document.createElement('div')
    stage.append(content)
    vi.spyOn(stage, 'getBoundingClientRect').mockReturnValue({ width: 400, height: 400, left: 0, top: 0 })
    const onTap = vi.fn(), onChange = vi.fn()
    const gestures = attachMediaGestures(stage, content, { width: 400, height: 400, onTap, onChange })
    const pointer = (type, id, x, y) => {
      const event = new Event(type, { bubbles: true, cancelable: true })
      Object.assign(event, { pointerId: id, clientX: x, clientY: y, button: 0 })
      stage.dispatchEvent(event)
    }
    pointer('pointerdown', 1, 100, 200)
    pointer('pointerdown', 2, 200, 200)
    pointer('pointermove', 2, 300, 200)
    expect(onChange.mock.lastCall[0].scale).toBeGreaterThan(1)
    pointer('pointerup', 1, 100, 200)
    pointer('pointerup', 2, 300, 200)
    stage.click()
    expect(onTap).not.toHaveBeenCalled()
    pointer('pointerdown', 1, 100, 200)
    pointer('pointerup', 1, 100, 200)
    stage.click()
    expect(onTap).toHaveBeenCalledOnce()
    const count = onChange.mock.calls.length
    gestures.destroy()
    stage.dispatchEvent(new WheelEvent('wheel', { deltaY: -100, cancelable: true }))
    expect(onChange).toHaveBeenCalledTimes(count)
  })
  it('closes on touch pointerup even without a compatibility click, but never twice', () => {
    vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} })
    const stage = document.createElement('div'), content = document.createElement('div'), onTap = vi.fn()
    vi.spyOn(stage, 'getBoundingClientRect').mockReturnValue({ width: 400, height: 400, left: 0, top: 0 })
    const gestures = attachMediaGestures(stage, content, { width: 400, height: 400, onTap })
    for (const type of ['pointerdown', 'pointerup']) {
      const event = new Event(type, { cancelable: true })
      Object.assign(event, { pointerId: 1, pointerType: 'touch', button: 0, clientX: 100, clientY: 100 })
      stage.dispatchEvent(event)
    }
    expect(onTap).toHaveBeenCalledOnce()
    stage.click()
    expect(onTap).toHaveBeenCalledOnce()
    gestures.destroy()
  })
})
