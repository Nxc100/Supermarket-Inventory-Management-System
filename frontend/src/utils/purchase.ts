import Decimal from 'decimal.js'
import type { PurchaseStatus, ReconcileStatus } from '../types/purchase'
export const purchaseStatuses: Record<PurchaseStatus, string> = {
  DRAFT: '草稿',
  PENDING: '待审核',
  REJECTED: '已驳回',
  IN_STOCK: '已入库',
  CLOSED: '已关闭',
  REVERSED: '入库已撤销',
}
export const reconcileStatuses: Record<ReconcileStatus, string> = {
  UNRECONCILED: '未核对',
  DIFFERENCE: '有差异',
  MATCHED: '已核对',
  NA: '不适用',
}
export { businessTime as purchaseTime } from './format'
export function validMoney(value: string | null, positive = false) {
  if (value === null || !/^\d{1,14}(\.\d{1,2})?$/.test(value)) return false
  const decimal = new Decimal(value)
  return positive ? decimal.gt(0) : decimal.gte(0)
}
export function lineAmount(quantity: number | null, price: string | null) {
  return Number.isInteger(quantity) && validMoney(price)
    ? new Decimal(quantity!).mul(price!).toFixed(2)
    : '—'
}
