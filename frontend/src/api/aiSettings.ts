import { request } from './http'
export interface AiSettings {
  version: number
  defaultModel: string
  models: string[]
  keyConfigured: boolean
  keySource: 'MANAGED' | 'LOCAL'
}
export interface AiSettingsInput {
  expectedVersion: number
  defaultModel: string
  models: string[]
  keyAction: 'KEEP' | 'REPLACE' | 'USE_LOCAL'
  apiKey?: string
}
export interface AiUsageRow {
  label: string
  calls: number
  successes: number
  failures: number
  pending: number
  reportedCalls: number
  unreportedCalls: number
  promptTokens: number
  completionTokens: number
  totalTokens: number
  cachedTokens: number
  cacheReportedCalls: number
}
export interface AiUsage {
  startDate: string
  endDate: string
  collectedAt: string
  totals: AiUsageRow
  models: AiUsageRow[]
  days: AiUsageRow[]
}
export interface AiUsageQuery {
  startDate?: string
  endDate?: string
  model?: string
  type?: 'INVENTORY' | 'SALES'
}
export const aiSettings = () => request<AiSettings>('GET', '/system/ai/settings')
export const saveAiSettings = (input: AiSettingsInput) =>
  request<AiSettings>('PUT', '/system/ai/settings', input)
export const aiUsage = (query: AiUsageQuery) =>
  request<AiUsage>('GET', '/system/ai/usage', undefined, query)
