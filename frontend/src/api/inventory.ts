import { request } from './http'
import type { Id, Page, PageQuery } from '../types/api'
import type { SaleProduct } from '../types/catalog'
import type {
  StockQuery,
  StockProduct,
  BatchQuery,
  StockBatch,
  MovementQuery,
  StockMovement,
} from '../types/stock'
export interface InventoryQuery extends PageQuery {
  categoryId?: Id
}
export const saleableInventory = (query: InventoryQuery) =>
  request<Page<SaleProduct>>('GET', '/inventory/saleable-products', undefined, query)
export const stockInventory = (query: StockQuery) =>
  request<Page<StockProduct>>('GET', '/inventory/products', undefined, query)
export const stockBatches = (id: Id, query: BatchQuery) =>
  request<Page<StockBatch>>('GET', `/inventory/products/${id}/batches`, undefined, query)
export const stockMovements = (query: MovementQuery) =>
  request<Page<StockMovement>>('GET', '/inventory/movements', undefined, query)
