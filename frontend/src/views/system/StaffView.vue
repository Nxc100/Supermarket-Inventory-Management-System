<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuth } from '../../stores/auth'
import { usePagedList } from '../../composables/usePagedList'
import { ApiError } from '../../api/http'
import * as api from '../../api/staff'
import { staffRoles, type Staff, type StaffQuery, type StaffTask } from '../../api/staff'
import type { PageQuery } from '../../types/api'
import { businessTime } from '../../utils/format'
import { purchaseStatuses } from '../../utils/purchase'
import { stocktakeStatuses, type StocktakeStatus } from '../../types/stocktake'
import StaffEditor, { type EditorMode } from './StaffEditor.vue'
const auth = useAuth(),
  router = useRouter()
const { query, rows, total, loading, error, load, search, reset } = usePagedList<Staff, StaffQuery>(
  { keyword: '', pageNum: 1, pageSize: 10 },
  api.staffPage,
)
const editor = ref(false),
  mode = ref<EditorMode>('CREATE'),
  account = ref<Staff | null>(null),
  opening = ref(false),
  busy = ref(''),
  actionError = ref('')
const taskOwner = ref<Staff | null>(null),
  tasksOpen = ref(false)
const tasks = usePagedList<StaffTask, PageQuery>({ keyword: '', pageNum: 1, pageSize: 10 }, (q) =>
  api.staffTasks(taskOwner.value?.id ?? '', q),
)
function pending(row: Staff) {
  return row.unfinishedPurchases + row.activeStocktakes > 0
}
function cannotDisable(row: Staff) {
  return row.enabled && (row.id === auth.user?.id || pending(row))
}
function cannotDelete(row: Staff) {
  return row.id === auth.user?.id || pending(row) || row.businessReferenced
}
async function open(kind: EditorMode, row?: Staff) {
  if (opening.value || busy.value) return
  opening.value = true
  actionError.value = ''
  try {
    account.value = row ? await api.staff(row.id) : null
    mode.value = kind
    editor.value = true
  } catch (e) {
    actionError.value = e instanceof Error ? e.message : '目标账号读取失败，请重试'
  } finally {
    opening.value = false
  }
}
async function saved(row: Staff, invalidate: boolean) {
  actionError.value = ''
  if (row.id === auth.user?.id) {
    if (invalidate) {
      auth.clear()
      await router.replace({ path: '/login', query: { redirect: '/system/users' } })
      ElMessage.info('账号安全设置已变更，请重新登录')
      return
    }
    try {
      await auth.load()
    } catch {
      if (auth.loggedIn) actionError.value = '账号已保存，但顶部资料刷新失败，请刷新页面核对'
    }
  }
  await load()
}
async function refresh() {
  actionError.value = ''
  await load()
}
async function changeState(row: Staff) {
  if (busy.value || opening.value) return
  const enabled = !row.enabled
  try {
    await ElMessageBox.confirm(
      `${enabled ? '启用' : '停用'}账号 ${row.username}（${row.realName}）？${enabled ? '启用后可重新登录。' : '其原登录将立即失效，历史业务保留。'}`,
      enabled ? '启用用户' : '停用用户',
      {
        confirmButtonText: enabled ? '启用用户' : '停用用户',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  busy.value = row.id
  actionError.value = ''
  try {
    await api.staffState(row, enabled)
    ElMessage.success(`账号 ${row.username} 已${enabled ? '启用' : '停用'}`)
    await load()
  } catch (e) {
    actionError.value =
      e instanceof ApiError && (e.status === 0 || e.status >= 500)
        ? '账号状态结果尚未核实，请刷新列表核对后再操作'
        : e instanceof Error
          ? e.message
          : '操作失败，请刷新核对后重试'
  } finally {
    busy.value = ''
  }
}
async function remove(row: Staff) {
  if (busy.value || opening.value) return
  try {
    await ElMessageBox.confirm(
      `删除从未产生业务关联的账号 ${row.username}（${row.realName}）？此操作不能撤销，历史关联账号应使用停用。`,
      '删除未使用账号',
      { confirmButtonText: '删除账号', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  busy.value = row.id
  actionError.value = ''
  try {
    await api.deleteStaff(row)
    ElMessage.success(`账号 ${row.username} 已删除`)
    await load()
  } catch (e) {
    actionError.value = e instanceof Error ? e.message : '删除结果尚未核实，请刷新列表确认'
  } finally {
    busy.value = ''
  }
}
async function showTasks(row: Staff) {
  taskOwner.value = row
  tasksOpen.value = true
  tasks.query.pageNum = 1
  await tasks.load()
}
function taskStatus(task: StaffTask) {
  return task.type === 'PURCHASE'
    ? purchaseStatuses[task.status as keyof typeof purchaseStatuses]
    : stocktakeStatuses[task.status as StocktakeStatus]
}
onMounted(load)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">STAFF ACCOUNTS</div>
      <h1>用户管理</h1>
      <p>固定三类岗位。变更岗位或停用前须处理未完成任务；历史单据保留经办人快照。</p>
    </div>
    <el-button
      v-if="auth.can('system.user.create')"
      type="primary"
      :disabled="opening || !!busy"
      @click="open('CREATE')"
      >新增用户</el-button
    >
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        aria-label="用户关键词"
        placeholder="用户名 / 姓名"
        clearable
      /><el-select v-model="query.role" aria-label="用户岗位筛选" placeholder="全部岗位" clearable
        ><el-option
          v-for="(label, value) in staffRoles"
          :key="value"
          :label="label"
          :value="value" /></el-select
      ><el-select v-model="query.enabled" aria-label="用户状态筛选" placeholder="全部状态" clearable
        ><el-option label="已启用" :value="true" /><el-option
          label="已停用"
          :value="false" /></el-select
      ><el-button native-type="submit" type="primary" :loading="loading">查询用户</el-button
      ><el-button @click="reset">重置</el-button
      ><el-button :loading="loading" @click="refresh">刷新用户</el-button>
    </form>
    <el-alert
      v-if="error || actionError"
      :title="actionError || error"
      type="error"
      :closable="false"
      ><el-button @click="refresh">刷新并核对</el-button></el-alert
    >
    <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的用户"
      ><el-table-column label="用户名 / 姓名" min-width="190"
        ><template #default="{ row }"
          >{{ row.username }}
          <div class="muted">
            {{ row.realName }}{{ row.id === auth.user?.id ? ' · 当前账号' : '' }}
          </div></template
        ></el-table-column
      ><el-table-column prop="roleName" label="岗位" min-width="150" /><el-table-column
        prop="phone"
        label="手机号"
        width="140"
      /><el-table-column label="状态" width="100"
        ><template #default="{ row }"
          ><el-tag :type="row.enabled ? 'success' : 'info'">{{
            row.enabled ? '已启用' : '已停用'
          }}</el-tag></template
        ></el-table-column
      ><el-table-column label="未完成任务" min-width="185"
        ><template #default="{ row }"
          ><div>采购 {{ row.unfinishedPurchases }} / 盘点 {{ row.activeStocktakes }}</div>
          <el-button v-if="pending(row)" link type="primary" @click="showTasks(row)"
            >处理未完成任务</el-button
          ></template
        ></el-table-column
      ><el-table-column label="创建时间（北京）" min-width="180"
        ><template #default="{ row }">{{ businessTime(row.createdAt) }}</template></el-table-column
      ><el-table-column label="操作" width="275" fixed="right"
        ><template #default="{ row }"
          ><el-button
            v-if="auth.can('system.user.edit')"
            link
            type="primary"
            :disabled="opening || !!busy"
            @click="open('EDIT', row)"
            >编辑用户</el-button
          ><el-tooltip
            :content="
              row.id === auth.user?.id
                ? '不能停用当前账号'
                : pending(row) && row.enabled
                  ? '请先处理未完成任务'
                  : row.enabled
                    ? '停用后原登录失效'
                    : '启用后可登录'
            "
            ><span
              ><el-button
                v-if="auth.can('system.user.enable')"
                link
                :type="row.enabled ? 'warning' : 'primary'"
                :disabled="cannotDisable(row) || opening || !!busy"
                @click="changeState(row)"
                >{{ row.enabled ? '停用用户' : '启用用户' }}</el-button
              ></span
            ></el-tooltip
          ><el-button
            v-if="auth.can('system.user.reset_password')"
            link
            type="primary"
            :disabled="opening || !!busy"
            @click="open('RESET', row)"
            >重置密码</el-button
          ><el-tooltip
            :content="
              row.id === auth.user?.id
                ? '不能删除当前账号'
                : row.businessReferenced
                  ? '已有业务关联，请使用停用'
                  : '仅允许删除未使用账号'
            "
            ><span
              ><el-button
                v-if="auth.can('system.user.delete')"
                link
                type="danger"
                :disabled="cannotDelete(row) || opening || !!busy"
                @click="remove(row)"
                >删除账号</el-button
              ></span
            ></el-tooltip
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 个用户 · 最后启用管理员受保护</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :disabled="loading"
        :total="total"
        layout="sizes,prev,pager,next"
        @current-change="load"
        @size-change="search"
      />
    </div>
  </section>
  <StaffEditor v-model="editor" :mode="mode" :account="account" @saved="saved" />
  <el-dialog v-model="tasksOpen" :title="`${taskOwner?.username ?? ''} · 未完成任务`" width="860px"
    ><p class="muted">
      管理员可接续处理或关闭这些任务。关闭盘点会解除其冻结且不调整库存，完成处理后请返回刷新用户列表。
    </p>
    <el-alert v-if="tasks.error.value" :title="tasks.error.value" type="error" :closable="false"
      ><el-button @click="tasks.load">重试任务</el-button></el-alert
    ><el-table
      v-loading="tasks.loading.value"
      :data="tasks.rows.value"
      empty-text="当前无未完成任务"
      ><el-table-column label="业务" width="100"
        ><template #default="{ row }">{{
          row.type === 'PURCHASE' ? '采购' : '盘点'
        }}</template></el-table-column
      ><el-table-column prop="orderNo" label="单号" min-width="250" /><el-table-column
        label="状态"
        width="100"
        ><template #default="{ row }">{{ taskStatus(row) }}</template></el-table-column
      ><el-table-column label="操作" width="110"
        ><template #default="{ row }"
          ><router-link
            v-if="auth.can(row.type === 'PURCHASE' ? 'purchase.order' : 'inventory.stocktake')"
            :to="row.type === 'PURCHASE' ? `/purchases/${row.id}` : `/stocktakes/${row.id}`"
            ><el-button link type="primary">打开任务</el-button></router-link
          ><span v-else class="muted">需先恢复对应业务页面权限</span></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ tasks.total.value }} 个任务</span
      ><el-pagination
        v-model:current-page="tasks.query.pageNum"
        v-model:page-size="tasks.query.pageSize"
        :page-sizes="[10, 20, 50]"
        :disabled="tasks.loading.value"
        :total="tasks.total.value"
        layout="sizes,prev,pager,next"
        @current-change="tasks.load"
        @size-change="tasks.search"
      /></div
  ></el-dialog>
</template>
<style scoped>
.el-alert {
  margin-bottom: 16px;
}
.muted {
  line-height: 1.7;
}
</style>
