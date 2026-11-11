export type Id = string
export type RoleCode = 'ADMIN' | 'PURCHASER' | 'CASHIER'
export interface User {
  id: Id
  username: string
  realName: string
  phone: string | null
  avatarPath: string | null
  roleCode: RoleCode
  roleName: string
  enabled: boolean
  permissions: string[]
}
export interface Envelope<T> {
  code: string
  message: string
  data: T
  traceId: string
}
export interface Page<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}
export interface PageQuery {
  keyword: string
  enabled?: boolean
  pageNum: number
  pageSize: number
}
export interface Category {
  id: Id
  code: string
  name: string
  sortOrder: number
  enabled: boolean
  productCount: number
  createdAt: string
}
export interface CategoryInput {
  code: string
  name: string
  sortOrder: number
}
