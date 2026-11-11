<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { useAuth } from '../../stores/auth'
import { usePagedList } from '../../composables/usePagedList'
import * as api from '../../api/products'
import type { Product, ProductDefaults, ProductQuery, CategoryOption } from '../../types/catalog'
import { showError } from '../../utils/feedback'
import { dateTime, money } from '../../utils/format'
import ProductEditor from './ProductEditor.vue'
import ProductImageDialog from './ProductImageDialog.vue'
import ArchiveImage from '../../components/ArchiveImage.vue'
const auth = useAuth()
const route = useRoute()
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  Product,
  ProductQuery
>(
  {
    keyword: typeof route.query.keyword === 'string' ? route.query.keyword : '',
    enabled: undefined,
    categoryId: undefined,
    managesExpiry: undefined,
    canPurchase: undefined,
    canSell: undefined,
    pageNum: 1,
    pageSize: 10,
  },
  api.products,
)
const options = ref<CategoryOption[]>([])
const defaults = ref<ProductDefaults | null>(null)
const metadataError = ref('')
const editor = ref(false)
const editing = ref<Product | null>(null)
const imageOpen = ref(false)
const imageProduct = ref<Product | null>(null)
const detail = ref<Product | null>(null)
async function metadata() {
  metadataError.value = ''
  try {
    const [categories, settings] = await Promise.all([api.categoryOptions(), api.productDefaults()])
    options.value = categories
    defaults.value = settings
    return true
  } catch (error) {
    metadataError.value = error instanceof Error ? error.message : '商品选项加载失败'
    return false
  }
}
async function open(row?: Product) {
  try {
    if (!(await metadata()) || !defaults.value) return
    editing.value = row ? await api.product(row.id) : null
    editor.value = true
  } catch (error) {
    showError(error)
  }
}
async function view(row: Product) {
  try {
    detail.value = await api.product(row.id)
  } catch (error) {
    showError(error)
  }
}
async function status(row: Product) {
  try {
    await ElMessageBox.confirm(
      `${row.enabled ? '停用' : '启用'}商品「${row.name}」（${row.code}）？停用后不能新采购或新销售，已有库存和历史保留。`,
      '商品状态确认',
      {
        type: 'warning',
        confirmButtonText: row.enabled ? '停用商品' : '启用商品',
        cancelButtonText: '取消',
      },
    )
    await api.setProductStatus(row.id, !row.enabled)
    ElMessage.success('商品状态已更新')
    await load()
  } catch (error) {
    showError(error)
  }
}
async function remove(row: Product) {
  try {
    await ElMessageBox.confirm(
      `删除零库存且无业务引用的商品「${row.name}」（${row.code}）？`,
      '删除商品',
      { type: 'warning', confirmButtonText: '删除商品', cancelButtonText: '取消' },
    )
    await api.deleteProduct(row.id)
    ElMessage.success('商品已删除')
    await load()
  } catch (error) {
    showError(error)
  }
}
function saved() {
  void load()
}
function showImage(product: Product) {
  imageProduct.value = product
  imageOpen.value = true
}
function imageSaved(product: Product) {
  imageProduct.value = product
  if (detail.value?.id === product.id) detail.value = product
  void load()
}
onMounted(() => {
  void load()
  void metadata()
})
</script>
<template>
  <section>
    <div class="page-heading">
      <div>
        <div class="eyebrow">CATALOG / PRODUCTS</div>
        <h1>商品档案</h1>
        <p>维护售价、保质期与交易开关，库存由业务单据流转。</p>
      </div>
      <el-button
        v-if="auth.can('base.product.create')"
        type="primary"
        :icon="Plus"
        :disabled="!defaults"
        @click="open()"
        >新增商品</el-button
      >
    </div>
    <el-alert
      v-if="metadataError"
      :title="metadataError"
      type="error"
      :closable="false"
      show-icon
      class="metadata-error"
      ><el-button text type="primary" @click="metadata">重试选项</el-button></el-alert
    >
    <div class="panel">
      <el-form class="query-bar" @submit.prevent="search"
        ><el-input
          v-model="query.keyword"
          placeholder="编码、条码或名称"
          aria-label="商品关键词"
          :prefix-icon="Search"
          clearable
          maxlength="100"
        /><el-select
          v-model="query.categoryId"
          aria-label="商品分类筛选"
          placeholder="全部分类"
          clearable
          @change="search"
          ><el-option
            v-for="category in options"
            :key="category.id"
            :label="category.name"
            :value="category.id" /></el-select
        ><el-select
          v-model="query.enabled"
          aria-label="商品状态筛选"
          placeholder="全部状态"
          clearable
          @change="search"
          ><el-option label="已启用" :value="true" /><el-option
            label="已停用"
            :value="false" /></el-select
        ><el-select
          v-model="query.managesExpiry"
          aria-label="保质期筛选"
          placeholder="保质期管理"
          clearable
          @change="search"
          ><el-option label="管理保质期" :value="true" /><el-option
            label="不管理保质期"
            :value="false" /></el-select
        ><el-select
          v-model="query.canPurchase"
          aria-label="采购开关筛选"
          placeholder="采购开关"
          clearable
          @change="search"
          ><el-option label="可采购" :value="true" /><el-option
            label="不可采购"
            :value="false" /></el-select
        ><el-select
          v-model="query.canSell"
          aria-label="销售开关筛选"
          placeholder="销售开关"
          clearable
          @change="search"
          ><el-option label="可销售" :value="true" /><el-option
            label="不可销售"
            :value="false" /></el-select
        ><el-button type="primary" native-type="submit">查询</el-button
        ><el-button :icon="Refresh" @click="reset">重置</el-button></el-form
      >
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon
        ><el-button text type="primary" @click="load">重试</el-button></el-alert
      >
      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的商品"
        ><el-table-column label="商品" min-width="250" fixed="left"
          ><template #default="{ row }"
            ><div class="product-cell">
              <ArchiveImage :path="row.imagePath" />
              <div>
                <strong>{{ row.name }}</strong
                ><small>{{ row.code }} · {{ row.barcode ?? '无条码' }}</small>
              </div>
            </div></template
          ></el-table-column
        ><el-table-column prop="categoryName" label="分类" min-width="110" /><el-table-column
          prop="unit"
          label="单位"
          width="70"
        /><el-table-column label="售价（元）" width="120" align="right"
          ><template #default="{ row }">{{ money(row.salePrice) }}</template></el-table-column
        ><el-table-column label="账面库存" width="110" align="right"
          ><template #default="{ row }"
            >{{ row.stockQty }} {{ row.unit }}</template
          ></el-table-column
        ><el-table-column label="保质期" min-width="100"
          ><template #default="{ row }">{{
            row.managesExpiry ? `${row.shelfLifeDays}天` : '不管理'
          }}</template></el-table-column
        ><el-table-column label="状态" min-width="150"
          ><template #default="{ row }"
            ><el-tag :type="row.enabled ? 'success' : 'info'">{{
              row.enabled ? '已启用' : '已停用'
            }}</el-tag
            ><small class="switches"
              >{{ row.canPurchase ? '可采购' : '禁止采购' }} ·
              {{ row.canSell ? '可销售' : '禁止销售' }}</small
            ></template
          ></el-table-column
        ><el-table-column label="操作" width="290" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="view(row)">详情</el-button
            ><el-button v-if="auth.can('base.product.edit')" link type="primary" @click="open(row)"
              >编辑</el-button
            ><el-button
              v-if="auth.can('base.product.image')"
              link
              type="primary"
              @click="showImage(row)"
              >图片</el-button
            ><el-button
              v-if="auth.can('base.product.enable')"
              link
              type="primary"
              @click="status(row)"
              >{{ row.enabled ? '停用' : '启用' }}</el-button
            ><el-tooltip
              v-if="auth.can('base.product.delete')"
              content="商品已有库存或业务引用，请使用停用"
              :disabled="!row.referenced && row.stockQty === 0"
              ><span
                ><el-button
                  link
                  type="danger"
                  :disabled="row.referenced || row.stockQty !== 0"
                  @click="remove(row)"
                  >删除</el-button
                ></span
              ></el-tooltip
            ></template
          ></el-table-column
        ></el-table
      >
      <div class="table-footer">
        <span>共 {{ total }} 件商品档案 · 不在此直接修改库存</span
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
    <ProductEditor
      v-if="defaults"
      v-model="editor"
      :product="editing"
      :defaults="defaults"
      :categories="options"
      @saved="saved"
    />
    <ProductImageDialog v-model="imageOpen" :product="imageProduct" @saved="imageSaved" />
    <el-dialog
      :model-value="detail !== null"
      title="商品详情"
      width="700px"
      @update:model-value="detail = null"
      ><template v-if="detail"
        ><div class="detail-title">
          <ArchiveImage :path="detail.imagePath" :size="64" />
          <div>
            <h2>{{ detail.name }}</h2>
            <span class="muted">{{ detail.code }}</span>
          </div>
        </div>
        <el-descriptions :column="2" border
          ><el-descriptions-item label="条码">{{ detail.barcode ?? '无条码' }}</el-descriptions-item
          ><el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item
          ><el-descriptions-item label="规格">{{
            detail.specification || '未填写'
          }}</el-descriptions-item
          ><el-descriptions-item label="单位">{{ detail.unit }}</el-descriptions-item
          ><el-descriptions-item label="参考进价">{{
            detail.referenceCost === null ? '未填写' : `${money(detail.referenceCost)} 元`
          }}</el-descriptions-item
          ><el-descriptions-item label="售价">{{ money(detail.salePrice) }} 元</el-descriptions-item
          ><el-descriptions-item label="账面库存"
            >{{ detail.stockQty }} {{ detail.unit }}</el-descriptions-item
          ><el-descriptions-item label="库存下限"
            >{{ detail.stockLowerLimit }} {{ detail.unit }}</el-descriptions-item
          ><el-descriptions-item label="保质期">{{
            detail.managesExpiry ? `${detail.shelfLifeDays} 天` : '不管理'
          }}</el-descriptions-item
          ><el-descriptions-item label="临期阈值"
            >{{ detail.nearExpiryDays }} 天</el-descriptions-item
          ><el-descriptions-item label="交易开关"
            >{{ detail.canPurchase ? '可采购' : '禁止采购' }} /
            {{ detail.canSell ? '可销售' : '禁止销售' }}</el-descriptions-item
          ><el-descriptions-item label="状态">{{
            detail.enabled ? '已启用' : '已停用'
          }}</el-descriptions-item
          ><el-descriptions-item label="创建时间" :span="2">{{
            dateTime(detail.createdAt)
          }}</el-descriptions-item
          ><el-descriptions-item label="备注" :span="2">{{
            detail.remark || '未填写'
          }}</el-descriptions-item></el-descriptions
        ></template
      ></el-dialog
    >
  </section>
</template>
<style scoped>
.metadata-error {
  margin-bottom: 16px;
}
.product-cell,
.detail-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-cell strong {
  font-weight: 500;
}
.product-cell small,
.switches {
  display: block;
  color: var(--muted);
  font-size: 11px;
  margin-top: 4px;
}
.detail-title {
  margin-bottom: 24px;
}
.detail-title h2 {
  margin-bottom: 8px;
}
</style>
