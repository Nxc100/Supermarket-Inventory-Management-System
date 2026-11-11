import { request } from './http'
import type { StaffRole } from './staff'
export interface RoleSummary {
  code: StaffRole
  name: string
  dataScope: string
  scopeNote: string
  permissionCount: number
  version: number
  updatedAt: string
}
export interface PermissionNode {
  code: string
  label: string
  kind: 'MODULE' | 'PAGE' | 'ACTION'
  parent: string | null
}
export interface RoleDetail {
  role: RoleSummary
  nodes: PermissionNode[]
  permissions: string[]
  protectedPermissions: string[]
}
export const roles = () => request<RoleSummary[]>('GET', '/system/roles')
export const role = (code: StaffRole) => request<RoleDetail>('GET', `/system/roles/${code}`)
export const saveRole = (detail: RoleDetail, permissions: string[]) =>
  request<RoleDetail>('PUT', `/system/roles/${detail.role.code}/permissions`, {
    expectedVersion: detail.role.version,
    permissions,
  })
export const restoreRole = (detail: RoleDetail) =>
  request<RoleDetail>('POST', `/system/roles/${detail.role.code}/restore`, {
    expectedVersion: detail.role.version,
  })
