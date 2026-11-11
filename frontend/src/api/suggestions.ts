import { request, ApiError } from './http'
import { postEventStream } from './eventStream'
import type { CategoryOption } from '../types/catalog'
export const suggestionCategories = () =>
  request<CategoryOption[]>('GET', '/suggestions/category-options')
import type { Page } from '../types/api'
import type {
  SuggestionContext,
  SuggestionPreviewRequest,
  SuggestionPreview,
  SuggestionDetail,
  SuggestionQuery,
  SuggestionRecord,
  SuggestionType,
  SuggestionProductOption,
} from '../types/suggestion'
export const suggestionContext = () => request<SuggestionContext>('GET', '/suggestions/context')
export const previewSuggestion = (input: SuggestionPreviewRequest) =>
  request<SuggestionPreview>('POST', '/suggestions/preview', input)
export const generateSuggestion = (key: string) =>
  request<SuggestionDetail>('POST', `/suggestions/by-request/${key}/generate`, {}, undefined, {
    timeout: 20000,
  })
export const suggestionByRequest = (key: string) =>
  request<SuggestionDetail>('GET', `/suggestions/by-request/${key}`)
export async function streamSuggestion(
  key: string,
  delta: (text: string) => void,
  signal?: AbortSignal,
): Promise<SuggestionDetail> {
  let result: SuggestionDetail | null = null
  await postEventStream(
    `/suggestions/by-request/${key}/stream`,
    ({ event, data }) => {
      if (!data || typeof data !== 'object') throw new Error('流式响应格式异常，请查询原结果')
      if (event === 'delta' && 'text' in data && typeof data.text === 'string') delta(data.text)
      if (event === 'error')
        throw new ApiError(
          'message' in data && typeof data.message === 'string' ? data.message : '结果尚未核实',
          'status' in data && typeof data.status === 'number' ? data.status : 0,
          'code' in data && typeof data.code === 'string' ? data.code : 'STREAM_FAILED',
        )
      if (event === 'result') {
        if (
          !('record' in data) ||
          !data.record ||
          typeof data.record !== 'object' ||
          !('status' in data.record) ||
          !['PENDING', 'SUCCESS', 'FAILURE'].includes(String(data.record.status))
        )
          throw new Error('结果格式异常，请查询原结果')
        result = data as SuggestionDetail
      }
    },
    signal,
  )
  if (!result) throw new Error('输出流已中断，请核实原请求结果')
  return result
}
export const suggestion = (id: string) => request<SuggestionDetail>('GET', `/suggestions/${id}`)
export const suggestions = (query: SuggestionQuery) =>
  request<Page<SuggestionRecord>>('GET', '/suggestions', undefined, query)
export const suggestionProducts = (
  type: SuggestionType,
  keyword: string,
  categoryId?: string,
  productId?: string,
) =>
  request<Page<SuggestionProductOption>>('GET', '/suggestions/product-options', undefined, {
    type,
    keyword,
    categoryId,
    productId,
    pageNum: 1,
    pageSize: 20,
  })
