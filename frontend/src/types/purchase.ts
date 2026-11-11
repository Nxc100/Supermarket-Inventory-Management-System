import type { Id, PageQuery } from './api'

export type PurchaseStatus = 'DRAFT' | 'PENDING' | 'REJECTED' | 'IN_STOCK' | 'CLOSED' | 'REVERSED'
export type ReconcileStatus = 'UNRECONCILED' | 'DIFFERENCE' | 'MATCHED' | 'NA'
export interface PurchaseQuery extends PageQuery {
  supplierId?: Id
  status?: PurchaseStatus
  reconcileStatus?: ReconcileStatus
  dateBasis: 'CREATED' | 'STOCKED' | 'EVENT'
  startDate?: string
  endDate?: string
}
export interface PurchaseSummary {
  id: Id
  orderNo: string
  supplierId: Id
  supplierName: string
  operatorId: Id
  operatorName: string
  status: PurchaseStatus
  totalQty: number
  totalReceivedQty: number
  totalAmount: string
  reconcileStatus: ReconcileStatus
  rowVersion: number
  reconcileVersion: number
  everSubmitted: boolean
  expectedArrivalDate: string | null
  arrivedAt: string | null
  submittedAt: string | null
  stockedAt: string | null
  createdAt: string
  remark: string | null
}
export interface PurchaseLineInput {
  productId: Id
  quantity: number
  receivedQty: number | null
  unitCost: string
  supplierBatchNo: string | null
  productionDate: string | null
  expiryDate: string | null
}
export interface PurchaseItem extends PurchaseLineInput {
  id: Id
  lineNo: number
  productCode: string
  productBarcode: string | null
  productName: string
  categoryId: Id
  categoryName: string
  specification: string | null
  unit: string
  managesExpiry: boolean
  lineAmount: string
  batchNo: string | null
  stockQty: number | null
  externalQty: number | null
  externalUnitCost: string | null
  externalLineAmount: string | null
  reconcileNote: string | null
}
export interface PurchaseInput {
  supplierId: Id
  expectedVersion?: number
  expectedArrivalDate: string | null
  arrivedAt: string | null
  remark: string | null
  items: PurchaseLineInput[]
}
export interface ReconcileLine {
  itemId: Id
  externalQty: number | null
  externalUnitCost: string | null
  note: string | null
}
export interface ReconcileSnapshot {
  status: ReconcileStatus
  externalTotal: string | null
  reference: string | null
  difference: string | null
  resolution: string | null
  lines: ReconcileLine[] | null
}
export interface ReconcileInput {
  expectedVersion: number
  expectedReconcileVersion: number
  externalTotal: string
  reference: string | null
  difference: string | null
  resolution: string | null
  lines: ReconcileLine[]
}
export interface PurchaseDetail {
  order: PurchaseSummary
  supplierSnapshot: {
    name: string
    contact: string | null
    phone: string | null
    address: string | null
  }
  items: PurchaseItem[]
  review: {
    reviewerId: Id | null
    reviewerName: string | null
    reviewedAt: string | null
    rejectionReason: string | null
  }
  closure: { actorId: Id | null; occurredAt: string | null; reason: string | null }
  reversal: { actorId: Id | null; occurredAt: string | null; reason: string | null }
  reconciliation: {
    externalTotal: string | null
    reference: string | null
    difference: string | null
    resolution: string | null
    reconcilerId: Id | null
    reconciledAt: string | null
  }
  reconcileHistory: {
    id: Id
    actorName: string
    action: string
    reason: string | null
    before: ReconcileSnapshot | null
    after: ReconcileSnapshot | null
    occurredAt: string
  }[]
}
export interface SupplierOption {
  id: Id
  code: string
  name: string
  enabled: boolean
}
export interface SupplierPurchaseSummary {
  supplierId: Id
  supplierCode: string
  supplierName: string
  netQty: number
  netAmount: string
  unreconciledCount: number
  differenceCount: number
}
export interface SupplierSummaryQuery extends PageQuery {
  supplierId?: Id
  startDate?: string
  endDate?: string
}
export interface PurchaseContext {
  today: string
  now: string
  summaryStartDate: string
  summaryEndDate: string
}
