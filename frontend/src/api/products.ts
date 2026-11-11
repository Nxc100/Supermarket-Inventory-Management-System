import { request } from './http'
import type { Id, Page } from '../types/api'
import type {
  Product,
  ProductInput,
  ProductQuery,
  ProductDefaults,
  CategoryOption,
  PurchaseProduct,
  SaleProduct,
} from '../types/catalog'
export const products = (query: ProductQuery) =>
  request<Page<Product>>('GET', '/products', undefined, query)
export const product = (id: Id) => request<Product>('GET', `/products/${id}`)
export const productDefaults = () => request<ProductDefaults>('GET', '/products/defaults')
export const categoryOptions = () => request<CategoryOption[]>('GET', '/products/category-options')
export const createProduct = (input: ProductInput) => request<Product>('POST', '/products', input)
export const updateProduct = (id: Id, input: ProductInput) =>
  request<Product>('PATCH', `/products/${id}`, input)
export const setProductStatus = (id: Id, enabled: boolean) =>
  request<Product>('POST', `/products/${id}/status`, { enabled })
export const deleteProduct = (id: Id) => request<void>('DELETE', `/products/${id}`)
export const uploadProductImage = (id: Id, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request<Product>('POST', `/products/${id}/image`, form)
}
export const removeProductImage = (id: Id) => request<Product>('DELETE', `/products/${id}/image`)
export const purchaseProducts = (query: ProductQuery) =>
  request<Page<PurchaseProduct>>('GET', '/product-lookups/purchase', undefined, query)
export const saleProducts = (query: ProductQuery) =>
  request<Page<SaleProduct>>('GET', '/product-lookups/sale', undefined, query)
