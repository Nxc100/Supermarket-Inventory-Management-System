import type { Page, PageQuery } from './api'
export type WarningType = 'LOW' | 'NEAR' | 'EXPIRED'
export interface WarningQuery extends PageQuery {
  productId?: string
  type: WarningType
  categoryId?: string
}
export interface StockWarning {
  productId: string
  code: string
  name: string
  categoryName: string
  unit: string
  enabled: boolean
  canPurchase: boolean
  canSell: boolean
  frozen: boolean
  bookQty: number
  saleableQty: number
  stockLowerLimit: number
  nearExpiryDays: number
  batchItemId: string | null
  batchNo: string | null
  productionDate: string | null
  expiryDate: string | null
  remainingDays: number | null
  quantity: number
}
export interface WarningPage extends Page<StockWarning> {
  type: WarningType
  businessDate: string
  collectedAt: string
}
