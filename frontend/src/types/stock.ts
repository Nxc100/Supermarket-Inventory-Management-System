import type { Id, PageQuery } from './api'
export interface StockQuery extends PageQuery {
  categoryId?: Id
  lowStock?: boolean
}
export interface StockProduct {
  id: Id
  code: string
  barcode: string | null
  name: string
  categoryId: Id
  categoryName: string
  specification: string | null
  unit: string
  enabled: boolean
  canSell: boolean
  bookQty: number
  batchQty: number
  saleableQty: number
  expiredQty: number
  stockLowerLimit: number
  frozen: boolean
  lastStockedAt: string | null
}
export interface BatchQuery extends PageQuery {
  includeEmpty: boolean
}
export interface StockBatch {
  id: Id
  batchNo: string
  supplierBatchNo: string | null
  purchaseId: Id
  purchaseNo: string
  sourceReadable: boolean
  sourceStatus: string
  productionDate: string | null
  expiryDate: string | null
  remainingDays: number | null
  stockQty: number
  unitCost: string
  saleable: boolean
  frozen: boolean
  enabled: boolean
  canSell: boolean
  stockedAt: string
}
export const movementTypes = {
  PURCHASE_IN: '采购入库',
  PURCHASE_REVERSE: '采购撤销',
  SALE_OUT: '销售出库',
  SALE_CORRECTION: '销售纠错',
  STOCKTAKE_GAIN: '盘点盘盈',
  STOCKTAKE_LOSS: '盘点盘亏',
} as const
export interface MovementQuery extends PageQuery {
  productId?: Id
  batchId?: Id
  eventType?: keyof typeof movementTypes
  startDate?: string
  endDate?: string
}
export interface StockMovement {
  id: Id
  productId: Id
  productCode: string
  productName: string
  unit: string
  batchId: Id
  batchNo: string
  eventType: keyof typeof movementTypes
  businessNo: string
  deltaQty: number
  batchBefore: number
  batchAfter: number
  productBefore: number
  productAfter: number
  actorName: string
  reason: string | null
  occurredAt: string
  purchaseId: Id | null
  sourceReadable: boolean
}
