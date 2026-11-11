import type { Id, PageQuery } from './api'
export const stocktakeStatuses = {
  COUNTING: '盘点中',
  PENDING: '待审核',
  REJECTED: '已驳回',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
} as const
export const countReasons = {
  COUNT_ERROR: '清点差异',
  LOSS: '损耗',
  EXPIRY: '过期清理',
  OTHER: '其他',
} as const
export type StocktakeStatus = keyof typeof stocktakeStatuses
export interface StocktakeQuery extends PageQuery {
  status?: StocktakeStatus
  startDate?: string
  endDate?: string
}
export interface StocktakeSummary {
  id: Id
  orderNo: string
  operatorId: Id
  operatorName: string
  status: StocktakeStatus
  startedAt: string
  submittedAt: string | null
  completedAt: string | null
  rowVersion: number
  productCount: number
  batchCount: number
  countedCount: number
  bookQty: number
  actualQty: number | null
  differenceQty: number | null
}
export interface StocktakeItem {
  id: Id
  productId: Id
  productCode: string
  productName: string
  unit: string
  batchItemId: Id
  batchNo: string
  productionDate: string | null
  expiryDate: string | null
  bookQty: number
  actualQty: number | null
  differenceQty: number | null
  reasonType: keyof typeof countReasons | null
  reasonNote: string | null
}
export interface StocktakeDetail {
  order: StocktakeSummary
  items: StocktakeItem[]
  remark: string | null
  reviewerName: string | null
  reviewedAt: string | null
  rejectionReason: string | null
  closedAt: string | null
  closeReason: string | null
  activeFreeze: boolean
}
export interface StocktakeCountInput {
  id: Id
  actualQty: number | null
  reasonType: keyof typeof countReasons | null
  reasonNote: string | null
}
