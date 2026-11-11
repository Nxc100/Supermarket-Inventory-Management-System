import { request } from './http'
import type {
  SalesStatisticsQuery,
  SalesStatistics,
  SlowProductPage,
  InventoryStatisticsQuery,
  InventoryStatistics,
} from '../types/statistics'
export const salesStatistics = (query: SalesStatisticsQuery) =>
  request<SalesStatistics>('GET', '/statistics/sales', undefined, query)
export const slowProducts = (query: SalesStatisticsQuery) =>
  request<SlowProductPage>('GET', '/statistics/sales/slow-products', undefined, query)
export const inventoryStatistics = (query: InventoryStatisticsQuery) =>
  request<InventoryStatistics>('GET', '/statistics/inventory', undefined, query)
