import type { Id, PageQuery } from './api'

export interface CategoryOption {
  id: Id
  code: string
  name: string
  enabled: boolean
}
export interface ProductInput {
  code: string
  barcode: string | null
  name: string
  categoryId: Id
  specification: string | null
  unit: string
  referenceCost: string | null
  salePrice: string
  managesExpiry: boolean
  shelfLifeDays: number | null
  nearExpiryDays: number
  stockLowerLimit: number
  canPurchase: boolean
  canSell: boolean
  remark: string | null
}
export interface Product extends ProductInput {
  id: Id
  categoryName: string
  imagePath: string | null
  enabled: boolean
  stockQty: number
  referenced: boolean
  createdAt: string
}
export interface ProductQuery extends PageQuery {
  categoryId?: Id
  managesExpiry?: boolean
  canPurchase?: boolean
  canSell?: boolean
}
export interface ProductDefaults {
  stockLowerLimit: number
  nearExpiryDays: number
  units: string[]
}
export interface PurchaseProduct {
  id: Id
  code: string
  barcode: string | null
  name: string
  categoryId: Id
  categoryName: string
  specification: string | null
  unit: string
  referenceCost: string | null
  managesExpiry: boolean
  shelfLifeDays: number | null
  imagePath: string | null
  frozen: boolean
}
export interface SaleProduct {
  id: Id
  code: string
  barcode: string | null
  name: string
  categoryId: Id
  categoryName: string
  specification: string | null
  unit: string
  salePrice: string
  imagePath: string | null
  saleableQty: number
  frozen: boolean
}
export interface SupplierInput {
  name: string
  contactName: string
  contactPhone: string
  address: string | null
  remark: string | null
}
export interface Supplier extends SupplierInput {
  id: Id
  code: string
  enabled: boolean
  purchaseCount: number
  createdAt: string
}
export interface SupplierQuery extends PageQuery {
  contactName: string
}
