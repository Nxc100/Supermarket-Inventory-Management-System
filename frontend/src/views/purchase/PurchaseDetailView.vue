<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ApiError } from '../../api/http'
import {
  purchase,
  purchaseCommand,
  deletePurchase,
  type PurchaseCommand,
} from '../../api/purchases'
import { useAuth } from '../../stores/auth'
import { showError } from '../../utils/feedback'
import { purchaseStatuses, reconcileStatuses, purchaseTime } from '../../utils/purchase'
import type { PurchaseDetail } from '../../types/purchase'
import PurchaseReconcileDialog from './PurchaseReconcileDialog.vue'
const route = useRoute(),
  router = useRouter(),
  auth = useAuth()
const id = route.params.id as string
const detail = ref<PurchaseDetail | null>(null),
  loading = ref(false),
  busy = ref(false),
  error = ref(''),
  reconcileOpen = ref(false)
const order = computed(() => detail.value?.order)
const editable = computed(() => order.value && ['DRAFT', 'REJECTED'].includes(order.value.status))
async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await purchase(id)
  } catch (reason) {
    detail.value = null
    error.value = reason instanceof Error ? reason.message : '采购加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)
const commandInfo: Record<PurchaseCommand, { label: string; message: string; reason?: boolean }> = {
  submit: { label: '提交入库审核', message: '本次完整到货将提交审核。审核通过后才增加库存。' },
  withdraw: {
    label: '撤回采购',
    message: '撤回后可编辑，提交历史会保留。若管理员已处理，本次撤回将失败并刷新。',
  },
  approve: {
    label: '审核通过并入库',
    message: '确认已逐行核实实收到货和批次日期？通过后将增加库存并生成入库流水。',
  },
  reject: { label: '驳回采购', message: '请填写驳回原因，经办人可修改后重新提交。', reason: true },
  close: { label: '关闭采购', message: '请填写关闭原因，关闭后不能继续提交。', reason: true },
  reverse: {
    label: '撤销入库',
    message:
      '仅用于整单误录纠错。确认全部原批次均未发生后续变动、实物未被使用，填写原因后扣回原入库数量。',
    reason: true,
  },
  'withdraw-reconciliation': {
    label: '撤回核对',
    message: '填写原因后恢复未核对，保留本次完整核对历史，不改变库存。',
    reason: true,
  },
}
async function command(action: PurchaseCommand) {
  if (!detail.value || busy.value) return
  const captured = detail.value.order,
    info = commandInfo[action]
  busy.value = true
  try {
    const summary = `${captured.orderNo}\n${captured.supplierName} · 数量 ${captured.totalQty} · 金额 ¥${captured.totalAmount}`
    let reason: string | undefined
    if (info.reason) {
      const response = await ElMessageBox.prompt(`${info.message}\n${summary}`, info.label, {
        type: 'warning',
        confirmButtonText: info.label,
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValidator: (value) =>
          (value.trim().length >= 2 && value.trim().length <= 500) || '请输入2～500字原因',
      })
      reason = response.value.trim()
    } else
      await ElMessageBox.confirm(`${info.message}\n${summary}`, info.label, {
        type: 'warning',
        confirmButtonText: info.label,
        cancelButtonText: '取消',
      })
    detail.value = await purchaseCommand(
      id,
      action,
      captured.rowVersion,
      reason,
      action === 'withdraw-reconciliation' ? captured.reconcileVersion : undefined,
    )
    ElMessage.success(`${info.label}已完成`)
  } catch (reason) {
    showError(reason)
    if (reason instanceof ApiError && reason.status === 409) await load()
  } finally {
    busy.value = false
  }
}
async function remove() {
  if (!detail.value || busy.value) return
  const version = detail.value.order.rowVersion
  busy.value = true
  try {
    await ElMessageBox.confirm('确认删除这张从未提交的草稿？', '删除采购草稿', {
      type: 'warning',
      confirmButtonText: '删除草稿',
      cancelButtonText: '取消',
    })
    await deletePurchase(id, version)
    ElMessage.success('采购草稿已删除')
    await router.replace('/purchases')
  } catch (reason) {
    showError(reason)
    if (reason instanceof ApiError && reason.status === 409) await load()
  } finally {
    busy.value = false
  }
}
function reconciled(value: PurchaseDetail) {
  detail.value = value
}
const historyActions: Record<string, string> = {
  RECONCILE_DIFFERENCE: '登记差异',
  RECONCILE_MATCH: '确认一致',
  RECONCILE_WITHDRAW: '撤回核对',
}
function itemName(itemId: string) {
  return detail.value?.items.find((item) => item.id === itemId)?.productName ?? `明细 ${itemId}`
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">PURCHASE RECORD</div>
      <h1>采购单详情</h1>
      <p>{{ order?.orderNo ?? '单据状态、到货批次与核对记录' }}</p>
    </div>
    <div class="query-bar">
      <router-link to="/purchases"><el-button>采购单列表</el-button></router-link
      ><el-button :loading="loading" :disabled="busy || reconcileOpen" @click="load"
        >刷新详情</el-button
      >
    </div>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false"
    ><el-button @click="load">重试</el-button></el-alert
  ><el-skeleton v-if="loading && !detail" :rows="6" animated />
  <template v-if="detail && order"
    ><section v-loading="busy" class="panel purchase-section">
      <div class="query-bar">
        <el-tag size="large">{{ purchaseStatuses[order.status] }}</el-tag
        ><el-tag type="info">{{ reconcileStatuses[order.reconcileStatus] }}</el-tag>
      </div>
      <el-descriptions :column="3" border
        ><el-descriptions-item label="供应商快照">{{ order.supplierName }}</el-descriptions-item
        ><el-descriptions-item label="联系人 / 电话"
          >{{ detail.supplierSnapshot.contact || '—' }} /
          {{ detail.supplierSnapshot.phone || '—' }}</el-descriptions-item
        ><el-descriptions-item label="经办人">{{ order.operatorName }}</el-descriptions-item
        ><el-descriptions-item label="地址快照">{{
          detail.supplierSnapshot.address || '—'
        }}</el-descriptions-item
        ><el-descriptions-item label="创建时间（北京）">{{
          purchaseTime(order.createdAt)
        }}</el-descriptions-item
        ><el-descriptions-item label="预计到货">{{
          order.expectedArrivalDate || '—'
        }}</el-descriptions-item
        ><el-descriptions-item label="到货登记（北京）">{{
          purchaseTime(order.arrivedAt)
        }}</el-descriptions-item
        ><el-descriptions-item label="提交时间（北京）">{{
          purchaseTime(order.submittedAt)
        }}</el-descriptions-item
        ><el-descriptions-item label="入库时间（北京）">{{
          purchaseTime(order.stockedAt)
        }}</el-descriptions-item
        ><el-descriptions-item label="审核人 / 时间"
          >{{ detail.review.reviewerName || '—' }} /
          {{ purchaseTime(detail.review.reviewedAt) }}</el-descriptions-item
        ><el-descriptions-item label="备注" :span="2">{{
          order.remark || '—'
        }}</el-descriptions-item></el-descriptions
      ><el-alert
        v-if="detail.review.rejectionReason"
        :title="`驳回原因：${detail.review.rejectionReason}`"
        type="warning"
        :closable="false"
      /><el-alert
        v-if="detail.closure.reason"
        :title="`关闭原因：${detail.closure.reason}（${purchaseTime(detail.closure.occurredAt)}）`"
        type="info"
        :closable="false"
      /><el-alert
        v-if="detail.reversal.reason"
        :title="`撤销原因：${detail.reversal.reason}（${purchaseTime(detail.reversal.occurredAt)}）`"
        type="warning"
        :closable="false"
      />
      <div class="query-bar document-actions">
        <router-link
          v-if="editable && auth.can('purchase.order.edit')"
          :to="`/purchases/${id}/edit`"
          ><el-button>编辑 / 到货登记</el-button></router-link
        ><el-button
          v-if="editable && auth.can('purchase.order.submit')"
          type="primary"
          :disabled="busy"
          @click="command('submit')"
          >提交入库审核</el-button
        ><el-button
          v-if="order.status === 'PENDING' && auth.can('purchase.order.withdraw')"
          :disabled="busy"
          @click="command('withdraw')"
          >撤回采购</el-button
        ><el-button
          v-if="order.status === 'PENDING' && auth.can('purchase.order.review')"
          type="primary"
          :disabled="busy"
          @click="command('approve')"
          >审核通过并入库</el-button
        ><el-button
          v-if="order.status === 'PENDING' && auth.can('purchase.order.reject')"
          type="danger"
          plain
          :disabled="busy"
          @click="command('reject')"
          >驳回采购</el-button
        ><el-button
          v-if="editable && auth.can('purchase.order.close')"
          :disabled="busy"
          @click="command('close')"
          >关闭采购</el-button
        ><el-button
          v-if="
            order.status === 'DRAFT' &&
            !order.everSubmitted &&
            auth.can('purchase.order.delete_draft')
          "
          type="danger"
          text
          :disabled="busy"
          @click="remove"
          >删除草稿</el-button
        ><el-button
          v-if="
            order.status === 'IN_STOCK' &&
            order.reconcileStatus === 'UNRECONCILED' &&
            auth.can('purchase.order.reverse')
          "
          type="danger"
          plain
          :disabled="busy"
          @click="command('reverse')"
          >撤销入库</el-button
        >
      </div>
      <p v-if="order.status === 'PENDING'" class="muted">
        等待管理员审核，库存尚未增加。审核前会再次检查供应商、商品状态、到货日期和盘点冻结。
      </p>
      <p v-if="order.status === 'IN_STOCK'" class="muted">
        入库已生效。批次当前数量随后续业务变化，原采购数量和单价作为历史快照保留。
      </p>
    </section>
    <section class="panel purchase-section">
      <h2>采购明细与批次</h2>
      <el-table :data="detail.items" row-key="id"
        ><el-table-column prop="lineNo" label="行" width="45" /><el-table-column
          label="商品快照"
          min-width="200"
          fixed
          ><template #default="{ row }"
            >{{ row.productName }}
            <div class="muted">{{ row.productCode }} · {{ row.productBarcode || '无条码' }}</div>
            <small class="muted"
              >{{ row.categoryName }} / {{ row.specification || '无规格' }} / {{ row.unit }}</small
            ></template
          ></el-table-column
        ><el-table-column prop="quantity" label="采购数量" width="90" /><el-table-column
          label="实收"
          width="85"
          ><template #default="{ row }">{{
            row.receivedQty ?? '未登记'
          }}</template></el-table-column
        ><el-table-column
          prop="unitCost"
          label="单价（元）"
          width="110"
          align="right"
        /><el-table-column
          prop="lineAmount"
          label="行金额（元）"
          width="125"
          align="right"
        /><el-table-column label="系统批次 / 供应商批号" min-width="250"
          ><template #default="{ row }"
            >{{ row.batchNo || '尚未入库' }}
            <div class="muted">{{ row.supplierBatchNo || '无供应商批号' }}</div></template
          ></el-table-column
        ><el-table-column label="生产 / 到期日期" min-width="170"
          ><template #default="{ row }">{{
            row.managesExpiry
              ? `${row.productionDate || '未登记'} / ${row.expiryDate || '未登记'}`
              : '不管理保质期'
          }}</template></el-table-column
        ><el-table-column label="批次当前数量" width="115"
          ><template #default="{ row }">{{ row.stockQty ?? '未建立' }}</template></el-table-column
        ></el-table
      >
      <div class="table-footer">
        <span>采购 {{ order.totalQty }} · 实收已登记 {{ order.totalReceivedQty }}</span
        ><strong>原单金额 ¥ {{ order.totalAmount }}</strong>
      </div>
    </section>
    <section v-if="auth.can('purchase.order.reconcile')" class="panel purchase-section">
      <div class="page-heading">
        <div>
          <h2>采购核对</h2>
          <p>{{ reconcileStatuses[order.reconcileStatus] }} · 对账不代表付款</p>
        </div>
        <div class="query-bar">
          <el-button
            v-if="
              order.status === 'IN_STOCK' &&
              ['UNRECONCILED', 'DIFFERENCE'].includes(order.reconcileStatus)
            "
            type="primary"
            :disabled="busy"
            @click="reconcileOpen = true"
            >核对账单</el-button
          ><el-button
            v-if="
              order.status === 'IN_STOCK' &&
              ['DIFFERENCE', 'MATCHED'].includes(order.reconcileStatus)
            "
            :disabled="busy"
            @click="command('withdraw-reconciliation')"
            >撤回核对</el-button
          >
        </div>
      </div>
      <el-descriptions :column="2" border
        ><el-descriptions-item label="外部凭据总金额">{{
          detail.reconciliation.externalTotal ?? '未登记'
        }}</el-descriptions-item
        ><el-descriptions-item label="参考凭据号">{{
          detail.reconciliation.reference || '—'
        }}</el-descriptions-item
        ><el-descriptions-item label="差异说明">{{
          detail.reconciliation.difference || '—'
        }}</el-descriptions-item
        ><el-descriptions-item label="处理说明">{{
          detail.reconciliation.resolution || '—'
        }}</el-descriptions-item></el-descriptions
      ><el-table v-if="detail.reconciliation.externalTotal !== null" :data="detail.items"
        ><el-table-column prop="productName" label="商品" min-width="180" /><el-table-column
          prop="externalQty"
          label="外部数量" /><el-table-column
          prop="externalUnitCost"
          label="外部单价" /><el-table-column
          prop="externalLineAmount"
          label="外部行金额" /><el-table-column prop="reconcileNote" label="行备注" min-width="160"
      /></el-table>
      <h3>核对历史</h3>
      <el-empty
        v-if="!detail.reconcileHistory.length"
        description="暂无核对记录"
        :image-size="60"
      /><el-collapse
        ><el-collapse-item
          v-for="history in detail.reconcileHistory"
          :key="history.id"
          :name="history.id"
          :title="`${purchaseTime(history.occurredAt)} · ${history.actorName} · ${historyActions[history.action] || history.action}`"
          ><p>原因：{{ history.reason || '—' }}</p>
          <div class="history-grid">
            <div v-for="(snapshot, index) in [history.before, history.after]" :key="index">
              <h4>
                {{ index === 0 ? '操作前' : '操作后' }} ·
                {{ snapshot ? reconcileStatuses[snapshot.status] : '无记录' }}
              </h4>
              <template v-if="snapshot"
                ><p>
                  凭据：{{ snapshot.reference || '—' }} · 外部合计：{{
                    snapshot.externalTotal ?? '未登记'
                  }}
                </p>
                <p>
                  差异：{{ snapshot.difference || '—' }} · 处理：{{ snapshot.resolution || '—' }}
                </p>
                <el-table :data="snapshot.lines || []" size="small"
                  ><el-table-column label="商品" min-width="120"
                    ><template #default="{ row }">{{
                      itemName(row.itemId)
                    }}</template></el-table-column
                  ><el-table-column prop="externalQty" label="外部数量" /><el-table-column
                    prop="externalUnitCost"
                    label="外部单价" /><el-table-column prop="note" label="备注" /></el-table
              ></template>
            </div></div></el-collapse-item
      ></el-collapse>
    </section>
    <PurchaseReconcileDialog
      v-model="reconcileOpen"
      :detail="detail"
      @saved="reconciled"
      @stale="load"
  /></template>
</template>
<style scoped>
.purchase-section {
  margin-bottom: 24px;
}
.document-actions {
  margin-top: 24px;
}
.el-alert {
  margin-top: 16px;
}
.history-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 1100px) {
  .history-grid {
    grid-template-columns: 1fr;
  }
}
</style>
