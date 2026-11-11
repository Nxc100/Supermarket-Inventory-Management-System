<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Decimal from 'decimal.js'
import { ApiError } from '../../api/http'
import { reconcilePurchase } from '../../api/purchases'
import { useDirtyGuard } from '../../composables/useDirtyGuard'
import type { PurchaseDetail, ReconcileLine } from '../../types/purchase'
import { lineAmount, validMoney } from '../../utils/purchase'
import { showError } from '../../utils/feedback'
const props = defineProps<{ modelValue: boolean; detail: PurchaseDetail }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [value: PurchaseDetail]
  stale: []
}>()
const saving = ref(false),
  baseline = ref(''),
  conflict = ref(false)
const form = reactive({
  expectedVersion: 0,
  expectedReconcileVersion: 0,
  externalTotal: '',
  reference: '',
  difference: '',
  resolution: '',
  lines: [] as ReconcileLine[],
})
const dirty = computed(() => props.modelValue && JSON.stringify(form) !== baseline.value)
useDirtyGuard(dirty)
watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    const d = props.detail
    Object.assign(form, {
      expectedVersion: d.order.rowVersion,
      expectedReconcileVersion: d.order.reconcileVersion,
      externalTotal: d.reconciliation.externalTotal ?? '',
      reference: d.reconciliation.reference ?? '',
      difference: d.reconciliation.difference ?? '',
      resolution: '',
      lines: d.items.map((item) => ({
        itemId: item.id,
        externalQty: item.externalQty,
        externalUnitCost: item.externalUnitCost,
        note: item.reconcileNote,
      })),
    })
    baseline.value = JSON.stringify(form)
    conflict.value = false
  },
)
const externalSum = computed(() =>
  form.lines
    .reduce(
      (sum, row) =>
        sum.plus(
          lineAmount(row.externalQty, row.externalUnitCost) === '—'
            ? 0
            : lineAmount(row.externalQty, row.externalUnitCost),
        ),
      new Decimal(0),
    )
    .toFixed(2),
)
function copySystem() {
  form.lines = props.detail.items.map((item) => ({
    itemId: item.id,
    externalQty: item.quantity,
    externalUnitCost: item.unitCost,
    note: null,
  }))
  form.externalTotal = props.detail.order.totalAmount
  ElMessage.info('已按系统值填入，请逐行对照外部凭据确认')
}
async function close(done?: () => void) {
  if (saving.value) return
  try {
    if (dirty.value)
      await ElMessageBox.confirm('本次核对尚未保存，确认放弃输入？', '未保存的核对', {
        type: 'warning',
      })
    emit('update:modelValue', false)
    done?.()
  } catch (reason) {
    showError(reason)
  }
}
async function save(match: boolean) {
  if (saving.value || conflict.value) return
  if (!validMoney(form.externalTotal)) {
    ElMessage.warning('请填写外部凭据总金额，最多两位小数')
    return
  }
  for (const [index, row] of form.lines.entries()) {
    if (
      row.externalQty === null ||
      !Number.isInteger(row.externalQty) ||
      row.externalQty < 0 ||
      row.externalQty > 999999 ||
      !validMoney(row.externalUnitCost)
    ) {
      ElMessage.warning(`第${index + 1}行：请完整填写外部数量和单价`)
      return
    }
  }
  if (!match && form.difference.trim().length < 2) {
    ElMessage.warning('差异说明至少2个字')
    return
  }
  if (
    match &&
    props.detail.order.reconcileStatus === 'DIFFERENCE' &&
    form.resolution.trim().length < 2
  ) {
    ElMessage.warning('解决差异时请填写至少2个字的处理说明')
    return
  }
  saving.value = true
  try {
    const detail = await reconcilePurchase(props.detail.order.id, match, {
      ...form,
      externalTotal: new Decimal(form.externalTotal).toFixed(2),
      reference: form.reference.trim() || null,
      difference: form.difference.trim() || null,
      resolution: form.resolution.trim() || null,
      lines: form.lines.map((row) => ({
        ...row,
        externalUnitCost: new Decimal(row.externalUnitCost!).toFixed(2),
        note: row.note?.trim() || null,
      })),
    })
    baseline.value = JSON.stringify(form)
    emit('update:modelValue', false)
    emit('saved', detail)
    ElMessage.success(match ? '已逐行核对一致，库存未改变' : '差异已登记，原采购数量和金额未改变')
  } catch (reason) {
    if (reason instanceof ApiError && reason.code === 'VERSION_CONFLICT') {
      conflict.value = true
      emit('stale')
    }
    showError(reason)
  } finally {
    saving.value = false
  }
}
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    title="核对采购账单"
    width="1100px"
    top="5vh"
    class="archive-editor-dialog"
    :before-close="close"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
    ><p class="muted">逐行对照外部凭据。核对仅登记结果，原始采购数量、金额和库存保持不变。</p>
    <el-alert
      v-if="conflict"
      title="本次保存未成功。请核对错误提示；关闭后可重新打开，基于最新状态重新填写。"
      type="warning"
      :closable="false"
    /><el-button :disabled="saving || conflict" @click="copySystem"
      >按系统值填入（需人工确认）</el-button
    ><el-table :data="form.lines" row-key="itemId"
      ><el-table-column label="商品 / 系统数量×单价" min-width="210" fixed
        ><template #default="{ $index }"
          >{{ detail.items[$index]?.productName }}
          <div class="muted">
            {{ detail.items[$index]?.quantity }} × {{ detail.items[$index]?.unitCost }} =
            {{ detail.items[$index]?.lineAmount }}
          </div></template
        ></el-table-column
      ><el-table-column label="外部凭据数量" width="160"
        ><template #default="{ row, $index }"
          ><el-input-number
            v-model="row.externalQty"
            :min="0"
            :max="999999"
            :precision="0"
            controls-position="right"
            :aria-label="`第${$index + 1}行外部数量`"
            style="width: 135px" /></template></el-table-column
      ><el-table-column label="外部单价（元）" width="150"
        ><template #default="{ row, $index }"
          ><el-input
            v-model="row.externalUnitCost"
            maxlength="17"
            :aria-label="`第${$index + 1}行外部单价`" /></template></el-table-column
      ><el-table-column label="外部行金额" width="150"
        ><template #default="{ row }">{{
          lineAmount(row.externalQty, row.externalUnitCost)
        }}</template></el-table-column
      ><el-table-column label="行备注" min-width="180"
        ><template #default="{ row, $index }"
          ><el-input
            v-model="row.note"
            maxlength="500"
            :aria-label="`第${$index + 1}行核对备注`" /></template></el-table-column
    ></el-table>
    <p>系统合计 ¥ {{ detail.order.totalAmount }} · 外部明细合计 ¥ {{ externalSum }}</p>
    <el-form label-position="top"
      ><div class="reconcile-grid">
        <el-form-item label="外部凭据总金额（必填）"
          ><el-input
            v-model="form.externalTotal"
            maxlength="17"
            aria-label="外部凭据总金额" /></el-form-item
        ><el-form-item label="参考凭据号"
          ><el-input v-model="form.reference" maxlength="100" aria-label="参考凭据号"
        /></el-form-item>
      </div>
      <el-form-item label="差异说明（存在差异必填）"
        ><el-input
          v-model="form.difference"
          type="textarea"
          maxlength="500"
          show-word-limit
          aria-label="差异说明" /></el-form-item
      ><el-form-item label="差异处理说明（有差异转一致时必填）"
        ><el-input
          v-model="form.resolution"
          type="textarea"
          maxlength="500"
          show-word-limit
          aria-label="差异处理说明" /></el-form-item></el-form
    ><template #footer
      ><el-button :disabled="saving" @click="close()">取消</el-button
      ><el-button :loading="saving" :disabled="conflict" @click="save(false)">登记有差异</el-button
      ><el-button type="primary" :loading="saving" :disabled="conflict" @click="save(true)"
        >逐行确认一致</el-button
      ></template
    ></el-dialog
  >
</template>
<style scoped>
.reconcile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
</style>
