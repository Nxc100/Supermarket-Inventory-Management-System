import { beforeEach, expect, it, vi } from 'vitest'
import { ApiError } from '../api/http'
import type { SaleCompleteInput, SaleDetail } from '../types/sale'
import { useSaleCheckout } from './useSaleCheckout'
import { completeSale, saleResult } from '../api/sales'
vi.mock('../api/sales', () => ({ completeSale: vi.fn(), saleResult: vi.fn() }))
const input: SaleCompleteInput = {
  requestKey: 'a53ccf89-dde9-45d7-a18a-88c17f0d3239',
  paymentMethod: 'CASH',
  receivedAmount: '100.00',
  items: [{ productId: '7', quantity: 4, expectedPrice: '5.00' }],
}
const result: SaleDetail = {
  order: {
    id: '8',
    orderNo: 'XS-8',
    cashierId: '5',
    cashierName: '收银',
    status: 'COMPLETED',
    itemCount: 1,
    totalQty: 4,
    totalAmount: '20.00',
    paymentMethod: 'CASH',
    receivedAmount: '100.00',
    changeAmount: '80.00',
    completedAt: '2026-11-10T10:00:00+08:00',
    correctedAt: null,
    rowVersion: 0,
  },
  items: [],
  correction: { actorId: null, actorName: null, occurredAt: null, reason: null },
  canCorrectToday: false,
  expiredSourceStock: false,
}
beforeEach(() => {
  sessionStorage.clear()
  vi.resetAllMocks()
})
it('a lost confirmation response queries the completed result without repeating the write', async () => {
  vi.mocked(completeSale).mockRejectedValue(new ApiError('连接失败', 0, 'NETWORK_ERROR'))
  vi.mocked(saleResult).mockResolvedValue(result)
  const finished = vi.fn(),
    state = useSaleCheckout('5', finished)
  await state.submit(input)
  expect(completeSale).toHaveBeenCalledTimes(1)
  expect(saleResult).toHaveBeenCalledWith(input.requestKey)
  expect(state.receipt.value?.order.id).toBe('8')
  expect(state.pending.value).toBeNull()
  expect(finished).toHaveBeenCalledOnce()
})
it('unknown results persist by owner and a manual retry keeps the original key and payload', async () => {
  vi.mocked(completeSale)
    .mockRejectedValueOnce(new ApiError('连接失败', 0, 'NETWORK_ERROR'))
    .mockResolvedValueOnce(result)
  vi.mocked(saleResult).mockRejectedValue(new ApiError('无记录', 404, 'NOT_FOUND'))
  const state = useSaleCheckout('5', vi.fn())
  await state.submit(input)
  expect(state.uncertain.value).toBe(true)
  expect(completeSale).toHaveBeenCalledTimes(1)
  expect(useSaleCheckout('6', vi.fn()).pending.value).toBeNull()
  const reloaded = useSaleCheckout('5', vi.fn())
  expect(reloaded.pending.value).toEqual(input)
  await reloaded.retry()
  expect(completeSale).toHaveBeenNthCalledWith(2, input)
  expect(reloaded.receipt.value?.order.id).toBe('8')
  expect(sessionStorage.getItem('inventory-pending-sale:5')).toBeNull()
})
it('a definite business rejection preserves editing and clears the failed confirmation', async () => {
  vi.mocked(completeSale).mockRejectedValue(new ApiError('售价已变化', 409, 'PRICE_CHANGED'))
  const finished = vi.fn(),
    state = useSaleCheckout('5', finished)
  await expect(state.submit(input)).rejects.toMatchObject({ code: 'PRICE_CHANGED' })
  expect(state.pending.value).toBeNull()
  expect(finished).not.toHaveBeenCalled()
  expect(saleResult).not.toHaveBeenCalled()
})
