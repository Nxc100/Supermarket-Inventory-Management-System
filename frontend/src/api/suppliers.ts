import { request } from './http'
import type { Id, Page } from '../types/api'
import type { Supplier, SupplierInput, SupplierQuery } from '../types/catalog'
export const suppliers = (query: SupplierQuery) =>
  request<Page<Supplier>>('GET', '/suppliers', undefined, query)
export const supplier = (id: Id) => request<Supplier>('GET', `/suppliers/${id}`)
export const createSupplier = (input: SupplierInput) =>
  request<Supplier>('POST', '/suppliers', input)
export const updateSupplier = (id: Id, input: SupplierInput) =>
  request<Supplier>('PATCH', `/suppliers/${id}`, input)
export const setSupplierStatus = (id: Id, enabled: boolean) =>
  request<Supplier>('POST', `/suppliers/${id}/status`, { enabled })
export const deleteSupplier = (id: Id) => request<void>('DELETE', `/suppliers/${id}`)
