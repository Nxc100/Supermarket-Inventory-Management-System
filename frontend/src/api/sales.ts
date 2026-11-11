import { request } from './http'
import type { Id } from '../types/api'
import type {
  SaleQuote,
  SaleLineInput,
  SaleCompleteInput,
  SaleDetail,
  SalePage,
  SaleQuery,
  SaleContext,
  CashierOption,
} from '../types/sale'
export const quoteSale = (items: SaleLineInput[]) =>
  request<SaleQuote>('POST', '/sales/quote', { items })
export const completeSale = (input: SaleCompleteInput) =>
  request<SaleDetail>('POST', '/sales/complete', input)
export const saleResult = (key: string) =>
  request<SaleDetail>('GET', `/sales/by-request/${encodeURIComponent(key)}`)
export const sale = (id: Id) => request<SaleDetail>('GET', `/sales/${id}`)
export const sales = (query: SaleQuery) => request<SalePage>('GET', '/sales', undefined, query)
export const saleContext = () => request<SaleContext>('GET', '/sales/context')
export const saleCashiers = () => request<CashierOption[]>('GET', '/sales/cashiers')
export const correctSale = (
  id: Id,
  expectedVersion: number,
  reason: string,
  goodsReturned: boolean,
) => request<SaleDetail>('POST', `/sales/${id}/correct`, { expectedVersion, reason, goodsReturned })
