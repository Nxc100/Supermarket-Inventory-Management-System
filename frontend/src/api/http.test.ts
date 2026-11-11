import { afterEach, describe, expect, it, vi } from 'vitest'
import axios, { AxiosError, AxiosHeaders } from 'axios'
import { ApiError, http, registerExpired, request, setSession } from './http'

afterEach(() => {
  vi.restoreAllMocks()
  setSession(null)
  registerExpired(() => {})
})

describe('session-aware request handling', () => {
  it('allows model waiting on one request without changing checkout timeout defaults', async () => {
    const spy = vi
      .spyOn(http, 'request')
      .mockResolvedValue({ status: 200, data: { code: 'OK', data: {} } })
    await request('POST', '/suggestions/by-request/test/generate', {}, undefined, {
      timeout: 20000,
    })
    await request('POST', '/sales/complete', {})
    expect(spy.mock.calls[0]?.[0]).toMatchObject({ timeout: 20000 })
    expect(spy.mock.calls[1]?.[0]).not.toHaveProperty('timeout')
    expect(http.defaults.timeout).toBe(12000)
  })
  it('discards a successful old-account response after session switch', async () => {
    let resolveResponse: (value: unknown) => void = () => {}
    vi.spyOn(http, 'request').mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveResponse = resolve
        }),
    )
    setSession('old-token')
    const pending = request('GET', '/auth/me')
    setSession('new-token')
    resolveResponse({ status: 200, data: { code: 'OK', data: { realName: 'old account' } } })
    const error: unknown = await pending.catch((reason: unknown) => reason)
    expect(axios.isCancel(error)).toBe(true)
  })

  it('clears an expired session once across concurrent failed requests', async () => {
    const config = { headers: new AxiosHeaders() }
    const error = new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, {
      status: 401,
      statusText: 'Unauthorized',
      headers: {},
      config,
      data: { code: 'UNAUTHORIZED', message: '登录已失效', traceId: 'trace-1' },
    })
    vi.spyOn(http, 'request').mockRejectedValue(error)
    const expired = vi.fn()
    registerExpired(expired)
    setSession('expired-token')
    const results = await Promise.allSettled([
      request('GET', '/auth/me'),
      request('GET', '/categories'),
    ])
    expect(expired).toHaveBeenCalledTimes(1)
    expect(
      results.every((result) => result.status === 'rejected' && axios.isCancel(result.reason)),
    ).toBe(true)
  })

  it('keeps forbidden responses distinct from expired credentials', async () => {
    const config = { headers: new AxiosHeaders() }
    vi.spyOn(http, 'request').mockRejectedValue(
      new AxiosError('Forbidden', 'ERR_BAD_REQUEST', config, undefined, {
        status: 403,
        statusText: 'Forbidden',
        headers: {},
        config,
        data: { code: 'FORBIDDEN', message: '无权执行此操作', traceId: 'trace-2' },
      }),
    )
    const expired = vi.fn()
    registerExpired(expired)
    setSession('valid-token')
    const error: unknown = await request('GET', '/categories').catch((reason: unknown) => reason)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 403, code: 'FORBIDDEN', traceId: 'trace-2' })
    expect(expired).not.toHaveBeenCalled()
  })
})
