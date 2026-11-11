import { ref, shallowRef } from 'vue'
import axios from 'axios'
import { ApiError } from '../api/http'
import { completeSale, saleResult } from '../api/sales'
import type { SaleCompleteInput, SaleDetail, SaleLineInput } from '../types/sale'

function object(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
function readPending(key: string): SaleCompleteInput | null {
  try {
    const raw = sessionStorage.getItem(key)
    if (!raw) return null
    const value: unknown = JSON.parse(raw)
    if (
      !object(value) ||
      typeof value.requestKey !== 'string' ||
      !/^[a-f0-9-]{36}$/.test(value.requestKey) ||
      !['CASH', 'MOCK_EPAY'].includes(String(value.paymentMethod)) ||
      typeof value.receivedAmount !== 'string' ||
      !/^\d{1,14}\.\d{2}$/.test(value.receivedAmount) ||
      !Array.isArray(value.items) ||
      !value.items.length ||
      value.items.length > 100
    )
      return null
    const items: SaleLineInput[] = []
    for (const line of value.items as unknown[]) {
      if (
        !object(line) ||
        typeof line.productId !== 'string' ||
        !/^[1-9]\d{0,18}$/.test(line.productId) ||
        typeof line.quantity !== 'number' ||
        !Number.isInteger(line.quantity) ||
        line.quantity < 1 ||
        line.quantity > 999999 ||
        typeof line.expectedPrice !== 'string' ||
        !/^\d{1,14}\.\d{2}$/.test(line.expectedPrice)
      )
        return null
      items.push({
        productId: line.productId,
        quantity: line.quantity,
        expectedPrice: line.expectedPrice,
      })
    }
    return {
      requestKey: value.requestKey,
      paymentMethod: value.paymentMethod === 'CASH' ? 'CASH' : 'MOCK_EPAY',
      receivedAmount: value.receivedAmount,
      items,
    }
  } catch {
    return null
  }
}

/** A pending confirmation survives reload under its owner and is queried before any manual retry. */
export function useSaleCheckout(ownerId: string, onCompleted: (result: SaleDetail) => void) {
  const storageKey = `inventory-pending-sale:${ownerId}`
  const pending = shallowRef<SaleCompleteInput | null>(readPending(storageKey))
  const receipt = shallowRef<SaleDetail | null>(null)
  const busy = ref(false),
    uncertain = ref(pending.value !== null),
    problem = ref('')
  function clearPending() {
    pending.value = null
    try {
      sessionStorage.removeItem(storageKey)
    } catch {
      /* Completed state remains safe in memory. */
    }
  }
  function completed(result: SaleDetail) {
    receipt.value = result
    clearPending()
    uncertain.value = false
    problem.value = ''
    onCompleted(result)
  }
  async function queryResult() {
    const input = pending.value
    if (!input) return
    try {
      completed(await saleResult(input.requestKey))
    } catch (reason) {
      if (axios.isCancel(reason)) return
      uncertain.value = true
      problem.value =
        reason instanceof ApiError && reason.status === 404
          ? '尚未查到已完成记录。请继续查询，或继续确认本次收款；系统会沿用本次请求。'
          : '暂时无法核实本次收款结果，请恢复连接后查询结果。'
    }
  }
  async function recover() {
    if (busy.value || !pending.value) return
    busy.value = true
    try {
      await queryResult()
    } finally {
      busy.value = false
    }
  }
  async function attempt() {
    const input = pending.value
    if (!input || busy.value) return
    busy.value = true
    problem.value = ''
    try {
      completed(await completeSale(input))
    } catch (reason) {
      if (axios.isCancel(reason)) {
        uncertain.value = true
        return
      }
      if (reason instanceof ApiError && [400, 403, 404, 409].includes(reason.status)) {
        clearPending()
        uncertain.value = false
        throw reason
      }
      uncertain.value = true
      await queryResult()
    } finally {
      busy.value = false
    }
  }
  async function submit(input: SaleCompleteInput) {
    if (busy.value || pending.value) return
    const snapshot = structuredClone(input)
    // Persist before sending: a page reload must not create a fresh sale after an uncertain response.
    sessionStorage.setItem(storageKey, JSON.stringify(snapshot))
    pending.value = snapshot
    await attempt()
  }
  function continueSelling() {
    if (!pending.value && !busy.value) receipt.value = null
  }
  return {
    pending,
    receipt,
    busy,
    uncertain,
    problem,
    submit,
    recover,
    retry: attempt,
    continueSelling,
  }
}
