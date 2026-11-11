import { afterEach, describe, expect, it, vi } from 'vitest'
import { postEventStream, type StreamEvent } from './eventStream'
import { registerExpired, setSession } from './http'
afterEach(() => {
  vi.unstubAllGlobals()
  setSession(null)
  registerExpired(() => {})
})
describe('authenticated incremental event streams', () => {
  it('decodes split UTF-8 and dispatches only complete events before EOF without retries', async () => {
    const bytes = new TextEncoder().encode(
      'event: delta\r\ndata: {"text":"库存四件"}\r\n\r\nevent: result\ndata: {"done":true}\n\n',
    )
    const events: StreamEvent[] = []
    const stream = new ReadableStream<Uint8Array>({
      start(c) {
        for (const byte of bytes) c.enqueue(new Uint8Array([byte]))
        c.close()
      },
    })
    const fetcher = vi
      .fn()
      .mockResolvedValue(new Response(stream, { headers: { 'Content-Type': 'text/event-stream' } }))
    vi.stubGlobal('fetch', fetcher)
    setSession('private-session')
    await postEventStream('/suggestions/by-request/test/stream', (event) => events.push(event))
    expect(events).toEqual([
      { event: 'delta', data: { text: '库存四件' } },
      { event: 'result', data: { done: true } },
    ])
    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(fetcher.mock.calls[0]?.[0]).not.toContain('private-session')
    expect(fetcher.mock.calls[0]?.[1].headers.Authorization).toBe('Bearer private-session')
  })
  it('rejects old-account chunks after a session switch', async () => {
    let controller: ReadableStreamDefaultController<Uint8Array> | undefined
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          new ReadableStream<Uint8Array>({
            start(c) {
              controller = c
            },
          }),
          { headers: { 'Content-Type': 'text/event-stream' } },
        ),
      ),
    )
    setSession('old')
    const events = vi.fn()
    const pending = postEventStream('/test', events)
    await Promise.resolve()
    setSession('new')
    controller?.enqueue(new TextEncoder().encode('event: delta\ndata: {"text":"old"}\n\n'))
    controller?.close()
    await expect(pending).rejects.toThrow('Session changed')
    expect(events).not.toHaveBeenCalled()
  })
  it('expires the session for 401 and never retries a write', async () => {
    const expired = vi.fn()
    registerExpired(expired)
    setSession('expired')
    const fetcher = vi.fn().mockResolvedValue(new Response('{}', { status: 401 }))
    vi.stubGlobal('fetch', fetcher)
    await expect(postEventStream('/test', () => {})).rejects.toThrow('登录已失效')
    expect(expired).toHaveBeenCalledTimes(1)
    expect(fetcher).toHaveBeenCalledTimes(1)
  })
})
