<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Decimal from 'decimal.js'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { Product, ProductInput, ProductDefaults, CategoryOption } from '../../types/catalog'
import * as api from '../../api/products'
import { money } from '../../utils/format'
import { showError } from '../../utils/feedback'
import { useDirtyGuard } from '../../composables/useDirtyGuard'

const props = defineProps<{
  modelValue: boolean
  product: Product | null
  defaults: ProductDefaults
  categories: CategoryOption[]
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; saved: [product: Product] }>()
const formRef = ref<FormInstance>()
const saving = ref(false)
const baseline = ref('')
const form = reactive<ProductInput>({
  code: '',
  barcode: null,
  name: '',
  categoryId: '',
  specification: null,
  unit: '件',
  referenceCost: null,
  salePrice: '',
  managesExpiry: false,
  shelfLifeDays: null,
  nearExpiryDays: 7,
  stockLowerLimit: 0,
  canPurchase: true,
  canSell: true,
  remark: null,
})
const dirty = computed(() => props.modelValue && JSON.stringify(form) !== baseline.value)
useDirtyGuard(dirty)
watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    const p = props.product
    Object.assign(form, {
      code: p?.code ?? '',
      barcode: p?.barcode ?? null,
      name: p?.name ?? '',
      categoryId: p?.categoryId ?? '',
      specification: p?.specification ?? null,
      unit: p?.unit ?? '件',
      referenceCost: p?.referenceCost ?? null,
      salePrice: p?.salePrice ?? '',
      managesExpiry: p?.managesExpiry ?? false,
      shelfLifeDays: p?.shelfLifeDays ?? null,
      nearExpiryDays: p?.nearExpiryDays ?? props.defaults.nearExpiryDays,
      stockLowerLimit: p?.stockLowerLimit ?? props.defaults.stockLowerLimit,
      canPurchase: p?.canPurchase ?? true,
      canSell: p?.canSell ?? true,
      remark: p?.remark ?? null,
    })
    baseline.value = JSON.stringify(form)
    formRef.value?.clearValidate()
  },
)
const rules: FormRules = {
  code: [
    { required: true, message: '请输入商品编码', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_-]{2,32}$/,
      message: '2～32位字母数字、短横线或下划线',
      trigger: 'blur',
    },
  ],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称为2～100个字符', trigger: 'blur' },
  ],
  barcode: [{ pattern: /^$|^[0-9]{4,32}$/, message: '条码为4～32位数字，或留空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择已启用分类', trigger: 'change' }],
  unit: [{ required: true, message: '请选择基本单位', trigger: 'change' }],
  salePrice: [
    { required: true, message: '请输入销售单价', trigger: 'blur' },
    {
      validator: (_r, value: string, cb) =>
        cb(
          !/^\d{1,14}(\.\d{1,2})?$/.test(value) || new Decimal(value).lte(0)
            ? new Error('请输入大于0、最多两位小数的金额')
            : undefined,
        ),
      trigger: 'blur',
    },
  ],
  referenceCost: [
    {
      validator: (_r, value: string | null, cb) =>
        cb(
          value && !/^\d{1,14}(\.\d{1,2})?$/.test(value)
            ? new Error('请输入非负金额，最多两位小数')
            : undefined,
        ),
      trigger: 'blur',
    },
  ],
  shelfLifeDays: [
    {
      validator: (_r, value: number | null, cb) =>
        cb(
          form.managesExpiry && (value === null || !Number.isInteger(value) || value < 1)
            ? new Error('请输入正整数保质期天数')
            : undefined,
        ),
      trigger: 'change',
    },
  ],
  nearExpiryDays: [
    {
      required: true,
      type: 'integer',
      min: 0,
      message: '请输入非负整数临期天数',
      trigger: 'change',
    },
  ],
  stockLowerLimit: [
    {
      required: true,
      type: 'integer',
      min: 0,
      max: 999999999999,
      message: '请输入合法非负整数库存下限',
      trigger: 'change',
    },
  ],
}
function normalize(field: 'salePrice' | 'referenceCost') {
  const value = form[field]
  if (value && /^\d{1,14}(\.\d{1,2})?$/.test(value)) form[field] = money(value)
}
async function close(done?: () => void) {
  if (saving.value) return
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('商品资料尚未保存，确认放弃修改？', '未保存的修改', {
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  emit('update:modelValue', false)
  done?.()
}
async function save() {
  if (saving.value) return
  saving.value = true
  try {
    if (!(await formRef.value?.validate().catch(() => false))) return
    const input: ProductInput = {
      ...form,
      code: form.code.trim(),
      name: form.name.trim(),
      barcode: form.barcode?.trim() || null,
      referenceCost: form.referenceCost ? money(form.referenceCost) : null,
      salePrice: money(form.salePrice),
      shelfLifeDays: form.managesExpiry ? form.shelfLifeDays : null,
    }
    const saved = props.product
      ? await api.updateProduct(props.product.id, input)
      : await api.createProduct(input)
    emit('update:modelValue', false)
    emit('saved', saved)
    ElMessage.success(`商品 ${saved.code} 已保存`)
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    :title="product ? '编辑商品' : '新增商品'"
    class="archive-editor-dialog"
    top="5vh"
    width="820px"
    :before-close="close"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-alert
      v-if="product?.referenced"
      title="该商品已有业务引用，编码、分类、单位与保质期管理方式已锁定。档案编辑不改写历史单据。"
      type="info"
      :closable="false"
      show-icon
      class="editor-notice"
    />
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      :disabled="saving"
      @submit.prevent="save"
    >
      <div class="form-grid">
        <el-form-item label="商品编码" prop="code"
          ><el-input
            v-model="form.code"
            :disabled="product?.referenced"
            maxlength="32"
            placeholder="如 DRINK_001"
        /></el-form-item>
        <el-form-item label="商品条码" prop="barcode"
          ><el-input v-model="form.barcode" maxlength="32" placeholder="可留空，4～32位数字"
        /></el-form-item>
        <el-form-item label="商品名称" prop="name"
          ><el-input v-model="form.name" maxlength="100"
        /></el-form-item>
        <el-form-item label="商品分类" prop="categoryId"
          ><el-select
            v-model="form.categoryId"
            filterable
            :disabled="product?.referenced"
            placeholder="选择已启用分类"
            ><el-option
              v-for="category in categories"
              :key="category.id"
              :value="category.id"
              :label="`${category.name}${category.enabled ? '' : '（已停用）'}`"
              :disabled="!category.enabled" /></el-select
        ></el-form-item>
        <el-form-item label="规格" prop="specification"
          ><el-input v-model="form.specification" maxlength="100" placeholder="如 500mL/瓶"
        /></el-form-item>
        <el-form-item label="基本单位" prop="unit"
          ><el-select v-model="form.unit" :disabled="product?.referenced"
            ><el-option
              v-for="unit in defaults.units"
              :key="unit"
              :label="unit"
              :value="unit" /></el-select
        ></el-form-item>
        <el-form-item label="参考进价（元）" prop="referenceCost"
          ><el-input
            v-model="form.referenceCost"
            placeholder="选填"
            @blur="normalize('referenceCost')"
        /></el-form-item>
        <el-form-item label="销售单价（元）" prop="salePrice"
          ><el-input v-model="form.salePrice" placeholder="0.00" @blur="normalize('salePrice')"
        /></el-form-item>
        <el-form-item label="管理保质期" prop="managesExpiry"
          ><el-switch
            v-model="form.managesExpiry"
            active-text="管理"
            inactive-text="不管理"
            :disabled="product?.referenced || (product?.stockQty ?? 0) > 0"
            @change="form.shelfLifeDays = form.managesExpiry ? 1 : null"
        /></el-form-item>
        <el-form-item
          v-if="form.managesExpiry"
          label="默认保质期（天）"
          prop="shelfLifeDays"
          required
          ><el-input-number v-model="form.shelfLifeDays" :min="1" :max="2147483647" :precision="0"
        /></el-form-item>
        <el-form-item label="临期预警（天）" prop="nearExpiryDays"
          ><el-input-number v-model="form.nearExpiryDays" :min="0" :max="2147483647" :precision="0"
        /></el-form-item>
        <el-form-item label="库存下限" prop="stockLowerLimit"
          ><el-input-number
            v-model="form.stockLowerLimit"
            :min="0"
            :max="999999999999"
            :precision="0"
        /></el-form-item>
        <el-form-item label="可采购"
          ><el-switch v-model="form.canPurchase" active-text="可采购" /></el-form-item
        ><el-form-item label="可销售"
          ><el-switch v-model="form.canSell" active-text="可销售"
        /></el-form-item>
      </div>
      <el-form-item label="备注" prop="remark"
        ><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit
      /></el-form-item>
      <p class="muted">
        {{
          product
            ? `当前账面库存：${product.stockQty} ${product.unit}，本表单只维护商品档案。`
            : '新商品库存为0；现货需通过采购审核入库。商品保存后可在列表上传图片。'
        }}
      </p>
    </el-form>
    <template #footer
      ><el-button :disabled="saving" @click="close()">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">保存商品</el-button></template
    >
  </el-dialog>
</template>
<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}
.form-grid .el-select,
.form-grid .el-input-number {
  width: 100%;
}
.editor-notice {
  margin-bottom: 20px;
}
@media (max-width: 700px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
