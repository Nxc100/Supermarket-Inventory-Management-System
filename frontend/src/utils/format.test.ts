import { expect, it } from 'vitest'
import { businessTime } from './format'
it('shows Beijing midnight correctly when ISO omits seconds or the source is UTC', () => {
  expect(businessTime('2026-09-01T00:00+08:00')).toBe('2026-09-01 00:00:00')
  expect(businessTime('2026-08-31T16:00:00Z')).toBe('2026-09-01 00:00:00')
  expect(businessTime(null)).toBe('—')
})
