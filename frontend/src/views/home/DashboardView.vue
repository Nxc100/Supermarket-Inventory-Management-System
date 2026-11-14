<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuth } from '../../stores/auth'
import { dashboard, type Dashboard } from '../../api/dashboard'
import { businessTime } from '../../utils/format'
const auth = useAuth()
const data = ref<Dashboard | null>(null),
  loading = ref(false),
  error = ref('')
async function load() {
  if (loading.value) return
  loading.value = true
  data.value = null
  error.value = ''
  try {
    data.value = await dashboard()
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '工作台加载失败，请重试'
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <div class="eyebrow">WORKSPACE</div>
        <h1>{{ auth.user?.realName }}，欢迎回来</h1>
        <p>当前岗位：{{ auth.user?.roleName }}。{{ data?.scopeNote ?? '正在读取岗位工作台' }}</p>
      </div>
      <el-button :loading="loading" @click="load">刷新工作台</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon>
      <el-button @click="load">重试工作台</el-button>
    </el-alert>
    <section v-loading="loading" :aria-busy="loading" class="dashboard-content">
      <template v-if="data">
        <p class="muted dashboard-time">
          北京业务日 {{ data.businessDate }} · 采集
          {{ businessTime(data.collectedAt) }}。卡片点击后使用相同筛选；业务变化后请刷新。
        </p>
        <div class="metric-grid">
          <router-link
            v-for="metric in data.metrics"
            :key="metric.code"
            :data-code="metric.code"
            :to="{ path: metric.path, query: metric.query }"
            class="panel dashboard-card"
          >
            <span class="metric-label">{{ metric.label }}</span>
            <div class="metric-value">
              {{ metric.value }} <small>{{ metric.unit }}</small>
            </div>
            <p>{{ metric.note }}</p>
            <span class="metric-link">查看对应记录 →</span>
          </router-link>
        </div>
        <el-empty
          v-if="!data.metrics.length"
          description="当前岗位未授权业务指标，请使用已授权入口"
        />
        <section class="panel dashboard-shortcuts">
          <div class="eyebrow">QUICK ACTIONS</div>
          <h2>常用操作</h2>
          <div class="shortcut-links">
            <router-link
              v-for="item in data.shortcuts"
              :key="item.label"
              :to="{ path: item.path, query: item.query }"
              ><el-button>{{ item.label }} →</el-button></router-link
            >
            <router-link v-if="auth.can('common.profile')" to="/profile"
              ><el-button>个人中心 →</el-button></router-link
            >
          </div>
        </section>
      </template>
    </section>
  </section>
</template>

<style scoped>
.dashboard-content {
  min-height: 180px;
}
.dashboard-time {
  margin-bottom: 20px;
  font-size: var(--font-size-caption);
  line-height: 1.8;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}
.dashboard-card {
  color: #294d37;
  transition: border-color 0.15s;
}
.dashboard-card:hover {
  border-color: #86a98d;
}
.metric-label {
  color: #5e7165;
  font-size: var(--font-size-body);
}
.metric-value {
  margin: 18px 0 12px;
  font-size: var(--font-size-metric);
  overflow-wrap: anywhere;
  font-variant-numeric: tabular-nums;
}
.metric-value small {
  font-size: var(--font-size-body);
  color: #7c8a80;
}
.dashboard-card p {
  color: #758278;
  font-size: var(--font-size-caption);
  min-height: 45px;
  line-height: 1.6;
}
.metric-link {
  font-size: var(--font-size-caption);
}
.dashboard-shortcuts {
  margin-top: 24px;
}
.dashboard-shortcuts h2 {
  font-size: var(--font-size-heading);
  margin: 10px 0 20px;
}
.shortcut-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
