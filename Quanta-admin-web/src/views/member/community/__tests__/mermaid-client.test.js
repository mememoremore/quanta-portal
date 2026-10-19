import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMermaidRenderer } from '../mermaid-client'

class FakeWorker {
  postMessage = vi.fn()
  terminate = vi.fn()
  send(data) { this.onmessage?.({ data }) }
}

describe('bounded Mermaid worker queue', () => {
  let workers, renderer
  beforeEach(() => {
    vi.useFakeTimers()
    workers = []
    renderer = createMermaidRenderer({ workerFactory: () => { const worker = new FakeWorker(); workers.push(worker); return worker }, startupTimeout: 100, renderTimeout: 20 })
  })
  afterEach(() => { renderer.destroy(); vi.useRealTimers() })

  it('loads only on first use, queues work and forwards only drawing inputs', async () => {
    expect(workers).toHaveLength(0)
    const first = renderer.render('flowchart LR\n A-->B', { width: 320, prefix: 'a', images: true })
    const second = renderer.render('sequenceDiagram\n A->>B:Hi', {})
    expect(workers[0].postMessage).not.toHaveBeenCalled()
    workers[0].send({ ready: true })
    const job1 = workers[0].postMessage.mock.lastCall[0]
    expect(job1.options).not.toHaveProperty('images')
    workers[0].send({ id: job1.id, result: { svg: '<svg/>', width: 100, height: 100 } })
    await expect(first).resolves.toMatchObject({ svg: '<svg/>' })
    const job2 = workers[0].postMessage.mock.lastCall[0]
    expect(job2.id).not.toBe(job1.id)
    workers[0].send({ id: job2.id, error: { message: '语法有误', line: 2 } })
    await expect(second).rejects.toThrow('语法有误')
  })

  it('terminates an over-time render and starts queued work on a fresh worker', async () => {
    const timed = renderer.render('flowchart LR\n A-->B', {}).catch(error => error)
    const next = renderer.render('flowchart LR\n C-->D', {})
    workers[0].send({ ready: true })
    await vi.advanceTimersByTimeAsync(21)
    expect((await timed).message).toContain('超时')
    expect(workers[0].terminate).toHaveBeenCalledOnce()
    workers[1].send({ ready: true })
    const id = workers[1].postMessage.mock.lastCall[0].id
    workers[1].send({ id, result: { svg: '<svg/>' } })
    await expect(next).resolves.toMatchObject({ svg: '<svg/>' })
  })

  it('rejects engine startup failures rather than leaving queued diagrams loading forever', async () => {
    const pending = renderer.render('flowchart LR\n A-->B', {}).catch(error => error)
    await vi.advanceTimersByTimeAsync(101)
    expect((await pending).message).toContain('加载失败')
    expect(workers[0].terminate).toHaveBeenCalledOnce()
  })

  it('aborts active and queued jobs and destroys the worker on cleanup', async () => {
    const active = new AbortController(), queued = new AbortController()
    const first = renderer.render('flowchart LR\n A-->B', {}, { signal: active.signal }).catch(error => error)
    workers[0].send({ ready: true })
    const second = renderer.render('flowchart LR\n C-->D', {}, { signal: queued.signal }).catch(error => error)
    queued.abort()
    active.abort()
    expect((await first).name).toBe('AbortError')
    expect((await second).name).toBe('AbortError')
    expect(workers[0].terminate).toHaveBeenCalled()
  })

  it('rejects oversized source without loading the engine', async () => {
    await expect(renderer.render('a'.repeat(8001), {})).rejects.toThrow('8000')
    expect(workers).toHaveLength(0)
  })
})
