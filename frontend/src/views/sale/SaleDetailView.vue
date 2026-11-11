<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ApiError } from '../../api/http'
import { sale, correctSale } from '../../api/sales'
import { useAuth } from '../../stores/auth'
import { showError } from '../../utils/feedback'
import type { SaleDetail } from '../../types/sale'
import SaleReceiptPanel from './SaleReceiptPanel.vue'
const route = useRoute(),
  auth = useAuth(),
  id = route.params.id as string
const detail = ref<SaleDetail | null>(null),
  loading = ref(false),
  error = ref(''),
  dialog = ref(false),
  busy = ref(false),
  goodsReturned = ref(false),
  reason = ref(''),
  expectedVersion = ref(0)
async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await sale(id)
  } catch (value) {
    detail.value = null
    error.value = value instanceof Error ? value.message : '销售详情加载失败'
  } finally {
    loading.value = false
  }
}
function openCorrection() {
  if (!detail.value || busy.value) return
  goodsReturned.value = false
  reason.value = ''
  expectedVersion.value = detail.value.order.rowVersion
  dialog.value = true
}
function close(done?: () => void) {
  if (busy.value) return
  dialog.value = false
  done?.()
}
async function correct() {
  if (busy.value || !detail.value) return
  if (!goodsReturned.value) {
    ElMessage.warning('请先确认货物实际仍在或已完整收回')
    return
  }
  if (reason.value.trim().length < 2 || reason.value.trim().length > 500) {
    ElMessage.warning('请输入2～500字纠错原因')
    return
  }
  busy.value = true
  try {
    detail.value = await correctSale(id, expectedVersion.value, reason.value.trim(), true)
    dialog.value = false
    ElMessage.success('整单纠错已完成，已按原批次恢复库存')
  } catch (value) {
    showError(value)
    if (value instanceof ApiError && value.status === 409) {
      dialog.value = false
      await load()
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
      <div class="eyebrow">SALE DETAIL</div>
      <h1>销售单详情</h1>
      <p>原始明细与收款快照；已完成销售不提供直接编辑或删除。</p>
    </div>
    <div class="query-bar">
      <router-link to="/sales"><el-button>销售记录</el-button></router-link
      ><el-button :loading="loading" :disabled="dialog || busy" @click="load">刷新详情</el-button>
    </div>
  </div>
  <el-alert v-if="error" type="error" :title="error" :closable="false"
    ><el-button @click="load">重试</el-button></el-alert
  ><el-skeleton v-if="loading && !detail" :rows="6" animated /><SaleReceiptPanel
    v-if="detail"
    :detail="detail"
  />
  <div v-if="detail && auth.can('sale.order.correct')" class="correction-actions">
    <p class="muted">整单纠错仅限当天且货物完整仍在/已收回；跨日或部分实物差异请核实后盘点处理。</p>
    <el-button
      v-if="detail.canCorrectToday"
      type="danger"
      plain
      :disabled="busy"
      @click="openCorrection"
      >整单纠错</el-button
    >
  </div>
  <el-dialog
    v-model="dialog"
    title="当天整单纠错"
    width="620px"
    :close-on-click-modal="false"
    :before-close="close"
    ><template v-if="detail"
      ><el-alert
        title="将按原销售批次恢复整单数量，保留原销售与反向流水。模拟收款只作反向记录，不发生真实退款。"
        type="warning"
        :closable="false" />
      <p class="correction-summary">
        {{ detail.order.orderNo }}<br />{{ detail.order.totalQty }} 件基本单位 · 原金额 ¥
        {{ detail.order.totalAmount }}
      </p>
      <el-checkbox v-model="goodsReturned" :disabled="busy"
        >我确认货物实际仍在店内或已完整收回</el-checkbox
      ><el-form label-position="top"
        ><el-form-item label="纠错原因（2～500字）"
          ><el-input
            v-model="reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            aria-label="整单纠错原因"
            :disabled="busy" /></el-form-item></el-form></template
    ><template #footer
      ><el-button :disabled="busy" @click="close()">取消</el-button
      ><el-button type="danger" :loading="busy" @click="correct">确认整单纠错</el-button></template
    ></el-dialog
  >
</template>
<style scoped>
.correction-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 24px;
}
.correction-actions p {
  margin: 0;
}
.correction-summary {
  line-height: 1.8;
  margin-top: 20px;
}
.el-form {
  margin-top: 20px;
}
</style>
