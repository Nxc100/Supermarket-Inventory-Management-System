import type { Id, Page, PageQuery } from './api'
export type PaymentMethod = 'CASH' | 'MOCK_EPAY'
export type SaleStatus = 'COMPLETED' | 'CORRECTED'
export interface SaleLineInput {
  productId: Id
  quantity: number
  expectedPrice: string
}
export interface SaleCompleteInput {
  requestKey: string
  paymentMethod: PaymentMethod
  receivedAmount: string
  items: SaleLineInput[]
}
export interface SaleQuoteLine {
  productId: Id
  productCode: string
  productName: string
  unit: string
  quantity: number
  expectedPrice: string
  currentPrice: string
  lineAmount: string
  saleableQty: number
  priceChanged: boolean
  problem: string | null
}
export interface SaleQuote {
  canConfirm: boolean
  itemCount: number
  totalQty: number
  totalAmount: string
  checkedAt: string
  items: SaleQuoteLine[]
}
export interface SaleSummary {
  id: Id
  orderNo: string
  cashierId: Id
  cashierName: string
  status: SaleStatus
  itemCount: number
  totalQty: number
  totalAmount: string
  paymentMethod: PaymentMethod
  receivedAmount: string
  changeAmount: string
  completedAt: string
  correctedAt: string | null
  rowVersion: number
}
export interface SaleItem {
  id: Id
  lineNo: number
  productId: Id
  productCode: string
  productBarcode: string | null
  productName: string
  categoryId: Id
  categoryName: string
  specification: string | null
  unit: string
  unitPrice: string
  quantity: number
  lineAmount: string
}
export interface SaleDetail {
  order: SaleSummary
  items: SaleItem[]
  correction: {
    actorId: Id | null
    actorName: string | null
    occurredAt: string | null
    reason: string | null
  }
  canCorrectToday: boolean
  expiredSourceStock: boolean
}
export interface SaleQuery extends PageQuery {
  productId?: Id
  cashierId?: Id
  startDate?: string
  endDate?: string
  paymentMethod?: PaymentMethod
  status?: SaleStatus
}
export interface SalePage extends Page<SaleSummary> {
  totals: {
    originalAmount: string
    netAmount: string
    completedCount: number
    correctedCount: number
  }
}
export interface SaleContext {
  today: string
  defaultPaymentMethod: PaymentMethod
}
export interface CashierOption {
  id: Id
  name: string
}
export interface SaleCartLine {
  id: Id
  code: string
  barcode: string | null
  name: string
  specification: string | null
  unit: string
  imagePath: string | null
  salePrice: string
  saleableQty: number
  frozen: boolean
  quantity: number | null
}
export const paymentMethods: Record<PaymentMethod, string> = {
  CASH: '现金',
  MOCK_EPAY: '模拟电子支付',
}
export const saleStatuses: Record<SaleStatus, string> = { COMPLETED: '已完成', CORRECTED: '已纠错' }
