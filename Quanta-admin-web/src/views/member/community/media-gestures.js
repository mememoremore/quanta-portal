const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

export function createMediaTransform(width, height, viewportWidth, viewportHeight) {
  const state = { scale: 1, x: 0, y: 0 }
  let fitScale = 1, viewWidth, viewHeight
  function constrain() {
    const w = width * state.scale, h = height * state.scale
    state.x = w <= viewWidth ? (viewWidth - w) / 2 : clamp(state.x, viewWidth - w, 0)
    state.y = h <= viewHeight ? (viewHeight - h) / 2 : clamp(state.y, viewHeight - h, 0)
  }
  function fit(w, h) {
    viewWidth = Math.max(1, w); viewHeight = Math.max(1, h)
    fitScale = Math.min(1, viewWidth / width, viewHeight / height)
    state.scale = fitScale; state.x = 0; state.y = 0
    constrain()
  }
  function zoomAt(scale, x = viewWidth / 2, y = viewHeight / 2) {
    const next = clamp(scale, fitScale, Math.max(8, fitScale))
    const ratio = next / state.scale
    state.x = x - (x - state.x) * ratio
    state.y = y - (y - state.y) * ratio
    state.scale = next
    constrain()
  }
  function moveBy(x, y) { state.x += x; state.y += y; constrain() }
  fit(viewportWidth, viewportHeight)
  return { state, fit, zoomAt, moveBy }
}

export function attachMediaGestures(stage, content, { width, height, onChange = () => {}, onTap = () => {} }) {
  const bounds = () => stage.getBoundingClientRect()
  const initial = bounds()
  const view = createMediaTransform(width, height, initial.width, initial.height)
  const points = new Map(), origins = new Map()
  let moved = false, disposed = false, tapHandled = false
  function paint() {
    if (disposed) return
    const { x, y, scale } = view.state
    content.style.transform = `translate3d(${x}px, ${y}px, 0) scale(${scale})`
    onChange({ ...view.state })
  }
  function zoomBy(ratio, x, y) { view.zoomAt(view.state.scale * ratio, x, y); paint() }
  function reset() {
    const rect = bounds()
    points.clear(); origins.clear()
    view.fit(rect.width, rect.height)
    paint()
  }
  function point(event) { const rect = bounds(); return { x: event.clientX - rect.left, y: event.clientY - rect.top } }
  function down(event) {
    if (event.button !== 0) return
    if (!points.size) { moved = false; tapHandled = false }
    const value = point(event)
    points.set(event.pointerId, value); origins.set(event.pointerId, value)
    if (points.size > 1) moved = true
    try { stage.setPointerCapture?.(event.pointerId) } catch { /* Synthetic/cancelled pointers cannot be captured. */ }
    // touch-action handles browser gestures. Cancelling pointerdown also suppresses
    // the compatibility click on touchscreens, so a genuine tap could never close.
  }
  const distance = pair => Math.hypot(pair[0].x - pair[1].x, pair[0].y - pair[1].y)
  const midpoint = pair => ({ x: (pair[0].x + pair[1].x) / 2, y: (pair[0].y + pair[1].y) / 2 })
  function move(event) {
    if (!points.has(event.pointerId)) return
    const previous = points.get(event.pointerId), next = point(event), origin = origins.get(event.pointerId)
    if (Math.hypot(next.x - origin.x, next.y - origin.y) > 6) moved = true
    const before = [...points.values()].slice(0, 2)
    points.set(event.pointerId, next)
    if (points.size === 1) view.moveBy(next.x - previous.x, next.y - previous.y)
    else {
      const after = [...points.values()].slice(0, 2), oldCenter = midpoint(before), center = midpoint(after)
      view.zoomAt(view.state.scale * distance(after) / Math.max(1, distance(before)), oldCenter.x, oldCenter.y)
      view.moveBy(center.x - oldCenter.x, center.y - oldCenter.y)
    }
    paint()
    event.preventDefault()
  }
  function up(event) {
    const touchTap = event.type === 'pointerup' && event.pointerType === 'touch' && points.has(event.pointerId) && points.size === 1 && !moved
    if (event.type === 'pointercancel') moved = true
    points.delete(event.pointerId); origins.delete(event.pointerId)
    try { stage.releasePointerCapture?.(event.pointerId) } catch { /* Already released by the browser. */ }
    // Mobile browsers do not consistently synthesize click after captured touch
    // gestures. Complete a genuine tap here and consume any later compatibility click.
    if (touchTap) { tapHandled = true; onTap() }
  }
  function wheel(event) { event.preventDefault(); const p = point(event); zoomBy(Math.exp(clamp(-event.deltaY * 0.002, -0.5, 0.5)), p.x, p.y) }
  function click(event) {
    event.preventDefault(); event.stopPropagation()
    if (tapHandled) { tapHandled = false; return }
    if (moved || points.size) { moved = false; return }
    onTap()
  }
  function drag(event) { event.preventDefault() }
  const listeners = { pointerdown: down, pointermove: move, pointerup: up, pointercancel: up, lostpointercapture: up, click, dragstart: drag }
  for (const [type, listener] of Object.entries(listeners)) stage.addEventListener(type, listener)
  stage.addEventListener('wheel', wheel, { passive: false })
  const observer = new ResizeObserver(reset)
  observer.observe(stage)
  paint()
  return { zoomBy, reset, destroy() {
    disposed = true; observer.disconnect(); points.clear(); origins.clear()
    for (const [type, listener] of Object.entries(listeners)) stage.removeEventListener(type, listener)
    stage.removeEventListener('wheel', wheel)
  } }
}
