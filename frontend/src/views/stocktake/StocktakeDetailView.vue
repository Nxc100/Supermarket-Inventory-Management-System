<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  stocktake,
  saveStocktake,
  stocktakeCommand,
  type StocktakeCommand,
} from '../../api/stocktakes'
import { ApiError } from '../../api/http'
import { useAuth } from '../../stores/auth'
import { useDirtyGuard, confirmUnsaved } from '../../composables/useDirtyGuard'
import { businessTime } from '../../utils/format'
import { showError } from '../../utils/feedback'
import {
  stocktakeStatuses,
  countReasons,
  type StocktakeDetail,
  type StocktakeCountInput,
} from '../../types/stocktake'
const auth = useAuth(),
  route = useRoute(),
  id = route.params.id as string
const detail = ref<StocktakeDetail | null>(null),
  counts = ref<StocktakeCountInput[]>([]),
  baseline = ref('')
const loading = ref(false),
  busy = ref(false),
  error = ref(''),
  conflict = ref(false)
const order = computed(() => detail.value?.order)
const editable = computed(
  () =>
    !!order.value &&
    ['COUNTING', 'REJECTED'].includes(order.value.status) &&
    auth.can('inventory.stocktake.save'),
)
const dirty = computed(() => editable.value && JSON.stringify(counts.value) !== baseline.value)
useDirtyGuard(dirty)
function accept(value: StocktakeDetail) {
  detail.value = value
  counts.value = value.items.map((i) => ({
    id: i.id,
    actualQty: i.actualQty,
    reasonType: i.reasonType,
    reasonNote: i.reasonNote,
  }))
  baseline.value = JSON.stringify(counts.value)
  conflict.value = false
}
async function load() {
  if (busy.value || !(await confirmUnsaved())) return
  loading.value = true
  error.value = ''
  try {
    accept(await stocktake(id))
  } catch (e) {
    detail.value = null
    error.value = e instanceof Error ? e.message : '盘点加载失败'
  } finally {
    loading.value = false
  }
}
const missing = computed(() => counts.value.filter((i) => i.actualQty === null).length)
const problems = computed(() =>
  counts.value.flatMap((i, index) => {
    const qty = i.actualQty,
      book = detail.value?.items[index]?.bookQty
    if (qty !== null && (!Number.isSafeInteger(qty) || qty < 0 || qty > 999999999999))
      return [`第${index + 1}行实盘数须为范围内非负整数`]
    if (qty !== null && qty !== book && (!i.reasonType || (i.reasonNote?.trim().length ?? 0) < 2))
      return [`第${index + 1}行差异须选择原因并填写2～500字说明`]
    return []
  }),
)
async function save() {
  if (busy.value || !order.value || conflict.value) return
  if (problems.value.length) {
    return
  }
  busy.value = true
  try {
    accept(
      await saveStocktake(
        id,
        order.value.rowVersion,
        counts.value.map((i) => ({ ...i, reasonNote: i.reasonNote?.trim() || null })),
      ),
    )
    ElMessage.success('实盘已暂存，商品继续冻结')
  } catch (e) {
    showError(e)
    if (e instanceof ApiError && e.status === 409) conflict.value = true
  } finally {
    busy.value = false
  }
}
const info: Record<StocktakeCommand, { label: string; message: string; reason?: boolean }> = {
  submit: {
    label: '提交盘点审核',
    message: '所有批次实盘数将锁定，商品继续冻结，审核通过后才调整库存。',
  },
  withdraw: { label: '撤回盘点', message: '回到盘点中供继续填写，商品继续冻结。' },
  approve: {
    label: '审核通过并调整库存',
    message: '请核实实盘与原因。按原批次差异调整库存并生成流水，无差异行不写流水；完成后释放冻结。',
  },
  reject: {
    label: '驳回盘点',
    message: '请填写原因，经办人修正后重提。商品仍处于盘点冻结。',
    reason: true,
  },
  close: {
    label: '关闭盘点',
    message: '填写原因后关闭，未生效的差异将丢弃，释放本单冻结，不调整库存。',
    reason: true,
  },
}
async function command(action: StocktakeCommand) {
  if (!order.value || busy.value || dirty.value || conflict.value) return
  if (action === 'submit' && (missing.value || problems.value.length)) {
    ElMessage.warning(problems.value[0] ?? '请填完所有批次实盘数，空白不能提交')
    return
  }
  const captured = order.value,
    message = info[action]
  busy.value = true
  try {
    const summary = `${captured.orderNo} · ${captured.productCount}种 / ${captured.batchCount}批次 · 差异${captured.differenceQty ?? '未填完'}`
    let reason: string | undefined
    if (message.reason) {
      const response = await ElMessageBox.prompt(`${message.message}\n${summary}`, message.label, {
        confirmButtonText: message.label,
        cancelButtonText: '取消',
        inputType: 'textarea',
        type: 'warning',
        inputValidator: (value) =>
          (value.trim().length >= 2 && value.trim().length <= 500) || '请输入2～500字原因',
      })
      reason = response.value.trim()
    } else
      await ElMessageBox.confirm(`${message.message}\n${summary}`, message.label, {
        confirmButtonText: message.label,
        cancelButtonText: '取消',
        type: 'warning',
      })
    accept(await stocktakeCommand(id, action, captured.rowVersion, reason))
    ElMessage.success(`${message.label}已完成`)
  } catch (e) {
    showError(e)
    if (e instanceof ApiError && e.status === 409) {
      try {
        accept(await stocktake(id))
      } catch {
        detail.value = null
        error.value = '状态冲突，刷新详情以核实结果'
      }
    }
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">STOCKTAKE DETAIL</div>
      <h1>盘点详情</h1>
      <p>{{ order?.orderNo ?? '批次快照、实盘及审核' }}</p>
    </div>
    <div class="query-bar">
      <router-link to="/stocktakes"><el-button>盘点列表</el-button></router-link
      ><el-button :loading="loading" :disabled="busy" @click="load">刷新详情</el-button>
    </div>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false"
    ><el-button @click="load">重试</el-button></el-alert
  ><el-skeleton v-if="loading && !detail" :rows="6" animated />
  <section v-if="detail && order" v-loading="busy || loading" class="panel">
    <el-alert
      :title="
        detail.activeFreeze
          ? '商品仍处于盘点冻结：禁止相关采购入库、销售、纠错和撤销；完成或关闭后释放。'
          : '盘点已结束，当前未保持本单冻结。'
      "
      :type="detail.activeFreeze ? 'warning' : 'success'"
      :closable="false"
    />
    <el-descriptions :column="3" border
      ><el-descriptions-item label="状态"
        ><el-tag>{{ stocktakeStatuses[order.status] }}</el-tag></el-descriptions-item
      ><el-descriptions-item label="经办人">{{ order.operatorName }}</el-descriptions-item
      ><el-descriptions-item label="范围"
        >{{ order.productCount }} 种 / {{ order.batchCount }} 批次</el-descriptions-item
      ><el-descriptions-item label="开始时间（北京）">{{
        businessTime(order.startedAt)
      }}</el-descriptions-item
      ><el-descriptions-item label="提交时间（北京）">{{
        businessTime(order.submittedAt)
      }}</el-descriptions-item
      ><el-descriptions-item label="审核人 / 时间"
        >{{ detail.reviewerName ?? '暂无' }} /
        {{ businessTime(detail.reviewedAt) }}</el-descriptions-item
      ><el-descriptions-item label="备注" :span="3">{{
        detail.remark || '暂无'
      }}</el-descriptions-item></el-descriptions
    >
    <el-alert
      v-if="detail.rejectionReason"
      :title="`驳回原因：${detail.rejectionReason}`"
      type="warning"
      :closable="false"
    /><el-alert
      v-if="detail.closeReason"
      :title="`关闭原因：${detail.closeReason} · ${businessTime(detail.closedAt)}`"
      type="info"
      :closable="false"
    />
    <el-alert
      v-if="conflict"
      title="盘点版本已变化，当前输入保留供核对；请刷新详情再继续。"
      type="error"
      :closable="false"
    />
    <p v-if="editable" class="muted">
      实盘未填写保持空白，0表示已清点为零。差异行须填原因和说明；先暂存，再提交审核。未知新批次须关闭后通过采购建立来源。
    </p>
    <el-table
      :data="detail.items"
      row-key="id"
      max-height="540"
      empty-text="盘点无批次，请管理员核查后关闭"
      ><el-table-column label="商品 / 批次" min-width="230" fixed
        ><template #default="{ row }"
          ><strong>{{ row.productName }}</strong>
          <div class="muted">{{ row.productCode }} · {{ row.unit }}</div>
          <div class="muted">{{ row.batchNo }}</div></template
        ></el-table-column
      ><el-table-column label="生产 / 到期日" width="135"
        ><template #default="{ row }"
          >{{ row.productionDate ?? '不管理' }}<br />{{ row.expiryDate ?? '不管理' }}</template
        ></el-table-column
      ><el-table-column prop="bookQty" label="账面快照" width="100" align="right" /><el-table-column
        label="实盘数"
        width="165"
        ><template #default="{ row, $index }"
          ><el-input-number
            v-if="editable && counts[$index]"
            :model-value="counts[$index]!.actualQty"
            :min="0"
            :max="999999999999"
            :precision="0"
            controls-position="right"
            :disabled="busy || conflict"
            :aria-label="`第${$index + 1}批实盘数`"
            style="width: 140px"
            @update:model-value="counts[$index]!.actualQty = $event ?? null"
          /><span v-else>{{ row.actualQty ?? '未填' }}</span></template
        ></el-table-column
      ><el-table-column label="差异" width="95" align="right"
        ><template #default="{ row, $index }">{{
          counts[$index]?.actualQty == null ? '未填' : counts[$index]!.actualQty! - row.bookQty
        }}</template></el-table-column
      ><el-table-column label="差异原因" width="150"
        ><template #default="{ row, $index }"
          ><el-select
            v-if="editable && counts[$index]"
            v-model="counts[$index]!.reasonType"
            clearable
            :disabled="busy || conflict"
            :aria-label="`第${$index + 1}批差异原因`"
            ><el-option
              v-for="(label, value) in countReasons"
              :key="value"
              :label="label"
              :value="value" /></el-select
          ><span v-else>{{
            row.actualQty === null
              ? '未清点'
              : row.reasonType
                ? countReasons[row.reasonType as keyof typeof countReasons]
                : '无差异'
          }}</span></template
        ></el-table-column
      ><el-table-column label="原因说明" min-width="200"
        ><template #default="{ row, $index }"
          ><el-input
            v-if="editable && counts[$index]"
            v-model="counts[$index]!.reasonNote"
            type="textarea"
            :disabled="busy || conflict"
            maxlength="500"
            :aria-label="`第${$index + 1}批原因说明`"
            placeholder="差异须填2～500字"
          /><span v-else>{{ row.reasonNote || '暂无' }}</span></template
        ></el-table-column
      ></el-table
    >
    <p v-if="dirty" class="muted">有未暂存输入，请先暂存；暂存不调整库存。</p>
    <p v-if="problems.length" class="form-error" role="alert">{{ problems[0] }}</p>
    <div class="stocktake-actions">
      <span
        >已填 {{ counts.length - missing }}/{{ counts.length }} 批次 · 已存差异
        {{ order.differenceQty ?? '未填完' }}</span
      >
      <div class="query-bar">
        <el-button v-if="editable" type="primary" :loading="busy" :disabled="conflict" @click="save"
          >暂存实盘</el-button
        >
        <el-button
          v-if="editable && auth.can('inventory.stocktake.submit')"
          :disabled="busy || dirty || conflict || missing > 0 || problems.length > 0"
          @click="command('submit')"
          >提交盘点审核</el-button
        >
        <el-button
          v-if="order.status === 'PENDING' && auth.can('inventory.stocktake.withdraw')"
          :disabled="busy"
          @click="command('withdraw')"
          >撤回盘点</el-button
        >
        <el-button
          v-if="order.status === 'PENDING' && auth.can('inventory.stocktake.review')"
          type="primary"
          :disabled="busy"
          @click="command('approve')"
          >审核通过并调整库存</el-button
        >
        <el-button
          v-if="order.status === 'PENDING' && auth.can('inventory.stocktake.reject')"
          :disabled="busy"
          @click="command('reject')"
          >驳回盘点</el-button
        >
        <el-button
          v-if="detail.activeFreeze && auth.can('inventory.stocktake.close')"
          type="danger"
          plain
          :disabled="
            busy ||
            dirty ||
            conflict ||
            (order.status === 'PENDING' && auth.user?.roleCode !== 'ADMIN')
          "
          @click="command('close')"
          >关闭盘点</el-button
        >
      </div>
    </div>
    <p v-if="order.status === 'PENDING' && auth.user?.roleCode !== 'ADMIN'" class="muted">
      待审核需先撤回才能关闭。已完成盘点只读，不允许重复调整。
    </p>
  </section>
</template>
<style scoped>
.el-descriptions {
  margin: 20px 0;
}
.stocktake-actions {
  position: sticky;
  bottom: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0;
  background: white;
  z-index: 3;
}
.form-error {
  color: var(--el-color-danger);
}
</style>
