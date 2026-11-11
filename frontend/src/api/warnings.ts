import { request } from './http'
import type { WarningQuery, WarningPage } from '../types/warning'
export const stockWarnings = (query: WarningQuery) =>
  request<WarningPage>('GET', '/inventory/warnings', undefined, query)
