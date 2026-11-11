<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../../stores/auth'

const auth = useAuth(),
  route = useRoute(),
  router = useRouter()
const busy = ref(false),
  error = ref('暂时无法读取登录身份，请检查服务连接后重试。')
function target() {
  const value = route.query.redirect
  return typeof value === 'string' &&
    value.startsWith('/') &&
    !value.startsWith('//') &&
    !value.startsWith('/login') &&
    !value.startsWith('/session-unavailable')
    ? value
    : '/'
}
async function retry() {
  if (busy.value) return
  busy.value = true
  try {
    await auth.load()
    await router.replace(target())
  } catch {
    if (auth.loggedIn) error.value = '登录身份仍未核实，请检查后端与数据库连接后再试。'
  } finally {
    busy.value = false
  }
}
async function otherAccount() {
  auth.clear()
  await router.replace({ path: '/login', query: { redirect: target() } })
}
</script>
<template>
  <main class="session-recovery">
    <section class="panel">
      <div class="eyebrow">禾序 · 登录连接</div>
      <h1>登录身份暂未核实</h1>
      <p>{{ error }}</p>
      <p class="muted">登录信息已保留。核验成功后返回原页面；核验期间不展示业务数据。</p>
      <div class="query-bar">
        <el-button type="primary" :loading="busy" :disabled="busy" @click="retry"
          >重试身份核验</el-button
        >
        <el-button :disabled="busy" @click="otherAccount">使用其他账号登录</el-button>
      </div>
    </section>
  </main>
</template>
<style scoped>
.session-recovery {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}
.session-recovery section {
  max-width: 620px;
}
.session-recovery p {
  line-height: 1.8;
}
.query-bar {
  margin-top: 24px;
  margin-bottom: 0;
}
</style>
