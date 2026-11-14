<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../../stores/auth'
import { usePagedList } from '../../composables/usePagedList'
import { sales, saleCashiers, saleContext } from '../../api/sales'
import {
  paymentMethods,
  saleStatuses,
  type SaleSummary,
  type SaleQuery,
  type SalePage,
  type CashierOption,
} from '../../types/sale'
import { businessTime } from '../../utils/format'
const auth = useAuth(),
  cashiers = ref<CashierOption[]>([]),
  initialError = ref(''),
  initializing = ref(true)
const route = useRoute()
const fromStatistics = route.query.fromStatistics === '1'
const { query, rows, total, pageResult, loading, error, load } = usePagedList<
  SaleSummary,
  SaleQuery,
  SalePage
>({ keyword: '', cashierId: auth.user?.id, pageNum: 1, pageSize: 10 }, sales)
const totals = computed(() => pageResult.value?.totals)
const appliedRange = ref(''),
  appliedScope = ref('本人')
let defaultDate = ''
async function loadRecords() {
  if (!query.startDate || !query.endDate) {
    initialError.value = '请选择完整日期区间'
    return
  }
  initialError.value = ''
  appliedRange.value = `${query.startDate} 至 ${query.endDate}`
  appliedScope.value =
    query.cashierId === auth.user?.id
      ? '本人'
      : query.cashierId
        ? cashiers.value.find((c) => c.id === query.cashierId)?.name || '指定收银员'
        : '全店'
  await load()
}
async function search() {
  query.pageNum = 1
  await loadRecords()
}
async function reset() {
  Object.assign(query, {
    keyword: '',
    cashierId: auth.user?.id,
    startDate: defaultDate,
    endDate: defaultDate,
    paymentMethod: undefined,
    status: undefined,
    productId: undefined,
    pageNum: 1,
  })
  await loadRecords()
}
async function initialize() {
  initializing.value = true
  initialError.value = ''
  try {
    const context = await saleContext()
    defaultDate = context.today
    query.startDate = defaultDate
    query.endDate = defaultDate
    if (fromStatistics) {
      if (typeof route.query.productId === 'string') query.productId = route.query.productId
      if (typeof route.query.startDate === 'string') query.startDate = route.query.startDate
      if (typeof route.query.endDate === 'string') query.endDate = route.query.endDate
      if (auth.user?.roleCode === 'ADMIN') query.cashierId = undefined
    }
    if (route.query.fromDashboard === 'allToday' || route.query.fromDashboard === 'ownToday') {
      if (typeof route.query.startDate === 'string') query.startDate = route.query.startDate
      if (typeof route.query.endDate === 'string') query.endDate = route.query.endDate
      query.status = 'COMPLETED'
      query.cashierId =
        route.query.fromDashboard === 'allToday' && auth.user?.roleCode === 'ADMIN'
          ? undefined
          : auth.user?.id
    }
    if (auth.user?.roleCode === 'ADMIN') cashiers.value = await saleCashiers()
    await loadRecords()
  } catch (reason) {
    initialError.value = reason instanceof Error ? reason.message : '销售查询设置加载失败'
  } finally {
    initializing.value = false
  }
}
onMounted(initialize)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">SALES RECORDS</div>
      <h1>销售记录</h1>
      <p>默认本人今天记录；管理员可切换收银员或查看全店。历史数量、售价与金额保留原单快照。</p>
      <p v-if="fromStatistics">
        {{
          auth.user?.roleCode === 'CASHIER'
            ? '从全店聚合下钻：下方仅为本人区间销售记录，范围与全店图表不同。'
            : '从全店聚合下钻：下方为对应商品和日期区间的全店销售记录。'
        }}
      </p>
    </div>
    <router-link v-if="auth.can('sale.order.complete')" to="/sales/new"
      ><el-button type="primary">前台开单</el-button></router-link
    >
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        placeholder="销售单号 / 商品编码、条码、名称"
        clearable
        aria-label="销售记录关键词"
      /><el-select
        v-if="auth.user?.roleCode === 'ADMIN'"
        v-model="query.cashierId"
        placeholder="全店收银员"
        clearable
        aria-label="收银员筛选"
        ><el-option v-for="c in cashiers" :key="c.id" :label="c.name" :value="c.id" /></el-select
      ><el-date-picker
        v-model="query.startDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="开始日期"
        aria-label="销售开始日期"
      /><el-date-picker
        v-model="query.endDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="结束日期"
        aria-label="销售结束日期"
      /><el-select
        v-model="query.paymentMethod"
        placeholder="全部付款方式"
        clearable
        aria-label="付款方式筛选"
        ><el-option
          v-for="(label, value) in paymentMethods"
          :key="value"
          :value="value"
          :label="label" /></el-select
      ><el-select
        v-model="query.status"
        placeholder="全部销售状态"
        clearable
        aria-label="销售状态筛选"
        ><el-option
          v-for="(label, value) in saleStatuses"
          :key="value"
          :value="value"
          :label="label" /></el-select
      ><el-button type="primary" native-type="submit" :disabled="initializing">查询销售</el-button
      ><el-button :disabled="initializing" @click="reset">重置</el-button
      ><el-button :loading="loading || initializing" @click="loadRecords">刷新</el-button>
    </form>
    <el-alert
      v-if="initialError || error"
      type="error"
      :title="initialError || error"
      :closable="false"
      ><el-button @click="initialize">重试</el-button></el-alert
    >
    <div v-if="totals" class="sales-totals">
      <div>
        <small>筛选范围内原销售金额</small><strong>¥ {{ totals.originalAmount }}</strong>
      </div>
      <div>
        <small>筛选范围内净销售额</small><strong>¥ {{ totals.netAmount }}</strong>
      </div>
      <div>
        <small>已完成 / 已纠错</small
        ><strong>{{ totals.completedCount }} / {{ totals.correctedCount }} 单</strong>
      </div>
      <p>
        {{ appliedScope }} ·
        {{ appliedRange }}（北京）<br />净额已扣除已纠错原单；不同单位数量仅按基本计数单位合计。
      </p>
    </div>
    <el-table
      v-loading="loading || initializing"
      :data="rows"
      row-key="id"
      empty-text="当前筛选范围暂无销售记录"
      ><el-table-column label="单号 / 时间（北京）" min-width="290" fixed
        ><template #default="{ row }"
          ><router-link :to="`/sales/${row.id}`">{{ row.orderNo }}</router-link>
          <div class="muted">{{ businessTime(row.completedAt) }}</div></template
        ></el-table-column
      ><el-table-column prop="cashierName" label="收银员" width="115" /><el-table-column
        label="种类 / 数量"
        width="125"
        ><template #default="{ row }"
          >{{ row.itemCount }} / {{ row.totalQty }}</template
        ></el-table-column
      ><el-table-column
        prop="totalAmount"
        label="原应收金额（元）"
        width="150"
        align="right"
      /><el-table-column label="支付方式" width="140"
        ><template #default="{ row }">{{
          paymentMethods[row.paymentMethod as keyof typeof paymentMethods]
        }}</template></el-table-column
      ><el-table-column label="状态" width="110"
        ><template #default="{ row }"
          ><el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">{{
            saleStatuses[row.status as keyof typeof saleStatuses]
          }}</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" fixed="right" width="100"
        ><template #default="{ row }"
          ><router-link :to="`/sales/${row.id}`">查看详情</router-link></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 张销售单 · 查看不会再次扣库</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="sizes,prev,pager,next"
        :page-sizes="[10, 20, 50]"
        :disabled="loading"
        @current-change="loadRecords"
        @size-change="search"
      />
    </div>
  </section>
</template>
<style scoped>
.sales-totals {
  display: flex;
  gap: 36px;
  flex-wrap: wrap;
  padding: 20px;
  background: #f3f7f0;
  margin-bottom: 24px;
  border-radius: 8px;
}
.sales-totals small {
  font-size: var(--font-size-body);
  display: block;
  color: #748078;
  margin-bottom: 10px;
}
.sales-totals strong {
  font-size: var(--font-size-title);
  color: #315f49;
}
.sales-totals p {
  margin: 0 0 0 auto;
  color: #748078;
  font-size: var(--font-size-caption);
  line-height: 1.8;
}
</style>
