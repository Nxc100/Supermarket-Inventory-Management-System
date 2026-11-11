<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import { useAuth } from '../../stores/auth'
import { useDirtyGuard, confirmUnsaved } from '../../composables/useDirtyGuard'
import { ApiError } from '../../api/http'
import * as api from '../../api/roles'
import type { PermissionNode, RoleDetail, RoleSummary } from '../../api/roles'
import type { StaffRole } from '../../api/staff'
import { businessTime } from '../../utils/format'
const auth = useAuth(),
  router = useRouter(),
  rows = ref<RoleSummary[]>([]),
  listLoading = ref(false),
  listError = ref(''),
  detail = ref<RoleDetail | null>(null),
  selectedRole = ref<StaffRole | null>(null),
  loading = ref(false),
  busy = ref(false),
  error = ref(''),
  selected = ref(new Set<string>()),
  treeRef = ref<InstanceType<typeof ElTree>>()
const dirty = computed(
  () =>
    !!detail.value &&
    JSON.stringify([...selected.value].sort()) !==
      JSON.stringify([...detail.value.permissions].sort()),
)
useDirtyGuard(dirty)
interface TreeNode extends PermissionNode {
  disabled: boolean
  children: TreeNode[]
}
const nodes = computed(() => new Map(detail.value?.nodes.map((n) => [n.code, n]) ?? []))
const tree = computed(() => {
  if (!detail.value) return []
  const all = new Map<string, TreeNode>()
  for (const node of detail.value.nodes)
    all.set(node.code, {
      ...node,
      disabled:
        busy.value ||
        !auth.can('system.role.save') ||
        detail.value.protectedPermissions.includes(node.code),
      children: [],
    })
  const roots: TreeNode[] = []
  for (const node of all.values()) {
    const parent = node.parent ? all.get(node.parent) : null
    if (parent) parent.children.push(node)
    else roots.push(node)
  }
  return roots
})
async function load() {
  listLoading.value = true
  listError.value = ''
  rows.value = []
  try {
    rows.value = await api.roles()
  } catch (e) {
    listError.value = e instanceof Error ? e.message : '岗位列表读取失败'
  } finally {
    listLoading.value = false
  }
}
async function accept(value: RoleDetail) {
  detail.value = value
  selected.value = new Set(value.permissions)
  await nextTick()
  treeRef.value?.setCheckedKeys([...selected.value])
}
async function open(code: StaffRole) {
  if (loading.value || busy.value) return
  if (dirty.value && !(await confirmUnsaved())) return
  loading.value = true
  selectedRole.value = code
  detail.value = null
  error.value = ''
  try {
    await accept(await api.role(code))
  } catch (e) {
    error.value = e instanceof Error ? e.message : '权限树读取失败'
  } finally {
    loading.value = false
  }
}
function check(node: PermissionNode, info: { checkedKeys: (string | number)[] }) {
  const next = new Set(info.checkedKeys.filter((v): v is string => typeof v === 'string'))
  if (next.has(node.code)) {
    let parent = node.parent
    const visited = new Set<string>()
    while (parent && !visited.has(parent)) {
      next.add(parent)
      visited.add(parent)
      parent = nodes.value.get(parent)?.parent ?? null
    }
  } else {
    const remove = (code: string) => {
      for (const item of nodes.value.values())
        if (item.parent === code) {
          next.delete(item.code)
          remove(item.code)
        }
    }
    remove(node.code)
  }
  for (const code of detail.value?.protectedPermissions ?? []) next.add(code)
  selected.value = next
  treeRef.value?.setCheckedKeys([...next])
}
async function apply(restore: boolean) {
  if (!detail.value || busy.value || loading.value) return
  const captured = detail.value
  try {
    await ElMessageBox.confirm(
      `${restore ? '放弃当前未保存勾选，恢复' : '保存'}“${captured.role.name}”的${restore ? '默认权限' : '当前权限'}？该岗位全部已登录账号的原凭证将失效，需重新登录。`,
      restore ? '恢复默认权限' : '保存岗位权限',
      {
        confirmButtonText: restore ? '恢复默认权限' : '保存岗位权限',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  busy.value = true
  error.value = ''
  try {
    const result = restore
      ? await api.restoreRole(captured)
      : await api.saveRole(captured, [...selected.value])
    if (result.role.code === auth.user?.roleCode) {
      detail.value = null
      selected.value = new Set()
      auth.clear()
      await router.replace({ path: '/login', query: { redirect: '/system/roles' } })
      ElMessage.info('本岗位权限已更新，请重新登录')
      return
    }
    await accept(result)
    ElMessage.success(`${result.role.name}权限已更新，相关用户需重新登录`)
    await load()
  } catch (e) {
    error.value =
      e instanceof ApiError && (e.status === 0 || e.status >= 500)
        ? '本次权限变更结果尚未核实，请刷新权限树确认后再操作'
        : e instanceof Error
          ? e.message
          : '权限变更失败，请刷新核对后重试'
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">ROLE PERMISSIONS</div>
      <h1>角色权限</h1>
      <p>固定三个岗位，在各岗位允许范围内配置菜单与操作；单据本人范围不随菜单授权扩大。</p>
    </div>
    <el-button :loading="listLoading" :disabled="busy || loading" @click="load"
      >刷新岗位列表</el-button
    >
  </div>
  <section class="panel">
    <el-alert v-if="listError" :title="listError" type="error" :closable="false"
      ><el-button @click="load">重试岗位列表</el-button></el-alert
    ><el-table v-loading="listLoading" :data="rows" row-key="code" empty-text="岗位列表尚未加载"
      ><el-table-column prop="name" label="岗位" min-width="160" /><el-table-column
        label="数据范围"
        min-width="360"
        ><template #default="{ row }">{{ row.scopeNote }}</template></el-table-column
      ><el-table-column prop="permissionCount" label="授权节点" width="100" /><el-table-column
        label="操作"
        width="120"
        ><template #default="{ row }"
          ><el-button link type="primary" :disabled="loading || busy" @click="open(row.code)"
            >配置权限</el-button
          ></template
        ></el-table-column
      ></el-table
    >
  </section>
  <section v-if="selectedRole" v-loading="loading" class="panel role-editor">
    <el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button :disabled="busy || loading" @click="open(selectedRole)"
        >刷新权限树</el-button
      ></el-alert
    ><template v-if="detail"
      ><div class="page-heading">
        <div>
          <h2>{{ detail.role.name }} · 权限树</h2>
          <p>{{ detail.role.scopeNote }}</p>
        </div>
        <el-button v-if="!error" :disabled="busy || loading" @click="open(detail.role.code)"
          >刷新权限树</el-button
        >
      </div>
      <p class="muted">
        勾选操作会同时保留所属页面和模块；取消页面会移除其下操作。灰色节点为岗位基础功能和必要管理入口，不能取消。
      </p>
      <p class="muted">
        更新于 {{ businessTime(detail.role.updatedAt) }}（北京） · 已选 {{ selected.size }} 个节点{{
          dirty ? ' · 有未保存修改' : ''
        }}
      </p>
      <el-tree
        ref="treeRef"
        :data="tree"
        node-key="code"
        :props="{ label: 'label', children: 'children', disabled: 'disabled' }"
        show-checkbox
        check-strictly
        default-expand-all
        :expand-on-click-node="false"
        aria-label="岗位权限树"
        @check="check"
        ><template #default="{ data }"
          ><span :data-permission="data.code">{{ data.label }}</span></template
        ></el-tree
      >
      <div class="query-bar role-actions">
        <el-button
          v-if="auth.can('system.role.save')"
          type="primary"
          :loading="busy"
          :disabled="!dirty || loading"
          @click="apply(false)"
          >保存岗位权限</el-button
        ><el-button
          v-if="auth.can('system.role.restore')"
          :disabled="busy || loading"
          @click="apply(true)"
          >恢复默认权限</el-button
        >
      </div></template
    ><el-empty v-else-if="!loading" description="权限树未加载，请重试" />
  </section>
</template>
<style scoped>
.role-editor {
  margin-top: 24px;
  min-height: 160px;
}
.el-alert {
  margin-bottom: 18px;
}
.role-editor p {
  line-height: 1.8;
}
.role-editor .el-tree {
  margin-top: 20px;
}
.role-actions {
  margin-top: 24px;
  margin-bottom: 0;
}
</style>
