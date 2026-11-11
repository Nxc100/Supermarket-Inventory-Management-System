import type { PageQuery } from './api'
export type SuggestionType = 'INVENTORY' | 'SALES'
export type SuggestionStatus = 'PENDING' | 'SUCCESS' | 'FAILURE'
export const suggestionTypes: Record<SuggestionType, string> = {
  INVENTORY: '库存优化建议',
  SALES: '销售经营分析建议',
}
export const suggestionStatuses: Record<SuggestionStatus, string> = {
  PENDING: '结果待完成',
  SUCCESS: '已生成',
  FAILURE: '生成失败',
}
export interface SuggestionContext {
  startDate: string
  endDate: string
  types: SuggestionType[]
  provider: string
  model: string
  waitTimeoutMs: number
  models: string[]
}
export interface SuggestionPreviewRequest {
  requestKey: string
  type: SuggestionType
  startDate: string
  endDate: string
  categoryId?: string
  productId?: string
  model?: string
}
export interface SuggestionPreview {
  requestKey: string
  type: SuggestionType
  periodStart: string
  periodEnd: string
  categoryId: string | null
  productId: string | null
  snapshotAt: string
  expiresAt: string
  provider: string
  model: string
  summary: Record<string, unknown>
}
export interface SuggestionRecord {
  id: string
  type: SuggestionType
  operatorName: string
  periodStart: string
  periodEnd: string
  snapshotAt: string
  provider: string
  modelName: string
  fixture: boolean
  status: SuggestionStatus
  requestedAt: string
  finishedAt: string | null
  errorMessage: string | null
}
export interface SuggestionDetail {
  record: SuggestionRecord
  filters: Record<string, unknown>
  summary: Record<string, unknown>
  content: string | null
  errorCode: string | null
  durationMs: number | null
  recoverablePending: boolean
  usage: {
    promptTokens: number | null
    completionTokens: number | null
    totalTokens: number | null
    cachedTokens: number | null
    requestId: string | null
  }
}
export interface SuggestionQuery extends PageQuery {
  type?: SuggestionType
  status?: SuggestionStatus
  startDate?: string
  endDate?: string
}
export interface SuggestionProductOption {
  id: string
  code: string
  name: string
}
