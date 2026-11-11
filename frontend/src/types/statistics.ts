import type { Page, PageQuery } from './api'
import type { StockProduct } from './stock'
export interface SalesStatisticsQuery extends PageQuery {
  productId?: string
  categoryId?: string
  year?: number
  startDate?: string
  endDate?: string
  windowDays: number
}
export interface SalesProductAggregate {
  productId: string
  code: string
  name: string
  categoryId: string
  categoryName: string
  unit: string
  netQty: number
  netAmount: string
}
export interface SalesStatistics extends Page<SalesProductAggregate> {
  year: number
  startDate: string
  endDate: string
  collectedAt: string
  totals: { netQty: number; netAmount: string; completedCount: number; correctedCount: number }
  monthly: { month: number; netQty: number; netAmount: string }[]
  daily: { date: string; netQty: number; netAmount: string }[]
  hotProducts: SalesProductAggregate[]
}
export interface SlowProduct {
  productId: string
  code: string
  name: string
  unit: string
  saleableQty: number
  firstStockedAt: string
  netQty: number
  observation: 'STAGNANT' | 'INSUFFICIENT_HISTORY'
}
export interface SlowProductPage extends Page<SlowProduct> {
  windowStart: string
  windowEnd: string
  windowDays: number
  collectedAt: string
}
export interface InventoryStatisticsQuery extends PageQuery {
  productId?: string
  categoryId?: string
}
export interface InventoryStatistics extends Page<StockProduct> {
  filters: {
    keyword: string
    categoryId: string | null
    productId: string | null
    enabled: boolean | null
  }
  businessDate: string
  collectedAt: string
  totals: {
    bookQty: number
    saleableQty: number
    expiredQty: number
    lowProductCount: number
    nearBatchCount: number
    expiredBatchCount: number
  }
  categories: { categoryId: string; categoryName: string; bookQty: number; percentage: string }[]
}
