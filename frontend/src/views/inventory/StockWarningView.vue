<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { categoryOptions } from '../../api/products'
import type { CategoryOption } from '../../types/catalog'
import { useRoute } from 'vue-router'
import { useAuth } from '../../stores/auth'
import { stockWarnings } from '../../api/warnings'
import { usePagedList } from '../../composables/usePagedList'
import { businessTime } from '../../utils/format'
import type { WarningType, WarningQuery, WarningPage, StockWarning } from '../../types/warning'
const auth = useAuth(),
  route = useRoute()
const types: Record<WarningType, string> = { LOW: '低库存', NEAR: '临期商品', EXPIRED: '过期商品' }
const initialType =
  typeof route.query.type === 'string' && Object.hasOwn(types, route.query.type)
    ? (route.query.type as WarningType)
    : 'LOW'
const { query, rows, total, loading, error, pageResult, load, search, reset } = usePagedList<
  StockWarning,
  WarningQuery,
  WarningPage
>(
  {
    keyword: typeof route.query.keyword === 'string' ? route.query.keyword : '',
    categoryId: typeof route.query.categoryId === 'string' ? route.query.categoryId : undefined,
    productId: typeof route.query.productId === 'string' ? route.query.productId : undefined,
    enabled:
      route.query.enabled === 'true' ? true : route.query.enabled === 'false' ? false : undefined,
    type: initialType,
    pageNum: 1,
    pageSize: 10,
  },
  stockWarnings,
)
const categories = ref<CategoryOption[]>([]),
  metadataError = ref('')
