import { describe, expect, it, vi } from 'vitest'
import { usePagedList } from './usePagedList'
import type { Page, PageQuery } from '../types/api'

describe('paged list async boundaries', () => {
  it('keeps pagination bounds until a later-page request finishes, and clears them on failure', async () => {
    let fail: (error: Error) => void = () => {}
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce({ records: ['first'], total: 40, pageNum: 1, pageSize: 10 })
      .mockImplementationOnce(
        () =>
          new Promise<Page<string>>((_resolve, reject) => {
            fail = reject
          }),
      )
    const list = usePagedList({ keyword: '', pageNum: 1, pageSize: 10 }, fetcher)
    await list.load()
    list.query.pageNum = 2
    const request = list.load()
    expect(list.total.value).toBe(40)
    expect(list.query.pageNum).toBe(2)
    expect(list.rows.value).toEqual([])
    fail(new Error('read failed'))
    await request
    expect(list.total.value).toBe(0)
    expect(list.query.pageNum).toBe(1)
    expect(fetcher).toHaveBeenCalledTimes(2)
  })
  it('reset removes optional filters added after initialization and requests the default scope', async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValue({ records: ['all'], total: 1, pageNum: 1, pageSize: 10 })
    const list = usePagedList<string, PageQuery & { categoryId?: string }>(
      { keyword: '', pageNum: 1, pageSize: 10 },
      fetcher,
    )
    list.query.categoryId = '12'
    list.query.enabled = false
    list.query.keyword = 'filtered'
    list.query.pageNum = 4
    await list.load()
    list.reset()
    await vi.waitFor(() => expect(list.loading.value).toBe(false))
    expect(fetcher).toHaveBeenLastCalledWith({ keyword: '', pageNum: 1, pageSize: 10 })
    expect(list.rows.value).toEqual(['all'])
  })
  it('ignores a slower old filter response', async () => {
    let resolveFirst: (value: Page<string>) => void = () => {}
    const fetcher = vi
      .fn()
      .mockImplementationOnce(
        () =>
          new Promise<Page<string>>((resolve) => {
            resolveFirst = resolve
          }),
      )
      .mockResolvedValueOnce({ records: ['new'], total: 1, pageNum: 1, pageSize: 10 })
    const list = usePagedList({ keyword: 'old', pageNum: 1, pageSize: 10 }, fetcher)
    const first = list.load()
    list.query.keyword = 'new'
    await list.load()
    resolveFirst({ records: ['old'], total: 100, pageNum: 1, pageSize: 10 })
    await first
    expect(list.rows.value).toEqual(['new'])
    expect(list.total.value).toBe(1)
    expect(list.pageResult.value?.records).toEqual(['new'])
    expect(list.loading.value).toBe(false)
  })
  it('clears stale rows and exposes an error instead of retaining the prior filter result', async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce({ records: ['old'], total: 1, pageNum: 1, pageSize: 10 })
      .mockRejectedValueOnce(new Error('database unavailable'))
    const list = usePagedList({ keyword: '', pageNum: 1, pageSize: 10 }, fetcher)
    await list.load()
    await list.load()
    expect(list.rows.value).toEqual([])
    expect(list.total.value).toBe(0)
    expect(list.pageResult.value).toBeNull()
    expect(list.error.value).toBe('database unavailable')
  })
  it('repairs the page after deleting the final result on a later page', async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce({ records: [], total: 10, pageNum: 2, pageSize: 10 })
      .mockResolvedValueOnce({ records: ['last page'], total: 10, pageNum: 1, pageSize: 10 })
    const list = usePagedList({ keyword: '', pageNum: 2, pageSize: 10 }, fetcher)
    await list.load()
    expect(list.query.pageNum).toBe(1)
    expect(list.rows.value).toEqual(['last page'])
    expect(fetcher).toHaveBeenCalledTimes(2)
  })
})
