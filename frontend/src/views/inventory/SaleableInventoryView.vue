<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { categoryOptions } from '../../api/products'
import { saleableInventory, type InventoryQuery } from '../../api/inventory'
import type { SaleProduct, CategoryOption } from '../../types/catalog'
import { usePagedList } from '../../composables/usePagedList'
import { money } from '../../utils/format'
import ArchiveImage from '../../components/ArchiveImage.vue'
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  SaleProduct,
  InventoryQuery
>({ keyword: '', categoryId: undefined, pageNum: 1, pageSize: 10 }, saleableInventory)
const categories = ref<CategoryOption[]>([])
const categoryError = ref('')
async function options() {
  categoryError.value = ''
  try {
    categories.value = await categoryOptions()
  } catch (error) {
    categoryError.value = error instanceof Error ? error.message : '分类加载失败'
  }
}
onMounted(() => {
  void load()
  void options()
})
</script>
<template>
  <section>
    <div class="page-heading">
      <div>
        <div class="eyebrow">INVENTORY / READ ONLY</div>
        <h1>库存查询</h1>
        <p>全店商品与可售数量，只读查询。</p>
      </div>
      <el-tag effect="plain">只读</el-tag>
    </div>
    <el-alert
      title="可售数量不包含已过期、停用或禁止销售的库存；盘点冻结会暂时阻止销售，不会把可售数量归零。"
      type="info"
      :closable="false"
      show-icon
      class="inventory-notice"
    />
    <div class="panel">
      <el-form class="query-bar" @submit.prevent="search"
        ><el-input
          v-model="query.keyword"
          aria-label="库存关键词"
          placeholder="商品编码、条码或名称"
          :prefix-icon="Search"
          clearable
          maxlength="100"
        /><el-select
          v-model="query.categoryId"
          aria-label="库存分类筛选"
          placeholder="全部分类"
          clearable
          @change="search"
          ><el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id" /></el-select
        ><el-button type="primary" native-type="submit">查询</el-button
        ><el-button :icon="Refresh" @click="reset">重置</el-button></el-form
      ><el-alert v-if="categoryError" :title="categoryError" type="error" :closable="false"
        ><el-button text type="primary" @click="options">重试分类</el-button></el-alert
      ><el-alert v-if="error" :title="error" type="error" :closable="false" show-icon
        ><el-button text type="primary" @click="load">重试</el-button></el-alert
      ><el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的库存商品"
        ><el-table-column label="商品" min-width="230" fixed="left"
          ><template #default="{ row }"
            ><div class="inventory-product">
              <ArchiveImage :path="row.imagePath" />
              <div>
                {{ row.name }}<small>{{ row.code }}</small>
              </div>
            </div></template
          ></el-table-column
        ><el-table-column prop="barcode" label="条码" min-width="150"
          ><template #default="{ row }">{{ row.barcode ?? '无条码' }}</template></el-table-column
        ><el-table-column prop="categoryName" label="分类" min-width="110" /><el-table-column
          prop="specification"
          label="规格"
          min-width="130"
          ><template #default="{ row }">{{
            row.specification || '未填写'
          }}</template></el-table-column
        ><el-table-column prop="unit" label="单位" width="70" /><el-table-column
          label="售价（元）"
          width="120"
          align="right"
          ><template #default="{ row }">{{ money(row.salePrice) }}</template></el-table-column
        ><el-table-column label="可售数量" width="120" align="right"
          ><template #default="{ row }"
            >{{ row.saleableQty }} {{ row.unit }}</template
          ></el-table-column
        ><el-table-column label="可售状态" min-width="190"
          ><template #default="{ row }"
            ><el-tag :type="row.frozen ? 'warning' : row.saleableQty > 0 ? 'success' : 'info'">{{
              row.frozen ? '盘点中暂不可销售' : row.saleableQty > 0 ? '可销售' : '暂无可售库存'
            }}</el-tag></template
          ></el-table-column
        ></el-table
      >
      <div class="table-footer">
        <span>共 {{ total }} 件商品 · 结算时仍会重新检查实际库存</span
        ><el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          :disabled="loading"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>
  </section>
</template>
<style scoped>
.inventory-notice {
  margin-bottom: 24px;
}
.inventory-product {
  display: flex;
  align-items: center;
  gap: 12px;
}
.inventory-product small {
  display: block;
  color: var(--muted);
  font-size: 11px;
  margin-top: 4px;
}
</style>