async function metadata() {
  metadataError.value = ''
  try {
    categories.value = await categoryOptions()
  } catch (reason) {
    metadataError.value = reason instanceof Error ? reason.message : '分类选项加载失败'
  }
}
function clearExtraScope() {
  query.productId = undefined
  query.enabled = undefined
  search()
}
onMounted(async () => {
  await Promise.all([load(), metadata()])
})
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">STOCK WARNINGS</div>
      <h1>库存预警</h1>
      <p>
        每次查询按北京业务日重算。处置完成须由实际入库、销售或盘点改变余额，预警不能手动标记消除。
      </p>
    </div>
    <el-button :loading="loading" @click="load">刷新预警</el-button>
  </div>
  <section class="panel">
    <el-tabs v-model="query.type" @tab-change="search"
      ><el-tab-pane v-for="(label, value) in types" :key="value" :name="value" :label="label"
    /></el-tabs>
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        aria-label="预警关键词"
        placeholder="商品编码 / 条码 / 名称 / 批次"
        clearable
      /><el-select v-model="query.categoryId" aria-label="预警分类" placeholder="全部分类" clearable
        ><el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" /></el-select
      ><el-button type="primary" native-type="submit" :loading="loading">查询预警</el-button
      ><el-button @click="reset">重置</el-button>
    </form>
    <el-alert v-if="metadataError" :title="metadataError" type="warning" :closable="false"
      ><el-button @click="metadata">重试分类选项</el-button></el-alert
    >
    <p v-if="query.productId || query.enabled !== undefined" class="muted">
      {{ query.productId ? '已限定所选商品；' : ''
      }}{{ query.enabled === true ? '仅启用商品' : query.enabled === false ? '仅停用商品' : '' }}
      <el-button link @click="clearExtraScope">解除附加范围</el-button>
    </p>
    <p class="muted">
      {{
        query.type === 'LOW'
          ? '启用且可销售商品：可售库存 ≤ 商品下限；盘点冻结不将可售量归零。'
          : query.type === 'NEAR'
            ? '正库存、管理保质期的批次：0 ≤ 剩余天数 ≤ 商品临期阈值，到期当天仍有效。'
            : '正库存且已过到期日的批次，禁止销售。请核实实物，清理后通过盘点登记实际数量并审核。'
      }}
    </p>
    <p v-if="pageResult" class="muted">
      业务日 {{ pageResult.businessDate }} · 数据采集
      {{ businessTime(pageResult.collectedAt) }}（北京） · 当前筛选 {{ pageResult.total }}
      {{ query.type === 'LOW' ? '种商品' : '个批次' }}
    </p>
    <el-alert v-if="error" type="error" :title="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    >
    <el-table
      v-loading="loading"
      :data="rows"
      :row-key="(row: StockWarning) => row.batchItemId ?? row.productId"
      empty-text="当前筛选暂无预警"
      ><el-table-column label="商品" min-width="200" fixed
        ><template #default="{ row }"
          ><strong>{{ row.name }}</strong>
          <div class="muted">{{ row.code }} · {{ row.categoryName }}</div>
          <div v-if="row.frozen" class="muted">盘点冻结（数量口径不变）</div>
          <div v-if="!row.enabled" class="muted">已停用，现存批次仍需处置</div></template
        ></el-table-column
      >
      <el-table-column v-if="query.type !== 'LOW'" label="来源批次 / 日期" min-width="265"
        ><template #default="{ row }"
          >{{ row.batchNo }}
          <div class="muted">
            生产 {{ row.productionDate }} · 到期 {{ row.expiryDate }}
          </div></template
        ></el-table-column
      >
      <el-table-column label="预警数量" width="115" align="right"
        ><template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template></el-table-column
      ><el-table-column
        v-if="query.type === 'LOW'"
        prop="stockLowerLimit"
        label="库存下限"
        width="105"
        align="right"
      /><el-table-column v-else label="剩余天数" width="120"
        ><template #default="{ row }"
          ><el-tag :type="query.type === 'EXPIRED' ? 'danger' : 'warning'"
            >{{ row.remainingDays }} 天{{ query.type === 'EXPIRED' ? ' · 禁售' : '' }}</el-tag
          ></template
        ></el-table-column
      >
      <el-table-column v-if="query.type === 'NEAR'" label="临期阈值" width="100"
        ><template #default="{ row }">{{ row.nearExpiryDays }} 天</template></el-table-column
      >
      <el-table-column label="查看 / 处置" width="335" fixed="right"
        ><template #default="{ row }"
          ><div class="warning-actions">
            <router-link
              v-if="auth.can('ai.inventory')"
              :to="{ path: '/suggestions', query: { type: 'INVENTORY', productId: row.productId } }"
              ><el-button link type="primary">库存优化建议</el-button></router-link
            >
            <router-link
              v-if="auth.can('inventory.batch')"
              :to="`/inventory/products/${row.productId}/batches`"
              ><el-button link type="primary">库存详情</el-button></router-link
            >
            <router-link
              v-if="
                query.type === 'LOW' &&
                auth.can('purchase.order.create') &&
                row.enabled &&
                row.canPurchase &&
                !row.frozen
              "
              :to="{ path: '/purchases/new', query: { productCode: row.code } }"
              ><el-button link type="primary">新建采购</el-button></router-link
            >
            <span v-else-if="query.type === 'LOW' && row.frozen" class="muted"
              >先完成或关闭盘点</span
            ><span v-else-if="query.type === 'LOW' && !row.canPurchase" class="muted"
              >商品禁止采购，联系管理员</span
            >
            <router-link
              v-if="query.type !== 'LOW' && auth.can('inventory.stocktake.start') && !row.frozen"
              :to="{ path: '/stocktakes/new', query: { productCode: row.code } }"
              ><el-button link type="primary">发起盘点</el-button></router-link
            ><span v-if="query.type !== 'LOW' && row.frozen" class="muted">已有活动盘点</span>
            <router-link
              v-if="auth.can('base.product.edit')"
              :to="{ path: '/catalog/products', query: { keyword: row.code } }"
              ><el-button link>阈值设置</el-button></router-link
            >
          </div></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} {{ query.type === 'LOW' ? '种商品' : '个批次' }}</span
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
.warning-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
</style>
