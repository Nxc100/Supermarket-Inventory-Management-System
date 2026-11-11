import { request } from './http'
import type { Id, Page } from '../types/api'
import type {
  StocktakeQuery,
  StocktakeDetail,
  StocktakeSummary,
  StocktakeCountInput,
} from '../types/stocktake'
export const stocktakes = (query: StocktakeQuery) =>
  request<Page<StocktakeSummary>>('GET', '/stocktakes', undefined, query)
export const stocktake = (id: Id) => request<StocktakeDetail>('GET', `/stocktakes/${id}`)
export const startStocktake = (productIds: Id[], remark: string) =>
  request<StocktakeDetail>('POST', '/stocktakes', { productIds, remark })
export const saveStocktake = (id: Id, expectedVersion: number, items: StocktakeCountInput[]) =>
  request<StocktakeDetail>('PUT', `/stocktakes/${id}/counts`, { expectedVersion, items })
export type StocktakeCommand = 'submit' | 'withdraw' | 'approve' | 'reject' | 'close'
export const stocktakeCommand = (
  id: Id,
  action: StocktakeCommand,
  expectedVersion: number,
  reason?: string,
) =>
  request<StocktakeDetail>('POST', `/stocktakes/${id}/${action}`, {
    expectedVersion,
    ...(reason !== undefined ? { reason } : {}),
  })
