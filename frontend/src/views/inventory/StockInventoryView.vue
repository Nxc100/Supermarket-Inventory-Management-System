<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuth } from '../../stores/auth'
import { usePagedList } from '../../composables/usePagedList'
import { stockInventory } from '../../api/inventory'
import { categoryOptions } from '../../api/products'
import type { StockProduct, StockQuery } from '../../types/stock'
import type { CategoryOption } from '../../types/catalog'
import { businessTime as purchaseTime } from '../../utils/format'
import { showError } from '../../utils/feedback'
const auth = useAuth(),
  categories = ref<CategoryOption[]>([])
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  StockProduct,
  StockQuery
>({ keyword: '', pageNum: 1, pageSize: 10 }, stockInventory)
onMounted(() => {
  void load()
  void categoryOptions()
    .then((result) => (categories.value = result))
    .catch(showError)
})
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">STOCK OVERVIEW</div>
      <h1>当前库存</h1>
      <p>账面数量、可售数量和过期数量实时查询。盘点冻结独立显示，不改变可售数量口径。</p>
    </div>
    <router-link v-if="auth.can('inventory.movement')" to="/inventory/movements"
      ><el-button>全部库存流水</el-button></router-link
    >
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        placeholder="商品编码 / 条码 / 名称"
        clearable
        aria-label="库存关键词"
      /><el-select v-model="query.categoryId" placeholder="全部分类" clearable aria-label="库存分类"
        ><el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" /></el-select
      ><el-select v-model="query.enabled" placeholder="全部状态" clearable aria-label="商品启用状态"
        ><el-option :value="true" label="启用" /><el-option
          :value="false"
          label="停用" /></el-select
      ><el-select
        v-model="query.lowStock"
        placeholder="低库存筛选"
        clearable
        aria-label="低库存筛选"
        ><el-option :value="true" label="仅低库存" /><el-option
          :value="false"
          label="排除低库存" /></el-select
      ><el-button native-type="submit" type="primary">查询</el-button
      ><el-button @click="reset">重置</el-button
      ><el-button :loading="loading" @click="load">刷新</el-button>
    </form>
    <el-alert v-if="error" type="error" :title="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    ><el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的商品库存"
      ><el-table-column label="商品" min-width="230" fixed
        ><template #default="{ row }"
          ><strong>{{ row.name }}</strong>
          <div class="muted">{{ row.code }} / {{ row.barcode || '无条码' }}</div>
          <small class="muted"
            >{{ row.categoryName }} · {{ row.specification || '无规格' }}</small
          ></template
        ></el-table-column
      ><el-table-column label="账面库存" width="115" align="right"
        ><template #default="{ row }">{{ row.bookQty }} {{ row.unit }}</template></el-table-column
      ><el-table-column label="可售库存" width="115" align="right"
        ><template #default="{ row }"
          >{{ row.saleableQty }} {{ row.unit }}</template
        ></el-table-column
      ><el-table-column label="过期库存" width="110" align="right"
        ><template #default="{ row }"
          ><span :class="{ expired: row.expiredQty > 0 }"
            >{{ row.expiredQty }} {{ row.unit }}</span
          ></template
        ></el-table-column
      ><el-table-column
        prop="stockLowerLimit"
        label="库存下限"
        width="100"
        align="right"
      /><el-table-column label="状态 / 数量说明" min-width="200"
        ><template #default="{ row }"
          ><el-tag v-if="row.frozen" type="warning">盘点冻结，暂不能交易</el-tag>
          <div v-if="!row.enabled" class="muted">商品已停用，可售数为0</div>
          <div v-else-if="!row.canSell" class="muted">商品禁止销售，可售数为0</div>
          <div v-else-if="row.expiredQty > 0" class="muted">过期部分不计入可售</div>
          <div v-if="row.bookQty !== row.batchQty" class="expired">
            账面与批次合计不一致，请核查流水
          </div>
          <div
            v-if="row.enabled && row.canSell && row.saleableQty <= row.stockLowerLimit"
            class="expired"
          >
            低库存
          </div>
          <span
            v-if="
              !row.frozen &&
              row.enabled &&
              row.canSell &&
              !row.expiredQty &&
              row.saleableQty > row.stockLowerLimit
            "
            >正常</span
          ></template
        ></el-table-column
      ><el-table-column label="最近有效入库（北京）" min-width="180"
        ><template #default="{ row }">{{
          purchaseTime(row.lastStockedAt)
        }}</template></el-table-column
      ><el-table-column label="操作" fixed="right" width="180"
        ><template #default="{ row }"
          ><router-link
            v-if="auth.can('inventory.batch')"
            :to="{
              path: `/inventory/products/${row.id}/batches`,
              query: { productName: row.name },
            }"
            >批次详情</router-link
          ><router-link
            v-if="auth.can('inventory.movement')"
            :to="{
              path: '/inventory/movements',
              query: { productId: row.id, productName: row.name },
            }"
            class="movement-link"
            >库存流水</router-link
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 种商品 · 低库存仅针对启用且可销售商品</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="sizes,prev,pager,next"
        :page-sizes="[10, 20, 50]"
        :disabled="loading"
        @current-change="load"
        @size-change="search"
      />
    </div>
  </section>
</template>
<style scoped>
.expired {
  color: #b65232;
}
.movement-link {
  margin-left: 18px;
}
</style>
