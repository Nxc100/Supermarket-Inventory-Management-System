import { request } from './http'
import type { Page, PageQuery } from '../types/api'
export interface SystemDefaults {
  stockLowerLimit: number
  nearExpiryDays: number
}
export const systemDefaults = () => request<SystemDefaults>('GET', '/system/defaults')
export const saveSystemDefaults = (current: SystemDefaults, expected: SystemDefaults) =>
  request<SystemDefaults>('PUT', '/system/defaults', {
    ...current,
    expectedStockLowerLimit: expected.stockLowerLimit,
    expectedNearExpiryDays: expected.nearExpiryDays,
  })
export interface OperationLog {
  id: string
  actorName: string
  action: string
  targetType: string
  targetId: string | null
  targetNo: string | null
  result: 'SUCCESS' | 'FAILURE'
  reason: string | null
  requestId: string | null
  occurredAt: string
}
export interface OperationLogDetail {
  record: OperationLog
  before: unknown
  after: unknown
}
export interface OperationLogQuery extends PageQuery {
  actor?: string
  action?: string
  result?: 'SUCCESS' | 'FAILURE'
  startDate?: string
  endDate?: string
}
export const operationLogs = (query: OperationLogQuery) =>
  request<Page<OperationLog>>('GET', '/system/logs', undefined, query)
export const operationLog = (id: string) => request<OperationLogDetail>('GET', `/system/logs/${id}`)
export const operationActions = () => request<string[]>('GET', '/system/logs/actions')
export const actionLabels: Record<string, string> = {
  PRODUCT_STATUS_CHANGE: '商品启停',
  SUPPLIER_STATUS_CHANGE: '供应商启停',
  CATEGORY_STATUS_CHANGE: '分类启停',
  STOCKTAKE_PENDING: '提交盘点审核',
  STOCKTAKE_COUNTING: '撤回盘点至录入',
  STOCKTAKE_REJECTED: '驳回盘点',
  STOCKTAKE_COMPLETED: '盘点审核调整',
  STOCKTAKE_CLOSED: '关闭盘点',
  CONFIG_UPDATE: '修改业务默认参数',
  AI_GENERATE_SUCCESS: '智能建议生成成功',
  AI_GENERATE_FAILURE: '智能建议生成失败',
  CATEGORY_CREATE: '新增分类',
  CATEGORY_EDIT: '编辑分类',
  CATEGORY_STATUS: '分类启停',
  CATEGORY_DELETE: '删除分类',
  PRODUCT_CREATE: '新增商品',
  PRODUCT_EDIT: '编辑商品',
  PRODUCT_STATUS: '商品启停',
  PRODUCT_DELETE: '删除商品',
  PRODUCT_IMAGE: '更换商品图片',
  SUPPLIER_CREATE: '新增供应商',
  SUPPLIER_EDIT: '编辑供应商',
  SUPPLIER_STATUS: '供应商启停',
  SUPPLIER_DELETE: '删除供应商',
  PURCHASE_CREATE: '新建采购',
  PURCHASE_EDIT: '编辑采购',
  PURCHASE_SUBMIT: '提交采购',
  PURCHASE_WITHDRAW: '撤回采购',
  PURCHASE_APPROVE: '采购审核入库',
  PURCHASE_REJECT: '驳回采购',
  PURCHASE_CLOSE: '关闭采购',
  PURCHASE_REVERSE: '撤销入库',
  PURCHASE_DELETE: '删除采购草稿',
  RECONCILE_DIFFERENCE: '登记核对差异',
  RECONCILE_MATCH: '确认核对一致',
  RECONCILE_WITHDRAW: '撤回采购核对',
  SALE_COMPLETE: '完成销售',
  SALE_CORRECT: '销售整单纠错',
  STOCKTAKE_START: '开始盘点',
  STOCKTAKE_SAVE: '保存实盘',
  STOCKTAKE_SUBMIT: '提交盘点',
  STOCKTAKE_WITHDRAW: '撤回盘点',
  STOCKTAKE_REJECT: '驳回盘点',
  STOCKTAKE_APPROVE: '盘点审核调整',
  STOCKTAKE_CLOSE: '关闭盘点',
  USER_CREATE: '新增账号',
  USER_EDIT: '编辑账号',
  USER_STATUS: '账号启停',
  USER_RESET_PASSWORD: '重置账号密码',
  USER_DELETE: '删除未使用账号',
  ROLE_SAVE: '保存岗位权限',
  ROLE_RESTORE: '恢复默认权限',
}
