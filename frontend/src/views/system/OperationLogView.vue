<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { usePagedList } from '../../composables/usePagedList'
import {
  operationLogs,
  operationLog,
  operationActions,
  actionLabels,
  type OperationLog,
  type OperationLogQuery,
  type OperationLogDetail,
} from '../../api/system'
import { businessTime } from '../../utils/format'
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  OperationLog,
  OperationLogQuery
>({ keyword: '', pageNum: 1, pageSize: 10 }, operationLogs)
const actions = ref<string[]>([]),
  metadataError = ref(''),
  detail = ref<OperationLogDetail | null>(null),
  detailError = ref(''),
  detailLoading = ref(false),
  dialog = ref(false),
  selectedId = ref('')
let detailSequence = 0
async function metadata() {
  metadataError.value = ''
  try {
    actions.value = await operationActions()
  } catch (e) {
    metadataError.value = e instanceof Error ? e.message : '动作选项加载失败'
  }
}
async function open(id: string) {
  const sequence = ++detailSequence
  selectedId.value = id
  dialog.value = true
  detail.value = null
  detailError.value = ''
  detailLoading.value = true
  try {
    const result = await operationLog(id)
    if (sequence === detailSequence) detail.value = result
  } catch (e) {
    if (sequence === detailSequence)
      detailError.value = e instanceof Error ? e.message : '记录详情加载失败'
  } finally {
    if (sequence === detailSequence) detailLoading.value = false
  }
}
function close() {
  detailSequence++
  detail.value = null
  detailLoading.value = false
  dialog.value = false
}
onMounted(() => {
  void load()
  void metadata()
})
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">OPERATION HISTORY</div>
      <h1>操作记录</h1>
      <p>按发生时间倒序显示关键操作；原业务记录和已完成结果保留，不在此修改。</p>
    </div>
    <el-button :loading="loading" @click="load">刷新操作记录</el-button>
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        aria-label="操作对象关键词"
        placeholder="对象单号 / 编码 / 原因"
        clearable
      /><el-input
        v-model="query.actor"
        aria-label="操作人姓名"
        placeholder="操作者姓名"
        clearable
      />
      <el-select
        v-model="query.action"
        aria-label="操作动作"
        placeholder="全部动作"
        clearable
        filterable
        ><el-option
          v-for="action in actions"
          :key="action"
          :value="action"
          :label="`${actionLabels[action] ?? action} · ${action}`"
      /></el-select>
      <el-select v-model="query.result" aria-label="操作结果" placeholder="全部结果" clearable
        ><el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILURE"
      /></el-select>
      <el-date-picker
        v-model="query.startDate"
        type="date"
        value-format="YYYY-MM-DD"
        aria-label="操作开始日期"
        placeholder="开始日期"
      /><el-date-picker
        v-model="query.endDate"
        type="date"
        value-format="YYYY-MM-DD"
        aria-label="操作结束日期"
        placeholder="结束日期"
      />
      <el-button native-type="submit" type="primary" :loading="loading">查询操作记录</el-button
      ><el-button @click="reset">重置</el-button>
    </form>
    <el-alert v-if="metadataError" :title="metadataError" type="warning" :closable="false"
      ><el-button @click="metadata">重试动作选项</el-button></el-alert
    ><el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    >
    <el-table v-loading="loading" :data="rows" empty-text="暂无符合条件的操作记录" row-key="id"
      ><el-table-column label="时间（北京）" min-width="180"
        ><template #default="{ row }">{{ businessTime(row.occurredAt) }}</template></el-table-column
      ><el-table-column prop="actorName" label="操作者" width="110" /><el-table-column
        label="动作"
        min-width="180"
        ><template #default="{ row }"
          >{{ actionLabels[row.action] ?? row.action }}
          <div class="muted">{{ row.action }}</div></template
        ></el-table-column
      ><el-table-column label="业务对象" min-width="230"
        ><template #default="{ row }"
          >{{ row.targetNo ?? row.targetId ?? '—' }}
          <div class="muted">{{ row.targetType }}</div></template
        ></el-table-column
      ><el-table-column label="结果" width="90"
        ><template #default="{ row }"
          ><el-tag :type="row.result === 'SUCCESS' ? 'success' : 'warning'">{{
            row.result === 'SUCCESS' ? '成功' : '失败'
          }}</el-tag></template
        ></el-table-column
      ><el-table-column
        prop="reason"
        label="原因 / 说明"
        min-width="220"
        show-overflow-tooltip
      /><el-table-column label="操作" width="90" fixed="right"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="open(row.id)">查看记录</el-button></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 条操作记录</span
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
  <el-dialog :model-value="dialog" title="操作记录详情" width="760px" @close="close"
    ><div v-loading="detailLoading" class="log-detail">
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false"
        ><el-button @click="open(selectedId)">重试详情</el-button></el-alert
      ><template v-if="detail"
        ><el-descriptions :column="1" border
          ><el-descriptions-item label="操作者">{{ detail.record.actorName }}</el-descriptions-item
          ><el-descriptions-item label="动作"
            >{{ actionLabels[detail.record.action] ?? detail.record.action }} /
            {{ detail.record.action }}</el-descriptions-item
          ><el-descriptions-item label="发生时间（北京）">{{
            businessTime(detail.record.occurredAt)
          }}</el-descriptions-item
          ><el-descriptions-item label="对象">{{
            detail.record.targetNo ?? detail.record.targetId ?? '—'
          }}</el-descriptions-item
          ><el-descriptions-item label="结果">{{
            detail.record.result === 'SUCCESS' ? '成功' : '失败'
          }}</el-descriptions-item
          ><el-descriptions-item label="原因">{{
            detail.record.reason ?? '—'
          }}</el-descriptions-item
          ><el-descriptions-item label="追踪编号">{{
            detail.record.requestId ?? '—'
          }}</el-descriptions-item></el-descriptions
        ><template v-if="detail.before !== null || detail.after !== null"
          ><h3>变更前</h3>
          <pre>{{ JSON.stringify(detail.before, null, 2) }}</pre>
          <h3>变更后</h3>
          <pre>{{ JSON.stringify(detail.after, null, 2) }}</pre>
        </template>
        <p v-else class="muted">本动作未保存字段快照，请结合对象单号查看原业务记录。</p></template
      >
    </div></el-dialog
  >
</template>
<style scoped>
.log-detail {
  min-height: 120px;
}
.log-detail pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-height: 320px;
  overflow: auto;
  background: #f6f8f5;
  padding: 14px;
  line-height: 1.7;
}
.log-detail h3 {
  margin-top: 24px;
  font-size: 15px;
}
</style>
