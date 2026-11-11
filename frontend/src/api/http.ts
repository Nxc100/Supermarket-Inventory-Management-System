import axios from 'axios'
import type { Envelope } from '../types/api'

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public code: string,
    public traceId?: string,
  ) {
    super(message)
  }
}
export const http = axios.create({ baseURL: '/api/v1', timeout: 12000 })
let accessToken: string | null = null
let sessionVersion = 0
let onExpired: () => void = () => {}
export function setSession(token: string | null) {
  accessToken = token
  sessionVersion++
}
export function registerExpired(handler: () => void) {
  onExpired = handler
}
/** Streaming requests share the same session identity and expiration path as Axios. */
export function streamSession() {
  const version = sessionVersion
  return {
    token: accessToken,
    assertCurrent() {
      if (version !== sessionVersion) throw new axios.CanceledError('Session changed')
    },
    expire() {
      if (version !== sessionVersion || !accessToken) return
      setSession(null)
      onExpired()
    },
  }
}
http.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  config.headers['X-Client-Trace'] = crypto.randomUUID()
  return config
})

/** Single unwrapping point; errors are displayed once by the owning page. */
export async function request<T>(
  method: string,
  url: string,
  data?: unknown,
  params?: unknown,
  options?: { timeout: number },
): Promise<T> {
  const version = sessionVersion
  try {
    const response = await http.request<Envelope<T>>({
      method,
      url,
      data,
      params,
      ...(options ?? {}),
    })
    if (version !== sessionVersion) throw new axios.CanceledError('Session changed')
    if (response.data.code !== 'OK')
      throw new ApiError(
        response.data.message,
        response.status,
        response.data.code,
        response.data.traceId,
      )
    return response.data.data
  } catch (error) {
    if (version !== sessionVersion) throw new axios.CanceledError('Session changed')
    if (axios.isAxiosError<Envelope<unknown>>(error)) {
      const status = error.response?.status ?? 0
      if (status === 401 && accessToken) {
        setSession(null)
        onExpired()
        throw new axios.CanceledError('Expired session already handled')
      }
      throw new ApiError(
        error.response?.data.message ?? '网络连接失败，请检查服务后重试',
        status,
        error.response?.data.code ?? 'NETWORK_ERROR',
        error.response?.data.traceId,
      )
    }
    throw error
  }
}
