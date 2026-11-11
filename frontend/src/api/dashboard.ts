import { request } from './http'
export interface DashboardTarget {
  label: string
  path: string
  query: Record<string, string>
}
export interface DashboardMetric extends DashboardTarget {
  code: string
  value: string
  unit: string
  note: string
}
export interface Dashboard {
  businessDate: string
  collectedAt: string
  scopeNote: string
  metrics: DashboardMetric[]
  shortcuts: DashboardTarget[]
}
export const dashboard = () => request<Dashboard>('GET', '/dashboard')
