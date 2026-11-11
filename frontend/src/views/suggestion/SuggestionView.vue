<script setup lang="ts">
import { computed, nextTick, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../../stores/auth'
import { usePagedList } from '../../composables/usePagedList'
import { ApiError } from '../../api/http'
import * as api from '../../api/suggestions'
import type { CategoryOption } from '../../types/catalog'
import {
  suggestionTypes,
  suggestionStatuses,
  type SuggestionType,
  type SuggestionContext,
  type SuggestionPreview,
  type SuggestionDetail,
  type SuggestionQuery,
  type SuggestionRecord,
  type SuggestionProductOption,
} from '../../types/suggestion'
import { businessTime } from '../../utils/format'
import SuggestionResult from './SuggestionResult.vue'
import SuggestionMarkdown from '../../components/SuggestionMarkdown.vue'
const streamText = ref(''),
  selectedModel = ref('')
const streamPanel = ref<HTMLElement>()
let activeStream: AbortController | null = null
onBeforeUnmount(() => activeStream?.abort())
const auth = useAuth(),
  route = useRoute(),
  context = ref<SuggestionContext | null>(null),
  tab = ref<SuggestionType | 'HISTORY'>(auth.can('ai.inventory') ? 'INVENTORY' : 'SALES')
const kind = computed<SuggestionType>(() => (tab.value === 'SALES' ? 'SALES' : 'INVENTORY'))
const form = reactive({
  startDate: '',
  endDate: '',
  categoryId: undefined as string | undefined,
  productId: undefined as string | undefined,
})
const categories = ref<CategoryOption[]>([]),
  products = ref<SuggestionProductOption[]>([]),
  metadataError = ref(''),
  optionsLoading = ref(false)
const preview = ref<SuggestionPreview | null>(null),
  result = ref<SuggestionDetail | null>(null),
  loading = ref(false),
  generating = ref(false),
  error = ref(''),
  pendingKey = ref(''),
  pendingType = ref<SuggestionType>('INVENTORY')
const storageKey = `inventory-pending-suggestion:${auth.user?.id ?? ''}`
const blocked = computed(() => generating.value || !!pendingKey.value)
const history = usePagedList<SuggestionRecord, SuggestionQuery>(
  { keyword: '', pageNum: 1, pageSize: 10 },
  api.suggestions,
)
let optionSequence = 0
async function loadProducts(keyword = '', selected = false) {
  const sequence = ++optionSequence
  optionsLoading.value = true
  try {
    const rows = await api.suggestionProducts(
      kind.value,
      keyword,
      form.categoryId,
      selected ? form.productId : undefined,
    )
    if (sequence !== optionSequence) return
    const current = products.value.find((p) => p.id === form.productId)
    products.value =
      current && !rows.records.some((p) => p.id === current.id)
        ? [current, ...rows.records]
        : rows.records
  } catch (e) {
    if (sequence === optionSequence)
      metadataError.value = e instanceof Error ? e.message : '商品选项加载失败'
  } finally {
    if (sequence === optionSequence) optionsLoading.value = false
  }
}
async function metadata() {
  metadataError.value = ''
  try {
    categories.value = await api.suggestionCategories()
  } catch (e) {
    metadataError.value = e instanceof Error ? e.message : '分类选项加载失败'
  }
  if (tab.value !== 'HISTORY') await loadProducts('', !!form.productId)
}
async function initialize() {
  loading.value = true
  error.value = ''
  try {
    context.value = await api.suggestionContext()
    selectedModel.value = context.value.model
    form.startDate = context.value.startDate
    form.endDate = context.value.endDate
    if (typeof route.query.startDate === 'string') form.startDate = route.query.startDate
    if (typeof route.query.endDate === 'string') form.endDate = route.query.endDate
    if (typeof route.query.categoryId === 'string') form.categoryId = route.query.categoryId
    if (typeof route.query.productId === 'string') form.productId = route.query.productId
    if (route.query.tab === 'history') tab.value = 'HISTORY'
    else if (route.query.type === 'INVENTORY' || route.query.type === 'SALES') {
      if (context.value.types.includes(route.query.type)) tab.value = route.query.type
      else error.value = '当前岗位无权生成此类型建议'
    }
    try {
      const stored = sessionStorage.getItem(storageKey)
      if (stored) {
        const saved: unknown = /^[a-f0-9-]{36}$/.test(stored)
          ? { key: stored, type: kind.value }
          : JSON.parse(stored)
        if (
          saved &&
          typeof saved === 'object' &&
          'key' in saved &&
          'type' in saved &&
          typeof saved.key === 'string' &&
          /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/.test(saved.key) &&
          (saved.type === 'INVENTORY' || saved.type === 'SALES')
        ) {
          pendingKey.value = saved.key
          pendingType.value = saved.type
          if (context.value.types.includes(saved.type)) tab.value = saved.type
        }
      }
    } catch {
      error.value = '浏览器无法保存本次状态，请保持页面打开并核实生成结果'
    }
    await metadata()
    if (tab.value === 'HISTORY') await history.load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '建议设置加载失败'
  } finally {
    loading.value = false
  }
}
function applyResult(detail: SuggestionDetail) {
  result.value = detail
  if (detail.record.status !== 'PENDING') {
    pendingKey.value = ''
    sessionStorage.removeItem(storageKey)
    if (tab.value === 'HISTORY') void history.load()
  }
}
async function queryResult() {
  if (!pendingKey.value || generating.value) return
  generating.value = true
  error.value = ''
  try {
    applyResult(await api.suggestionByRequest(pendingKey.value))
  } catch (e) {
    error.value = e instanceof Error ? e.message : '暂未查到原结果，请稍后核实或同键重试'
  } finally {
    generating.value = false
  }
}
async function collect() {
  if (blocked.value || loading.value) return
  if (!form.startDate || !form.endDate || form.endDate < form.startDate) {
    error.value = '请选择完整且有效的分析日期区间'
    return
  }
  loading.value = true
  error.value = ''
  result.value = null
  streamText.value = ''
  try {
    preview.value = await api.previewSuggestion({
      requestKey: crypto.randomUUID(),
      type: kind.value,
      ...form,
      model: selectedModel.value,
    })
  } catch (e) {
    preview.value = null
    error.value = e instanceof Error ? e.message : '摘要采集失败'
  } finally {
    loading.value = false
  }
}
async function generate(retry = false) {
  if (generating.value) return
  const key = retry ? pendingKey.value : preview.value?.requestKey
  if (!key) return
  generating.value = true
  error.value = ''
  streamText.value = ''
  result.value = null
  activeStream = new AbortController()
  pendingKey.value = key
  try {
    pendingType.value = retry ? pendingType.value : (preview.value?.type ?? kind.value)
    sessionStorage.setItem(storageKey, JSON.stringify({ key, type: pendingType.value }))
    applyResult(
      await api.streamSuggestion(
        key,
        (text) => {
          const first = !streamText.value
          streamText.value += text
          if (first)
            void nextTick(() =>
              streamPanel.value?.scrollIntoView({ behavior: 'auto', block: 'start' }),
            )
        },
        activeStream.signal,
      ),
    )
  } catch (e) {
    if (e instanceof ApiError && [400, 403, 404, 409].includes(e.status)) {
      pendingKey.value = ''
      sessionStorage.removeItem(storageKey)
      preview.value = null
      error.value = e.message
    } else {
      try {
        applyResult(await api.suggestionByRequest(key))
      } catch {
        error.value = '本次结果尚未核实，请查询原结果或同键重试；不要新建另一请求。'
      }
    }
  } finally {
    generating.value = false
  }
}
async function changeTab() {
  if (loading.value) return
  if (tab.value === 'HISTORY') {
    await history.load()
    return
  }
  preview.value = null
  result.value = null
  error.value = ''
  form.productId = undefined
  await loadProducts()
}
function changeCategory() {
  form.productId = undefined
  void loadProducts()
}
function showHistory() {
  tab.value = 'HISTORY'
  void history.load()
}
watch(selectedModel, () => {
  if (!pendingKey.value) {
    preview.value = null
    result.value = null
    streamText.value = ''
  }
})
watch(
  () => JSON.stringify(form),
  () => {
    if (!pendingKey.value) {
      preview.value = null
      result.value = null
    }
  },
)
onMounted(initialize)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">BUSINESS SUGGESTIONS</div>
      <h1>智能经营建议</h1>
      <p>真实聚合摘要经人工核对后发送通义千问。建议仅供参考，基础进销存独立运行。</p>
    </div>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-alert
    v-if="!context && !loading"
    title="建议设置尚未加载，基础业务可正常使用"
    type="warning"
    :closable="false"
    ><el-button @click="initialize">重试加载设置</el-button></el-alert
  >
  <section class="panel">
    <el-tabs v-model="tab" @tab-change="changeTab"
      ><el-tab-pane
        v-if="auth.can('ai.inventory')"
        label="库存优化建议"
        name="INVENTORY"
        :disabled="blocked || loading" /><el-tab-pane
        v-if="auth.can('ai.sales')"
        label="销售经营分析建议"
        name="SALES"
        :disabled="blocked || loading" /><el-tab-pane label="历史记录" name="HISTORY"
    /></el-tabs>
    <div v-if="pendingKey" class="pending-suggestion">
      <el-alert
        title="本次建议结果待核实，原请求标识已按账号保存；刷新后仍可查询。"
        type="info"
        :closable="false"
      />
      <div class="query-bar">
        <el-button :loading="generating" @click="queryResult">查询本次建议结果</el-button
        ><el-button :disabled="generating" @click="generate(true)">同键重试本次生成</el-button>
      </div>
    </div>
    <template v-if="tab !== 'HISTORY'"
      ><p v-if="context" class="muted">
        提供方 {{ context.provider }} · 逐段实时输出，单次模型等待上限15秒，失败不写库存。
      </p>
      <form class="query-bar" @submit.prevent="collect">
        <el-select
          v-model="selectedModel"
          aria-label="建议使用模型"
          :disabled="blocked || loading || !context"
          style="width: 220px"
          ><el-option
            v-for="model in context?.models ?? []"
            :key="model"
            :value="model"
            :label="model"
        /></el-select>
        <el-date-picker
          v-model="form.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          aria-label="建议开始日期"
          :disabled="blocked || loading"
          placeholder="开始日期"
        /><el-date-picker
          v-model="form.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          aria-label="建议结束日期"
          :disabled="blocked || loading"
          placeholder="结束日期"
        /><el-select
          v-model="form.categoryId"
          aria-label="建议分类"
          placeholder="全部分类"
          clearable
          :disabled="blocked || loading"
          @change="changeCategory"
          ><el-option
            v-for="c in categories"
            :key="c.id"
            :value="c.id"
            :label="c.name" /></el-select
        ><el-select
          v-model="form.productId"
          aria-label="建议商品"
          placeholder="全部商品 / 搜索编码名称"
          clearable
          filterable
          remote
          :remote-method="loadProducts"
          :loading="optionsLoading"
          :disabled="blocked || loading"
          style="width: 260px"
          ><el-option
            v-for="p in products"
            :key="p.id"
            :value="p.id"
            :label="`${p.code} · ${p.name}`" /></el-select
        ><el-button
          native-type="submit"
          type="primary"
          :loading="loading"
          :disabled="blocked || !context"
          >查看数据摘要</el-button
        >
      </form>
      <el-alert v-if="metadataError" :title="metadataError" type="warning" :closable="false"
        ><el-button @click="metadata">重试筛选选项</el-button></el-alert
      >
      <div v-if="preview" class="preview-summary">
        <h2>请核对本次输入摘要</h2>
        <p class="muted">
          {{ suggestionTypes[preview.type] }} · 模型 {{ preview.model }} ·
          {{ preview.periodStart }} 至 {{ preview.periodEnd }} · 采集
          {{ businessTime(preview.snapshotAt) }}（北京） · 有效至
          {{ businessTime(preview.expiresAt) }}。确认后发送这一快照，不随稍后库存变化。
        </p>
        <p class="muted">
          摘要最多列50个库存商品或最近366个销售事件日；若有截取会在coverage中说明，范围总计不截取。
        </p>
        <pre>{{ JSON.stringify(preview.summary, null, 2) }}</pre>
        <el-button
          type="primary"
          :loading="generating"
          :disabled="blocked || loading"
          @click="generate(false)"
          >已核对摘要，生成建议</el-button
        >
      </div>
      <el-empty
        v-else-if="!result && !pendingKey && !loading"
        description="先选择范围并查看真实数据摘要，再确认生成"
      />
      <div class="query-bar suggestion-links">
        <el-button v-if="result?.record.status === 'FAILURE'" :disabled="blocked" @click="collect"
          >重试：重新采集摘要</el-button
        ><el-button v-if="result?.record.status === 'SUCCESS'" :disabled="blocked" @click="collect"
          >重新生成：先更新摘要</el-button
        ><el-button @click="showHistory">查看已有记录</el-button
        ><router-link
          v-if="auth.can(kind === 'SALES' ? 'statistics.sales' : 'statistics.inventory')"
          :to="kind === 'SALES' ? '/statistics/sales' : '/statistics/inventory'"
          ><el-button>返回统计分析</el-button></router-link
        >
      </div>
    </template>
    <template v-else
      ><form class="query-bar" @submit.prevent="history.search">
        <el-select
          v-model="history.query.type"
          aria-label="建议历史类型"
          placeholder="全部允许类型"
          clearable
          ><el-option
            v-for="t in context?.types ?? []"
            :key="t"
            :value="t"
            :label="suggestionTypes[t]" /></el-select
        ><el-select
          v-model="history.query.status"
          aria-label="建议历史状态"
          placeholder="全部状态"
          clearable
          ><el-option
            v-for="(label, value) in suggestionStatuses"
            :key="value"
            :value="value"
            :label="label" /></el-select
        ><el-input
          v-model="history.query.keyword"
          aria-label="建议历史关键词"
          placeholder="模型标识 / 操作者"
          clearable
        /><el-button native-type="submit" type="primary" :loading="history.loading.value"
          >查询历史</el-button
        ><el-button @click="history.reset">重置</el-button
        ><el-button @click="history.load">刷新历史</el-button>
      </form>
      <el-alert
        v-if="history.error.value"
        :title="history.error.value"
        type="error"
        :closable="false"
        ><el-button @click="history.load">重试</el-button></el-alert
      ><el-table
        v-loading="history.loading.value"
        :data="history.rows.value"
        empty-text="暂无有权查看的建议记录"
        ><el-table-column label="类型" width="150"
          ><template #default="{ row }">{{
            suggestionTypes[row.type as SuggestionType]
          }}</template></el-table-column
        ><el-table-column label="状态 / 来源" width="160"
          ><template #default="{ row }"
            ><el-tag>{{
              suggestionStatuses[row.status as keyof typeof suggestionStatuses]
            }}</el-tag>
            <div>{{ row.fixture ? '测试夹具，非真实调用' : row.provider }}</div></template
          ></el-table-column
        ><el-table-column label="分析区间" min-width="190"
          ><template #default="{ row }"
            >{{ row.periodStart }} 至 {{ row.periodEnd }}</template
          ></el-table-column
        ><el-table-column prop="operatorName" label="操作者" width="110" /><el-table-column
          prop="modelName"
          label="模型"
          min-width="150"
        /><el-table-column label="请求时间（北京）" min-width="185"
          ><template #default="{ row }">{{
            businessTime(row.requestedAt)
          }}</template></el-table-column
        ><el-table-column label="操作" width="95" fixed="right"
          ><template #default="{ row }"
            ><router-link :to="`/suggestions/${row.id}`"
              ><el-button link type="primary">查看建议</el-button></router-link
            ></template
          ></el-table-column
        ></el-table
      >
      <div class="table-footer">
        <span>共 {{ history.total.value }} 条 · 采购只看库存成功共享及本人失败/待完成</span
        ><el-pagination
          v-model:current-page="history.query.pageNum"
          v-model:page-size="history.query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="history.total.value"
          layout="sizes,prev,pager,next"
          :disabled="history.loading.value"
          @current-change="history.load"
          @size-change="history.search"
        /></div
    ></template>
  </section>
  <section
    v-if="tab !== 'HISTORY' && (generating || streamText) && !result"
    ref="streamPanel"
    class="panel streaming-suggestion"
    aria-label="实时建议输出"
    aria-live="polite"
  >
    <div class="page-heading">
      <div>
        <h2>{{ generating ? '正在生成建议' : '本次输出尚未核实' }}</h2>
        <p class="muted">
          {{ preview?.model ?? selectedModel }} ·
          {{
            generating
              ? '正文将随模型返回逐段显示'
              : '以下为未完成片段，请查询原结果，不能作为已生成建议'
          }}
        </p>
      </div>
      <el-tag :type="generating ? 'success' : 'warning'">{{
        generating ? '实时输出中' : '未完成'
      }}</el-tag>
    </div>
    <SuggestionMarkdown v-if="streamText" :content="streamText" />
    <p v-else class="muted">正在连接模型，等待第一段正文…</p>
  </section>
  <SuggestionResult v-if="result && tab !== 'HISTORY'" :detail="result" />
</template>
<style scoped>
.streaming-suggestion {
  min-height: 70vh;
  margin-top: 24px;
  scroll-margin-top: 20px;
}
.preview-summary pre {
  max-height: 430px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: #f6f8f5;
  padding: 16px;
  line-height: 1.7;
}
.suggestion-links {
  margin-top: 24px;
  margin-bottom: 0;
}
.pending-suggestion .query-bar {
  margin-top: 16px;
}
</style>
