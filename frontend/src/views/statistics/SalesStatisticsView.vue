<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { salesStatistics, slowProducts } from '../../api/statistics'
import { categoryOptions } from '../../api/products'
import type { CategoryOption } from '../../types/catalog'
import { usePagedList } from '../../composables/usePagedList'
import { useAuth } from '../../stores/auth'
import { businessTime } from '../../utils/format'
import ReportChart from '../../components/ReportChart.vue'
import type {
  SalesStatisticsQuery,
  SalesStatistics,
  SalesProductAggregate,
  SlowProduct,
  SlowProductPage,
} from '../../types/statistics'
const auth = useAuth(),
  router = useRouter(),
  metric = ref<'netQty' | 'netAmount'>('netAmount'),
  categories = ref<CategoryOption[]>([]),
  metadataError = ref(''),
  focusedProduct = ref('')
const { query, rows, total, pageResult, loading, error, load } = usePagedList<
  SalesProductAggregate,
  SalesStatisticsQuery,
  SalesStatistics
>({ keyword: '', pageNum: 1, pageSize: 10, windowDays: 30 }, salesStatistics)
const slow = usePagedList<SlowProduct, SalesStatisticsQuery, SlowProductPage>(
  { keyword: '', pageNum: 1, pageSize: 10, windowDays: 30 },
  (q) => slowProducts({ ...query, pageNum: q.pageNum, pageSize: q.pageSize }),
)
const unit = computed(() => (metric.value === 'netAmount' ? '元' : '件基本单位'))
const monthlyOption = computed<EChartsOption>(() => ({
  color: ['#166c56'],
  tooltip: { trigger: 'axis', renderMode: 'richText' },
  grid: { left: 20, right: 24, top: 45, bottom: 20, containLabel: true },
  xAxis: { type: 'category', data: pageResult.value?.monthly.map((i) => `${i.month}月`) ?? [] },
  yAxis: { type: 'value', name: unit.value },
  series: [
    {
      type: 'bar',
      name: metric.value === 'netAmount' ? '净销售额' : '净销量',
      data: pageResult.value?.monthly.map((i) => i[metric.value]) ?? [],
    },
  ],
}))
const dailyOption = computed<EChartsOption>(() => ({
  color: ['#589985'],
  tooltip: { trigger: 'axis', renderMode: 'richText' },
  grid: { left: 20, right: 24, top: 45, bottom: 20, containLabel: true },
  xAxis: {
    type: 'category',
    data: pageResult.value?.daily.map((i) => i.date) ?? [],
    axisLabel: { rotate: 25 },
  },
  yAxis: { type: 'value', name: unit.value },
  series: [
    {
      type: 'line',
      name: '区间净额 / 净量',
      data: pageResult.value?.daily.map((i) => i[metric.value]) ?? [],
    },
  ],
}))
const hotOption = computed<EChartsOption>(() => ({
  color: ['#166c56'],
  tooltip: { trigger: 'axis', renderMode: 'richText' },
  grid: { left: 20, right: 45, top: 25, bottom: 30, containLabel: true },
  xAxis: { type: 'value', name: '净销量' },
  yAxis: {
    type: 'category',
    inverse: true,
    data: pageResult.value?.hotProducts.map((i) => i.name) ?? [],
    axisLabel: { width: 135, overflow: 'truncate' },
  },
  series: [{ type: 'bar', data: pageResult.value?.hotProducts.map((i) => i.netQty) ?? [] }],
}))
async function refresh() {
  await Promise.all([load(), slow.load()])
}
async function search() {
  query.pageNum = 1
  slow.query.pageNum = 1
  await refresh()
}
async function reset() {
  focusedProduct.value = ''
  Object.assign(query, {
    keyword: '',
    productId: undefined,
    categoryId: undefined,
    year: undefined,
    startDate: undefined,
    endDate: undefined,
    windowDays: 30,
    pageNum: 1,
  })
  await initialize()
}
async function initialize() {
  await load()
  if (pageResult.value) {
    query.startDate = pageResult.value.startDate
    query.endDate = pageResult.value.endDate
    query.year = pageResult.value.year
  }
  await slow.load()
  metadataError.value = ''
  try {
    categories.value = await categoryOptions()
  } catch (reason) {
    metadataError.value = reason instanceof Error ? reason.message : '分类选项加载失败，请重试'
  }
}
async function focusProduct(product: SalesProductAggregate | SlowProduct) {
  query.productId = product.productId
  focusedProduct.value = product.name
  await search()
}
async function month(index: number) {
  if (!pageResult.value || index < 0 || index > 11) return
  const year = pageResult.value.year,
    m = String(index + 1).padStart(2, '0')
  query.startDate = `${year}-${m}-01`
  query.endDate = `${year}-${m}-${new Date(Date.UTC(year, index + 1, 0)).getUTCDate()}`
  await search()
}
function documents(product: SalesProductAggregate) {
  if (!pageResult.value) return
  void router.push({
    path: '/sales',
    query: {
      productId: product.productId,
      startDate: pageResult.value.startDate,
      endDate: pageResult.value.endDate,
      fromStatistics: '1',
    },
  })
}
function clearFocus() {
  query.productId = undefined
  focusedProduct.value = ''
  void search()
}
onMounted(initialize)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">SALES ANALYSIS</div>
      <h1>销售统计</h1>
      <p>全店商品聚合，不含采购成本。完成事件计正、纠错事件计负；原单历史价格保持不变。</p>
    </div>
    <el-button :loading="loading" @click="refresh">刷新统计</el-button>
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        aria-label="销售统计关键词"
        placeholder="商品编码 / 历史商品名称"
        clearable
      /><el-select
        v-model="query.categoryId"
        aria-label="销售统计分类"
        placeholder="全部分类"
        clearable
        ><el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" /></el-select
      ><el-date-picker
        v-model="query.startDate"
        type="date"
        value-format="YYYY-MM-DD"
        aria-label="统计开始日期"
        placeholder="开始日期"
      /><el-date-picker
        v-model="query.endDate"
        type="date"
        value-format="YYYY-MM-DD"
        aria-label="统计结束日期"
        placeholder="结束日期"
      /><el-input-number
        v-model="query.year"
        :min="1"
        :max="9998"
        :precision="0"
        aria-label="月度统计年份"
      /><el-button type="primary" native-type="submit" :loading="loading">查询统计</el-button
      ><el-button @click="reset">重置</el-button>
    </form>
    <p v-if="query.productId" class="muted">
      当前选定商品：{{ focusedProduct }}
      <el-button link @click="clearFocus">解除商品筛选</el-button>
    </p>
    <el-alert v-if="metadataError" :title="metadataError" type="warning" :closable="false"
      ><el-button @click="initialize">重试分类选项</el-button></el-alert
    >
    <el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button @click="refresh">重试</el-button></el-alert
    >
    <template v-if="pageResult"
      ><p class="muted">
        全店区间 {{ pageResult.startDate }} 至 {{ pageResult.endDate }} · 采集
        {{ businessTime(pageResult.collectedAt) }}（北京）
      </p>
      <div class="stat-cards">
        <div>
          <span>区间净销售额</span><strong>¥ {{ pageResult.totals.netAmount }}</strong>
        </div>
        <div>
          <span>区间净销量</span><strong>{{ pageResult.totals.netQty }} 件基本单位</strong>
        </div>
        <div>
          <span>完成 / 纠错事件单数</span
          ><strong
            >{{ pageResult.totals.completedCount }} / {{ pageResult.totals.correctedCount }}</strong
          >
        </div>
      </div></template
    >
    <div class="chart-title">
      <h2>{{ pageResult?.year ?? '指定年份' }}年月度销售</h2>
      <el-radio-group v-model="metric" aria-label="销售统计指标"
        ><el-radio-button value="netAmount">净销售额</el-radio-button
        ><el-radio-button value="netQty">净销量</el-radio-button></el-radio-group
      >
    </div>
    <p class="muted">
      年度图独立使用所选年份，商品筛选相同。缺少业务的月份补0；点击柱子或表格月份查询对应区间。
    </p>
    <ReportChart
      :option="monthlyOption"
      :empty="
        !pageResult || !pageResult.monthly.some((i) => i.netQty !== 0 || i.netAmount !== '0.00')
      "
      label="12个月净销售额或净销量柱状图，配套明细如下"
      @datum="month"
    />
    <el-table :data="pageResult?.monthly ?? []" max-height="260" empty-text="暂无统计"
      ><el-table-column label="月份" width="130"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="month(row.month - 1)"
            >{{ row.month }}月汇总</el-button
          ></template
        ></el-table-column
      ><el-table-column prop="netQty" label="净销量（基本单位）" align="right" /><el-table-column
        prop="netAmount"
        label="净销售额（元）"
        align="right"
    /></el-table>
    <h2 class="section-title">区间销售趋势</h2>
    <p class="muted">仅列出区间内发生销售/纠错事件的日期，详细日期和指标见下表。</p>
    <ReportChart
      :option="dailyOption"
      :empty="!pageResult?.daily.length"
      label="当前区间日度净销售趋势，配套明细如下"
    /><el-table :data="pageResult?.daily ?? []" max-height="260" empty-text="区间暂无销售事件"
      ><el-table-column prop="date" label="业务日期（北京）" /><el-table-column
        prop="netQty"
        label="净销量（基本单位）"
        align="right" /><el-table-column prop="netAmount" label="净销售额（元）" align="right"
    /></el-table>
    <h2 class="section-title">区间热销前10</h2>
    <p class="muted">按净销量降序，平局按净销售额、商品编码排序。</p>
    <ReportChart
      :option="hotOption"
      :empty="!pageResult?.hotProducts.length"
      label="按净销量排列的前10商品，配套明细如下"
    /><el-table :data="pageResult?.hotProducts ?? []" empty-text="区间暂无正净销量商品"
      ><el-table-column prop="code" label="商品编码" /><el-table-column
        prop="name"
        label="商品快照名称" /><el-table-column
        prop="netQty"
        label="净销量"
        align="right" /><el-table-column prop="netAmount" label="净销售额（元）" align="right"
    /></el-table>
    <h2 class="section-title">区间商品汇总明细</h2>
    <p v-if="auth.user?.roleCode === 'CASHIER'" class="muted">
      图表为全店聚合；进入销售记录后仅本人，金额范围可能不同。
    </p>
    <el-table v-loading="loading" :data="rows" empty-text="区间暂无商品销售"
      ><el-table-column prop="code" label="商品编码" /><el-table-column
        prop="name"
        label="最近销售快照名称"
      /><el-table-column prop="categoryName" label="历史分类" /><el-table-column
        prop="netQty"
        label="净销量"
        align="right"
      /><el-table-column prop="netAmount" label="净销售额（元）" align="right" /><el-table-column
        label="下钻"
        width="295"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="focusProduct(row)">聚焦商品</el-button
          ><router-link
            v-if="auth.can('ai.sales') && pageResult"
            :to="{
              path: '/suggestions',
              query: {
                type: 'SALES',
                productId: row.productId,
                startDate: pageResult.startDate,
                endDate: pageResult.endDate,
              },
            }"
            ><el-button link type="primary">经营建议</el-button></router-link
          ><el-button v-if="auth.can('sale.order')" link type="primary" @click="documents(row)">{{
            auth.user?.roleCode === 'CASHIER' ? '本人区间销售记录' : '全店区间销售记录'
          }}</el-button></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>全店 {{ total }} 种商品</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="sizes,prev,pager,next"
        :disabled="loading"
        @current-change="load"
        @size-change="search"
      />
    </div>
  </section>
  <section class="panel slow-section">
    <div class="page-heading">
      <div>
        <h2>滞销与观察不足</h2>
        <p>
          当前有可售库存且窗口零净销量的商品列为滞销；首次入库不足观察窗口的商品另列“观察不足”，可包含已有销量的新品。需结合缺货和到货情况人工判断，不表示整个窗口持续有货。
        </p>
      </div>
      <el-input-number
        v-model="query.windowDays"
        :min="1"
        :max="3650"
        :precision="0"
        aria-label="滞销观察天数"
        @change="slow.search"
      />
    </div>
    <p v-if="slow.pageResult.value" class="muted">
      独立窗口 {{ slow.pageResult.value.windowStart }} 至 {{ slow.pageResult.value.windowEnd }} ·
      {{ slow.pageResult.value.windowDays }} 个自然日（含今天） · 商品筛选与上方一致
    </p>
    <el-alert v-if="slow.error.value" :title="slow.error.value" type="error" :closable="false"
      ><el-button @click="slow.load">重试</el-button></el-alert
    ><el-table
      v-loading="slow.loading.value"
      :data="slow.rows.value"
      empty-text="当前范围没有滞销或观察不足商品"
      ><el-table-column prop="code" label="商品编码" /><el-table-column
        prop="name"
        label="商品名称"
      /><el-table-column label="判断" width="140"
        ><template #default="{ row }"
          ><el-tag :type="row.observation === 'STAGNANT' ? 'warning' : 'info'">{{
            row.observation === 'STAGNANT' ? '窗口零净销量' : '观察不足'
          }}</el-tag></template
        ></el-table-column
      ><el-table-column prop="saleableQty" label="当前可售量" align="right" /><el-table-column
        prop="netQty"
        label="窗口净销量"
        align="right"
      /><el-table-column label="首次有效入库" min-width="180"
        ><template #default="{ row }">{{
          businessTime(row.firstStockedAt)
        }}</template></el-table-column
      ><el-table-column label="下钻" width="110"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="focusProduct(row)">聚焦商品</el-button></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ slow.total.value }} 种</span
      ><el-pagination
        v-model:current-page="slow.query.pageNum"
        v-model:page-size="slow.query.pageSize"
        :total="slow.total.value"
        layout="sizes,prev,pager,next"
        :page-sizes="[10, 20, 50]"
        :disabled="slow.loading.value"
        @current-change="slow.load"
        @size-change="slow.search"
      />
    </div>
  </section>
</template>
<style scoped>
.stat-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin: 20px 0;
}
.stat-cards > div {
  background: #f4f8f5;
  padding: 20px;
  flex: 1;
  min-width: 200px;
  border-radius: 8px;
}
.stat-cards span {
  display: block;
  color: var(--muted);
  margin-bottom: 10px;
}
.stat-cards strong {
  font-size: var(--font-size-title);
}
.chart-title {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}
.section-title {
  margin-top: 32px;
}
.slow-section {
  margin-top: 24px;
}
</style>
