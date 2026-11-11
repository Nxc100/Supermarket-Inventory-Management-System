import { ApiError, streamSession } from './http'

export interface StreamEvent {
  event: string
  data: unknown
}

/** Incremental UTF-8 decoding and complete SSE event boundaries; never retries a POST. */
export async function postEventStream(
  path: string,
  onEvent: (event: StreamEvent) => void,
  signal?: AbortSignal,
) {
  const session = streamSession(),
    controller = new AbortController()
  const abort = () => controller.abort()
  signal?.addEventListener('abort', abort, { once: true })
  if (signal?.aborted) controller.abort()
  const timer = setTimeout(() => controller.abort(), 22000)
  let reader: ReadableStreamDefaultReader<Uint8Array> | undefined
  try {
    const response = await fetch(`/api/v1${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
        'X-Client-Trace': crypto.randomUUID(),
      },
      body: '{}',
      signal: controller.signal,
      cache: 'no-store',
    })
    session.assertCurrent()
    if (response.status === 401) {
      session.expire()
      throw new ApiError('登录已失效，请重新登录', 401, 'UNAUTHORIZED')
    }
    if (!response.ok) {
      const payload: unknown = await response.json().catch(() => null)
      const message =
        payload &&
        typeof payload === 'object' &&
        'message' in payload &&
        typeof payload.message === 'string'
          ? payload.message
          : '流式请求失败，请核实原请求结果'
      throw new ApiError(message, response.status, 'STREAM_FAILED')
    }
    if (!response.headers.get('Content-Type')?.startsWith('text/event-stream') || !response.body)
      throw new Error('未收到模型输出流，请查询原结果')
    reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = '',
      event = 'message',
      data: string[] = [],
      received = 0
    const line = (value: string) => {
      if (value.endsWith('\r')) value = value.slice(0, -1)
      if (!value) {
        if (data.length) {
          onEvent({ event, data: JSON.parse(data.join('\n')) as unknown })
          data = []
        }
        event = 'message'
      } else if (value.startsWith('event:')) event = value.slice(6).trim()
      else if (value.startsWith('data:')) data.push(value.slice(5).replace(/^ /, ''))
    }
    while (true) {
      const chunk = await reader.read()
      session.assertCurrent()
      if (chunk.done) break
      received += chunk.value.byteLength
      if (received > 2000000) throw new Error('流式响应过长，请查询原结果')
      buffer += decoder.decode(chunk.value, { stream: true })
      let end: number
      while ((end = buffer.indexOf('\n')) >= 0) {
        line(buffer.slice(0, end))
        buffer = buffer.slice(end + 1)
      }
    }
    session.assertCurrent()
  } finally {
    clearTimeout(timer)
    signal?.removeEventListener('abort', abort)
    await reader?.cancel().catch(() => {})
    controller.abort()
  }
}
