import { reactive, ref, shallowRef, type UnwrapNestedRefs } from 'vue'
import type { Page, PageQuery } from '../types/api'

/** Lists share request ordering, error/empty states and page repair; business columns stay explicit. */
export function usePagedList<T, Q extends PageQuery, R extends Page<T> = Page<T>>(
  defaults: Q,
  fetcher: (query: Q) => Promise<R>,
) {
  const query = reactive({ ...defaults }) as UnwrapNestedRefs<Q>
  const rows = shallowRef<T[]>([])
  const pageResult = shallowRef<R | null>(null)
  const total = ref(0)
  const loading = ref(false)
  const error = ref('')
  let sequence = 0
  async function load() {
    const current = ++sequence
    loading.value = true
    error.value = ''
    rows.value = []
    pageResult.value = null
    // Keep the known page bounds while loading. A transient zero makes ElPagination clamp a
    // requested later page back to page one before its response can arrive.
    try {
      const result = await fetcher({ ...query } as Q)
      if (current !== sequence) return
      rows.value = result.records
      pageResult.value = result
      total.value = result.total
      const last = Math.max(1, Math.ceil(result.total / query.pageSize))
      if (query.pageNum > last) {
        query.pageNum = last
        await load()
      }
    } catch (reason) {
      if (current === sequence) {
        query.pageNum = 1
        total.value = 0
        error.value = reason instanceof Error ? reason.message : '加载失败，请重试'
      }
    } finally {
      if (current === sequence) loading.value = false
    }
  }
  function search() {
    query.pageNum = 1
    void load()
  }
  function reset() {
    for (const key of Object.keys(query)) Reflect.deleteProperty(query, key)
    Object.assign(query, defaults)
    void load()
  }
  return { query, rows, total, loading, error, pageResult, load, search, reset }
}
