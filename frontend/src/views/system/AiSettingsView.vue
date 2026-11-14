<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  aiSettings,
  saveAiSettings,
  aiUsage,
  type AiSettings,
  type AiSettingsInput,
  type AiUsage,
  type AiUsageQuery,
  type AiUsageRow,
} from '../../api/aiSettings'
import { useAuth } from '../../stores/auth'
import { confirmUnsaved, useDirtyGuard } from '../../composables/useDirtyGuard'
import { businessTime } from '../../utils/format'
const auth = useAuth(),
  saved = ref<AiSettings | null>(null),
  loading = ref(false),
  saving = ref(false),
  error = ref(''),
  tab = ref('settings')
const inputTokens = (row: AiUsageRow) =>
  row.reportedCalls ? row.promptTokens.toLocaleString() : '未报告'
const outputTokens = (row: AiUsageRow) =>
  row.reportedCalls ? row.completionTokens.toLocaleString() : '未报告'
const totalTokens = (row: AiUsageRow) =>
  row.reportedCalls ? row.totalTokens.toLocaleString() : '未报告'
const form = reactive({
  defaultModel: '',
  models: [] as string[],
  keyAction: 'KEEP' as AiSettingsInput['keyAction'],
  apiKey: '',
})
const dirty = computed(
  () =>
    !!saved.value &&
    (form.defaultModel !== saved.value.defaultModel ||
      JSON.stringify(form.models) !== JSON.stringify(saved.value.models) ||
      form.keyAction !== 'KEEP' ||
      !!form.apiKey),
)
useDirtyGuard(dirty)
function assign(value: AiSettings) {
  saved.value = value
  form.defaultModel = value.defaultModel
  form.models = [...value.models]
  form.keyAction = 'KEEP'
  form.apiKey = ''
}
async function load() {
  if (saving.value || loading.value || (dirty.value && !(await confirmUnsaved()))) return
  loading.value = true
  error.value = ''
  try {
    assign(await aiSettings())
  } catch (e) {
    error.value = e instanceof Error ? e.message : '模型配置加载失败'
  } finally {
    loading.value = false
  }
}
async function save() {
  if (!saved.value || saving.value || loading.value) return
  error.value = ''
  if (
    !form.models.length ||
    form.models.length > 12 ||
    !form.models.includes(form.defaultModel) ||
    form.models.some((m) => !/^qwen[a-zA-Z0-9.:-]{0,95}$/.test(m))
  ) {
    error.value = '请填写1～12个有效千问模型ID，默认模型必须在列表中'
    return
  }
  if (form.keyAction === 'REPLACE' && (form.apiKey.length < 8 || /\s/.test(form.apiKey))) {
    error.value = '请输入有效API Key，不得包含空格或换行'
    return
  }
  saving.value = true
  try {
    assign(
      await saveAiSettings({
        expectedVersion: saved.value.version,
        defaultModel: form.defaultModel,
        models: form.models,
        keyAction: form.keyAction,
        ...(form.keyAction === 'REPLACE' ? { apiKey: form.apiKey } : {}),
      }),
    )
    ElMessage.success('模型配置已保存，新调用将使用更新后的设置')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败，请刷新核对后重试'
  } finally {
    saving.value = false
  }
}
const usage = ref<AiUsage | null>(null),
  usageLoading = ref(false),
  usageError = ref(''),
  query = reactive<AiUsageQuery>({})
