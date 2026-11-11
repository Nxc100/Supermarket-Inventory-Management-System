import { request } from './http'
import type { Category, CategoryInput, Id, Page, PageQuery } from '../types/api'
export const categories = (query: PageQuery) =>
  request<Page<Category>>('GET', '/categories', undefined, query)
export const category = (id: Id) => request<Category>('GET', `/categories/${id}`)
export const createCategory = (input: CategoryInput) =>
  request<Category>('POST', '/categories', input)
export const updateCategory = (id: Id, input: CategoryInput) =>
  request<Category>('PATCH', `/categories/${id}`, input)
export const setCategoryStatus = (id: Id, enabled: boolean) =>
  request<Category>('POST', `/categories/${id}/status`, { enabled })
export const deleteCategory = (id: Id) => request<void>('DELETE', `/categories/${id}`)
