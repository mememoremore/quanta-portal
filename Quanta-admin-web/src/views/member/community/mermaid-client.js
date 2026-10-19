import { validateMermaidSource } from './mermaid-options'

const abortError = () => new DOMException('图表渲染已取消', 'AbortError')

export function createMermaidRenderer({
  workerFactory = () => new Worker(new URL('./mermaid.worker.js', import.meta.url), { type: 'module' }),
  startupTimeout = 10000, renderTimeout = 2000,
} = {}) {
  let worker, ready = false, active, startupTimer, renderTimer, sequence = 0, destroyed = false
  const queue = []

  function resetWorker() {
    clearTimeout(startupTimer)
    clearTimeout(renderTimer)
    if (worker) {
      worker.onmessage = worker.onerror = worker.onmessageerror = null
      worker.terminate()
    }
    worker = null
    ready = false
  }

  function settle(job, error, result) {
    job.signal?.removeEventListener('abort', job.onAbort)
    if (error) job.reject(error)
    else job.resolve(result)
  }

  function failAll(error) {
    if (active) { settle(active, error); active = null }
    for (const job of queue.splice(0)) settle(job, error)
    resetWorker()
  }

  function pump() {
    if (destroyed || active || !queue.length) return
    if (!worker) {
      try {
        worker = workerFactory()
        startupTimer = setTimeout(() => failAll(new Error('图表引擎加载失败，请刷新后重试。')), startupTimeout)
        worker.onerror = worker.onmessageerror = () => failAll(new Error('图表渲染线程异常，请刷新后重试。'))
        worker.onmessage = ({ data }) => {
          if (data.ready) { clearTimeout(startupTimer); ready = true; pump(); return }
          if (!active || data.id !== active.id) return
          clearTimeout(renderTimer)
          const job = active
          active = null
          const error = data.error ? Object.assign(new Error(data.error.message || 'Mermaid 图表无法绘制'), { line: data.error.line, code: data.error.code }) : null
          settle(job, error, data.result)
          pump()
        }
      } catch { failAll(new Error('当前浏览器无法启动图表阅读器，请查看源码。')) }
      return
    }
    if (!ready) return
    active = queue.shift()
    renderTimer = setTimeout(() => {
      const job = active
      active = null
      resetWorker()
      settle(job, new Error('图表绘制超时，请减少节点或拆分图表。'))
      pump()
    }, renderTimeout)
    try { worker.postMessage({ id: active.id, source: active.source, options: active.options }) }
    catch { failAll(new Error('图表任务发送失败，请重试。')) }
  }

  function render(source, { width, prefix, fontFamily } = {}, { signal } = {}) {
    return new Promise((resolve, reject) => {
      try { validateMermaidSource(source) } catch (error) { reject(error); return }
      if (destroyed || signal?.aborted) { reject(abortError()); return }
      const job = { id: ++sequence, source, options: { width, prefix, fontFamily }, signal, resolve, reject }
      job.onAbort = () => {
        if (active === job) {
          active = null
          resetWorker()
          settle(job, abortError())
          pump()
        } else {
          const index = queue.indexOf(job)
          if (index !== -1) { queue.splice(index, 1); settle(job, abortError()) }
          if (!queue.length && !active) resetWorker()
        }
      }
      signal?.addEventListener('abort', job.onAbort, { once: true })
      queue.push(job)
      pump()
    })
  }

  return {
    render,
    destroy() { destroyed = true; failAll(abortError()) },
  }
}