let sequence = 0
async function loadUsage() {
  const current = ++sequence
  usageLoading.value = true
  usageError.value = ''
  usage.value = null
  try {
    const value = await aiUsage({ ...query })
    if (current !== sequence) return
    usage.value = value
    query.startDate = value.startDate
    query.endDate = value.endDate
  } catch (e) {
    if (current === sequence) usageError.value = e instanceof Error ? e.message : '用量加载失败'
  } finally {
    if (current === sequence) usageLoading.value = false
  }
}
function changeTab() {
  if (tab.value === 'usage') void loadUsage()
}
onMounted(load)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">MODEL OPERATIONS</div>
      <h1>模型配置与用量</h1>
      <p>管理通义千问连接、可选模型与本系统的实际调用消耗。</p>
    </div>
    <el-button :disabled="saving" :loading="loading" @click="load">刷新配置</el-button>
  </div>
  <el-tabs v-model="tab" @tab-change="changeTab"
    ><el-tab-pane label="连接与模型" name="settings" /><el-tab-pane label="调用用量" name="usage"
  /></el-tabs>
  <section v-if="tab === 'settings'" v-loading="loading" class="panel ai-settings">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-if="saved"
      ><div class="connection-status">
        <el-tag :type="saved.keyConfigured ? 'success' : 'warning'">{{
          saved.keyConfigured ? '已配置密钥' : '尚未配置密钥'
        }}</el-tag
        ><span class="muted"
          >{{
            saved.keySource === 'MANAGED'
              ? '使用管理界面保存的加密密钥'
              : '使用后端环境变量或本地文件'
          }}
          · 保存后不回显密钥</span
        >
      </div>
      <el-form
        label-position="top"
        :disabled="loading || saving || !auth.can('system.config.save')"
        @submit.prevent="save"
      >
        <el-form-item label="可选模型" required
          ><el-select
            v-model="form.models"
            multiple
            filterable
            allow-create
            default-first-option
            :multiple-limit="12"
            aria-label="可选千问模型"
            style="width: 100%"
            ><el-option
              v-for="m in ['qwen-plus', 'qwen-flash', 'qwen-max']"
              :key="m"
              :label="m"
              :value="m"
          /></el-select>
          <p class="muted field-note">
            可输入已开通的千问文本模型ID并回车添加，最多12个。模型能否调用取决于密钥所属地域与授权。
          </p></el-form-item
        >
        <el-form-item label="默认模型" required
          ><el-select v-model="form.defaultModel" aria-label="默认千问模型"
            ><el-option v-for="m in form.models" :key="m" :label="m" :value="m" /></el-select
        ></el-form-item>
        <el-form-item label="API Key 设置"
          ><el-select v-model="form.keyAction" aria-label="密钥操作" @change="form.apiKey = ''"
            ><el-option label="保留当前密钥" value="KEEP" /><el-option
              label="设置新的 API Key"
              value="REPLACE" /><el-option label="切回后端本地配置" value="USE_LOCAL" /></el-select
        ></el-form-item>
        <el-form-item v-if="form.keyAction === 'REPLACE'" label="新的 API Key" required
          ><el-input
            v-model="form.apiKey"
            type="password"
            autocomplete="new-password"
            :maxlength="4096"
            aria-label="新的模型API Key"
            placeholder="仅写入，不会回显已保存的密钥"
          />
          <p class="muted field-note">
            密钥在后端加密保存；保存成功后清空输入。不会出现在建议正文或操作记录中。
          </p></el-form-item
        >
        <el-alert
          v-if="form.keyAction === 'USE_LOCAL'"
          title="保存后移除管理界面的密钥覆盖，恢复使用后端环境变量或文件；若本地未配置，生成会明确失败。"
          type="warning"
          :closable="false"
        />
        <div class="query-bar">
          <el-button native-type="submit" type="primary" :loading="saving">保存模型配置</el-button
          ><span class="muted">进行中的调用保留原模型与密钥，历史记录保持原值。</span>
        </div>
      </el-form></template
    ><el-button v-else :disabled="loading" @click="load">重试加载模型配置</el-button>
  </section>
  <template v-else
    ><section class="panel">
      <form class="query-bar" @submit.prevent="loadUsage">
        <el-date-picker
          v-model="query.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          aria-label="模型用量开始日期"
          placeholder="开始日期"
        /><el-date-picker
          v-model="query.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          aria-label="模型用量结束日期"
          placeholder="结束日期"
        /><el-select
          v-model="query.model"
          filterable
          clearable
          allow-create
          aria-label="用量模型筛选"
          placeholder="全部模型"
          ><el-option
            v-for="m in [
              ...new Set([...(saved?.models ?? []), ...(usage?.models.map((m) => m.label) ?? [])]),
            ]"
            :key="m"
            :label="m"
            :value="m" /></el-select
        ><el-select
          v-model="query.type"
          clearable
          aria-label="用量建议类型"
          placeholder="全部建议类型"
          ><el-option label="库存优化建议" value="INVENTORY" /><el-option
            label="销售经营建议"
            value="SALES" /></el-select
        ><el-button native-type="submit" type="primary" :loading="usageLoading"
          >查询模型用量</el-button
        >
      </form>
      <p class="muted">
        按北京时间请求日期统计本系统的非夹具调用。Token
        来自服务商，未报告用量单独列出；不代表阿里云账户账单、余额或全部调用。
      </p>
    </section>
    <el-alert v-if="usageError" :title="usageError" type="error" :closable="false"
      ><el-button @click="loadUsage">重试用量查询</el-button></el-alert
    ><el-skeleton v-if="usageLoading" :rows="5" animated />
    <template v-if="usage"
      ><p class="muted">
        采集 {{ businessTime(usage.collectedAt) }}（北京） · {{ usage.startDate }} 至
        {{ usage.endDate }}
      </p>
      <div class="ai-usage-cards">
        <div>
          <span>调用次数</span><strong>{{ usage.totals.calls.toLocaleString() }}</strong
          ><small
            >成功 {{ usage.totals.successes }} · 失败 {{ usage.totals.failures }} · 待完成
            {{ usage.totals.pending }}</small
          >
        </div>
        <div>
          <span>已报告 Token 合计</span
          ><strong>{{ usage.totals.totalTokens.toLocaleString() }}</strong
          ><small
            >输入 {{ usage.totals.promptTokens.toLocaleString() }} · 输出
            {{ usage.totals.completionTokens.toLocaleString() }}</small
          >
        </div>
        <div>
          <span>用量报告覆盖</span
          ><strong>{{ usage.totals.reportedCalls }} / {{ usage.totals.calls }}</strong
          ><small>未报告 {{ usage.totals.unreportedCalls }} 次，不按零消耗计算</small>
        </div>
        <div>
          <span>已报告缓存输入 Token</span
          ><strong>{{ usage.totals.cachedTokens.toLocaleString() }}</strong
          ><small>{{ usage.totals.cacheReportedCalls }} 次提供缓存明细；已包含在输入中</small>
        </div>
      </div>
      <section class="panel">
        <h2>按模型汇总</h2>
        <el-table :data="usage.models" row-key="label" empty-text="当前范围没有真实调用"
          ><el-table-column prop="label" label="模型" min-width="180" /><el-table-column
            prop="calls"
            label="调用次数"
            width="100" /><el-table-column
            prop="successes"
            label="成功"
            width="75" /><el-table-column prop="failures" label="失败" width="75" /><el-table-column
            prop="promptTokens"
            :formatter="inputTokens"
            label="输入 Token"
            min-width="115" /><el-table-column
            prop="completionTokens"
            :formatter="outputTokens"
            label="输出 Token"
            min-width="115" /><el-table-column
            prop="totalTokens"
            :formatter="totalTokens"
            label="已知 Token 合计"
            min-width="145" /><el-table-column
            prop="unreportedCalls"
            label="未报告用量"
            width="120"
        /></el-table>
      </section>
      <section class="panel">
        <h2>每日用量</h2>
        <el-table
          :data="usage.days"
          row-key="label"
          max-height="420"
          empty-text="当前范围没有真实调用"
          ><el-table-column prop="label" label="日期（北京）" min-width="160" /><el-table-column
            prop="calls"
            label="调用次数" /><el-table-column
            prop="promptTokens"
            :formatter="inputTokens"
            label="输入 Token" /><el-table-column
            prop="completionTokens"
            :formatter="outputTokens"
            label="输出 Token" /><el-table-column
            prop="totalTokens"
            :formatter="totalTokens"
            label="已知 Token 合计" /><el-table-column prop="unreportedCalls" label="未报告用量"
        /></el-table>
      </section>
    </template>
  </template>
</template>
<style scoped>
.ai-settings {
  max-width: 900px;
}
.connection-status {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 26px;
  flex-wrap: wrap;
}
.field-note {
  margin: 8px 0 0;
  line-height: 1.7;
  width: 100%;
}
.ai-settings :deep(.el-select) {
  min-width: 260px;
}
.ai-settings .query-bar {
  margin-top: 24px;
}
.ai-usage-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 20px 0;
}
.ai-usage-cards > div {
  background: white;
  border: 1px solid #e1e8df;
  padding: 24px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ai-usage-cards strong {
  font-size: var(--font-size-metric);
  font-weight: 500;
  color: #245441;
}
.ai-usage-cards small {
  font-size: var(--font-size-caption);
  color: #758779;
  line-height: 1.6;
}
.panel + .panel {
  margin-top: 20px;
}
@media (max-width: 1200px) {
  .ai-usage-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
