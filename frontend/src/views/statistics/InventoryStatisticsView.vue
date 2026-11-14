<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { inventoryStatistics } from '../../api/statistics'
import { categoryOptions } from '../../api/products'
import { usePagedList } from '../../composables/usePagedList'
import { useAuth } from '../../stores/auth'
import { businessTime } from '../../utils/format'
import ReportChart from '../../components/ReportChart.vue'
import type { InventoryStatistics, InventoryStatisticsQuery } from '../../types/statistics'
import type { CategoryOption } from '../../types/catalog'
import type { StockProduct } from '../../types/stock'
const auth = useAuth(),
  router = useRouter(),
  categories = ref<CategoryOption[]>([]),
  focusedName = ref(''),
  metadataError = ref('')
const { query, rows, total, pageResult, loading, error, load, search, reset } = usePagedList<
  StockProduct,
  InventoryStatisticsQuery,
  InventoryStatistics
>({ keyword: '', pageNum: 1, pageSize: 10 }, inventoryStatistics)
const option = computed<EChartsOption>(() => ({
  color: ['#166c56', '#589985', '#8db7a9', '#b9c99a', '#829fbb', '#cbb88d'],
  tooltip: { trigger: 'item', renderMode: 'richText' },
  legend: { bottom: 0, type: 'scroll' },
  series: [
    {
      type: 'pie',
      radius: ['30%', '65%'],
      center: ['50%', '43%'],
      data:
        pageResult.value?.categories
          .filter((c) => c.bookQty > 0)
          .map((c) => ({ name: c.categoryName, value: c.bookQty })) ?? [],
      label: { formatter: '{b}\n{d}%' },
    },
  ],
}))
function category(index: number) {
  const selected = pageResult.value?.categories.filter((c) => c.bookQty > 0)[index]
  if (!selected) return
  query.categoryId = selected.categoryId
  search()
}
function selectCategory(id: string) {
  query.categoryId = id
  search()
}
function focusProduct(row: StockProduct) {
  focusedName.value = row.name
  query.productId = row.id
  search()
}
function clearProduct() {
  query.productId = undefined
  focusedName.value = ''
  search()
}
async function initialize() {
  await load()
  try {
    metadataError.value = ''
    categories.value = await categoryOptions()
  } catch (reason) {
    metadataError.value =
      reason instanceof Error ? reason.message : '分类选项加载失败，请刷新统计重试'
  }
}
function warning(type: string) {
  const filters = pageResult.value?.filters
  if (!filters) return
  void router.push({
    path: '/inventory/warnings',
    query: {
      type,
      keyword: filters.keyword,
      categoryId: filters.categoryId ?? undefined,
      productId: filters.productId ?? undefined,
      enabled: filters.enabled === null ? undefined : String(filters.enabled),
    },
  })
}
onMounted(initialize)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">INVENTORY ANALYSIS</div>
      <h1>库存统计</h1>
      <p>
        当前库存按基本计数单位合计，不代表体积、重量或价值。账面包含停用和过期实物，冻结不改变可售量。
      </p>
    </div>
    <el-button :loading="loading" @click="initialize">刷新统计</el-button>
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        aria-label="库存统计关键词"
        placeholder="商品编码 / 名称 / 条码"
        clearable
      /><el-select
        v-model="query.categoryId"
        aria-label="库存统计分类"
        placeholder="全部分类"
        clearable
        ><el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" /></el-select
      ><el-select
        v-model="query.enabled"
        aria-label="库存统计启停"
        placeholder="全部启停状态"
        clearable
        ><el-option :value="true" label="启用商品" /><el-option
          :value="false"
          label="停用商品" /></el-select
      ><el-button type="primary" native-type="submit" :loading="loading">查询统计</el-button
      ><el-button @click="reset">重置</el-button>
    </form>
    <p v-if="query.productId" class="muted">
      当前选定商品：{{ focusedName }} <el-button link @click="clearProduct">解除商品筛选</el-button>
    </p>
    <el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    >
    <el-alert v-if="metadataError" :title="metadataError" type="warning" :closable="false"
      ><el-button @click="initialize">重试分类选项</el-button></el-alert
    >
    <p v-if="pageResult" class="muted">
      业务日 {{ pageResult.businessDate }} · 数据采集
      {{ businessTime(pageResult.collectedAt) }}（北京） · 所有指标及明细遵循当前已查询筛选
    </p>
    <div v-if="pageResult" class="inventory-stat-cards">
      <div>
        <span>账面合计 / 可售合计</span
        ><strong>{{ pageResult.totals.bookQty }} / {{ pageResult.totals.saleableQty }}</strong>
      </div>
      <div>
        <span>过期实物量</span><strong>{{ pageResult.totals.expiredQty }}</strong>
      </div>
      <div>
        <span>低库存商品 / 临期批次 / 过期批次</span
        ><strong
          >{{ pageResult.totals.lowProductCount }} / {{ pageResult.totals.nearBatchCount }} /
          {{ pageResult.totals.expiredBatchCount }}</strong
        >
      </div>
    </div>
    <h2>分类账面库存占比</h2>
    <p class="muted">
      分母为当前筛选账面数量，百分比保留两位小数；总量0时不绘制饼图。点击分类或下表“分类明细”缩小商品范围。
    </p>
    <ReportChart
      :option="option"
      :empty="!pageResult || pageResult.totals.bookQty === 0"
      label="按基本单位账面数量计算的分类库存占比，精确百分比见下表"
      @datum="category"
    />
    <el-table :data="pageResult?.categories ?? []" empty-text="暂无库存分类数据"
      ><el-table-column prop="categoryName" label="分类" /><el-table-column
        prop="bookQty"
        label="账面数量（基本单位）"
        align="right"
      /><el-table-column label="占比" align="right"
        ><template #default="{ row }">{{ row.percentage }}%</template></el-table-column
      ><el-table-column label="下钻" width="130"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="selectCategory(row.categoryId)"
            >分类明细</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <div v-if="auth.can('inventory.warning')" class="query-bar warning-links">
      <span>查看同筛选范围的预警：</span><el-button @click="warning('LOW')">低库存预警</el-button
      ><el-button @click="warning('NEAR')">临期预警</el-button
      ><el-button @click="warning('EXPIRED')">过期预警</el-button>
    </div>
    <h2 class="inventory-detail-title">同筛选商品明细</h2>
    <el-table v-loading="loading" :data="rows" empty-text="当前筛选暂无商品"
      ><el-table-column prop="code" label="商品编码" min-width="155" /><el-table-column
        prop="name"
        label="商品名称"
        min-width="180"
      /><el-table-column prop="unit" label="单位" width="65" /><el-table-column
        prop="bookQty"
        label="账面"
        align="right"
      /><el-table-column prop="saleableQty" label="可售" align="right" /><el-table-column
        prop="expiredQty"
        label="过期"
        align="right"
      /><el-table-column label="状态" width="165"
        ><template #default="{ row }"
          >{{ row.enabled ? '启用' : '停用' }}{{ row.frozen ? ' · 盘点冻结' : ''
          }}{{ !row.canSell ? ' · 禁售' : '' }}</template
        ></el-table-column
      ><el-table-column label="详情" fixed="right" width="260"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="focusProduct(row)">聚焦商品</el-button
          ><router-link
            v-if="auth.can('ai.inventory')"
            :to="{ path: '/suggestions', query: { type: 'INVENTORY', productId: row.id } }"
            ><el-button link type="primary">库存建议</el-button></router-link
          ><router-link
            v-if="auth.can('inventory.batch')"
            :to="`/inventory/products/${row.id}/batches`"
            ><el-button link type="primary">批次详情</el-button></router-link
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 种商品</span
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
</template>
<style scoped>
.inventory-stat-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin: 20px 0;
}
.inventory-stat-cards > div {
  background: #f4f8f5;
  padding: 20px;
  flex: 1;
  min-width: 190px;
  border-radius: 8px;
}
.inventory-stat-cards span {
  display: block;
  color: var(--muted);
  margin-bottom: 10px;
}
.inventory-stat-cards strong {
  font-size: var(--font-size-title);
}
.inventory-detail-title,
.warning-links {
  margin-top: 28px;
}
</style>
