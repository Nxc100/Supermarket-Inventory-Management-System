<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { purchaseContext, supplierSummary } from '../../api/purchases'
import { usePagedList } from '../../composables/usePagedList'
import { useAuth } from '../../stores/auth'
import type { SupplierPurchaseSummary, SupplierSummaryQuery } from '../../types/purchase'
const auth = useAuth(),
  router = useRouter(),
  contextError = ref('')
const { query, rows, total, loading, error, load } = usePagedList<
  SupplierPurchaseSummary,
  SupplierSummaryQuery
>({ keyword: '', pageNum: 1, pageSize: 10 }, supplierSummary)
const applied = ref<{ startDate?: string; endDate?: string }>({})
async function loadSummary() {
  if (!query.startDate || !query.endDate) {
    contextError.value = '请选择完整的日期区间'
    return
  }
  contextError.value = ''
  applied.value = { startDate: query.startDate, endDate: query.endDate }
  await load()
}
async function initialize() {
  try {
    const context = await purchaseContext()
    query.startDate = context.summaryStartDate
    query.endDate = context.summaryEndDate
    await loadSummary()
  } catch (reason) {
    contextError.value = reason instanceof Error ? reason.message : '默认区间加载失败'
  }
}
function drill(row: SupplierPurchaseSummary) {
  void router.push({
    path: '/purchases',
    query: { supplierId: row.supplierId, dateBasis: 'EVENT', ...applied.value },
  })
}
async function searchSummary() {
  query.pageNum = 1
  await loadSummary()
}
async function resetSummary() {
  query.keyword = ''
  query.pageNum = 1
  await initialize()
}
onMounted(initialize)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">SUPPLIER RECONCILIATION</div>
      <h1>供应商核对汇总</h1>
      <p>
        {{ auth.user?.roleCode === 'ADMIN' ? '全店范围' : '本人经办范围' }} ·
        北京时间入库事件计正数、撤销事件计负数。
      </p>
    </div>
    <router-link to="/purchases"><el-button>采购单列表</el-button></router-link>
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="searchSummary">
      <el-input
        v-model="query.keyword"
        placeholder="供应商编码 / 名称"
        aria-label="供应商汇总关键词"
        clearable
      /><el-date-picker
        v-model="query.startDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="开始日期"
        aria-label="汇总开始日期"
      /><el-date-picker
        v-model="query.endDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="结束日期"
        aria-label="汇总结束日期"
      /><el-button type="primary" native-type="submit">查询汇总</el-button
      ><el-button @click="resetSummary">重置</el-button
      ><el-button :loading="loading" @click="loadSummary">刷新</el-button>
    </form>
    <el-alert
      v-if="contextError || error"
      :title="contextError || error"
      type="error"
      :closable="false"
      ><el-button text @click="initialize">重试</el-button></el-alert
    >
    <p class="muted">
      未核对数、差异数：区间内入库且目前仍为已入库的单据。数量为各商品基本单位计数合计。点击明细按相同区间和权限下钻。
    </p>
    <el-table
      v-loading="loading"
      :data="rows"
      row-key="supplierId"
      empty-text="该区间暂无有权查看的采购事件"
      ><el-table-column label="供应商" min-width="220" fixed
        ><template #default="{ row }"
          >{{ row.supplierName }}
          <div class="muted">{{ row.supplierCode }}</div></template
        ></el-table-column
      ><el-table-column
        prop="netQty"
        label="净采购数量"
        min-width="130"
        align="right"
      /><el-table-column
        prop="netAmount"
        label="净采购金额（元）"
        min-width="170"
        align="right"
      /><el-table-column prop="unreconciledCount" label="未核对单数" width="130" /><el-table-column
        prop="differenceCount"
        label="差异单数"
        width="120"
      /><el-table-column label="操作" width="120" fixed="right"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="drill(row)">查看采购明细</el-button></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 家供应商</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        :disabled="loading"
        @current-change="loadSummary"
        @size-change="searchSummary"
      />
    </div>
  </section>
</template>
