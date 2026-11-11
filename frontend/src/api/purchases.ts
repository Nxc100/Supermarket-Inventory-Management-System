import { request } from './http'
import type { Id, Page, PageQuery } from '../types/api'
import type {
  PurchaseContext,
  PurchaseDetail,
  PurchaseInput,
  PurchaseQuery,
  PurchaseSummary,
  ReconcileInput,
  SupplierOption,
  SupplierPurchaseSummary,
  SupplierSummaryQuery,
} from '../types/purchase'

export const purchases = (query: PurchaseQuery) =>
  request<Page<PurchaseSummary>>('GET', '/purchases', undefined, query)
export const purchase = (id: Id) => request<PurchaseDetail>('GET', `/purchases/${id}`)
export const savePurchase = (id: Id | undefined, input: PurchaseInput) =>
  request<PurchaseDetail>(id ? 'PATCH' : 'POST', id ? `/purchases/${id}` : '/purchases', input)
export const purchaseContext = () => request<PurchaseContext>('GET', '/purchases/context')
export const supplierOptions = (query: PageQuery) =>
  request<Page<SupplierOption>>('GET', '/purchases/supplier-options', undefined, query)
export const supplierSummary = (query: SupplierSummaryQuery) =>
  request<Page<SupplierPurchaseSummary>>('GET', '/purchases/supplier-summary', undefined, query)
export type PurchaseCommand =
  | 'submit'
  | 'withdraw'
  | 'approve'
  | 'reject'
  | 'close'
  | 'reverse'
  | 'withdraw-reconciliation'
export const purchaseCommand = (
  id: Id,
  command: PurchaseCommand,
  expectedVersion: number,
  reason?: string,
  expectedReconcileVersion?: number,
) =>
  request<PurchaseDetail>('POST', `/purchases/${id}/${command}`, {
    expectedVersion,
    reason,
    expectedReconcileVersion,
  })
export const deletePurchase = (id: Id, expectedVersion: number) =>
  request<void>('DELETE', `/purchases/${id}`, { expectedVersion })
export const reconcilePurchase = (id: Id, match: boolean, input: ReconcileInput) =>
  request<PurchaseDetail>(
    'POST',
    `/purchases/${id}/${match ? 'reconcile-match' : 'reconcile-difference'}`,
    input,
  )
