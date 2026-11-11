import { request } from './http'
import type { Page, PageQuery, User } from '../types/api'
export type StaffRole = User['roleCode']
export const staffRoles: Record<StaffRole, string> = {
  ADMIN: '超市管理员',
  PURCHASER: '采购入库操作员',
  CASHIER: '前台收银操作员',
}
export interface Staff {
  id: string
  username: string
  realName: string
  phone: string | null
  roleCode: StaffRole
  roleName: string
  enabled: boolean
  createdAt: string
  updatedAt: string
  unfinishedPurchases: number
  activeStocktakes: number
  businessReferenced: boolean
}
export interface StaffQuery extends PageQuery {
  role?: StaffRole
}
export interface StaffCreate {
  username: string
  realName: string
  phone: string | null
  role: StaffRole
  password: string
  confirmPassword: string
}
export interface StaffEdit {
  realName: string
  phone: string | null
  role: StaffRole
  expectedUpdatedAt: string
}
export interface StaffTask {
  type: 'PURCHASE' | 'STOCKTAKE'
  id: string
  orderNo: string
  status: string
  createdAt: string
}
export const staffPage = (query: StaffQuery) =>
  request<Page<Staff>>('GET', '/system/users', undefined, query)
export const staff = (id: string) => request<Staff>('GET', `/system/users/${id}`)
export const createStaff = (input: StaffCreate) => request<Staff>('POST', '/system/users', input)
export const editStaff = (id: string, input: StaffEdit) =>
  request<Staff>('PUT', `/system/users/${id}`, input)
export const staffState = (row: Staff, enabled: boolean) =>
  request<Staff>('PATCH', `/system/users/${row.id}/state`, {
    enabled,
    expectedUpdatedAt: row.updatedAt,
  })
export const resetStaffPassword = (row: Staff, password: string, confirmPassword: string) =>
  request<Staff>('POST', `/system/users/${row.id}/reset-password`, {
    password,
    confirmPassword,
    expectedUpdatedAt: row.updatedAt,
  })
export const deleteStaff = (row: Staff) =>
  request<void>('DELETE', `/system/users/${row.id}`, { expectedUpdatedAt: row.updatedAt })
export const staffTasks = (id: string, query: PageQuery) =>
  request<Page<StaffTask>>('GET', `/system/users/${id}/tasks`, undefined, query)
