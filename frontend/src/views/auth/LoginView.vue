<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../../stores/auth'

const auth = useAuth()
const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '' })
const busy = ref(false)
const errorMessage = ref('')
async function submit() {
  if (busy.value) return
  if (!form.username.trim() || !form.password) {
    errorMessage.value = '请输入用户名与密码'
    return
  }
  busy.value = true
  errorMessage.value = ''
  try {
    await auth.signIn(form.username.trim(), form.password)
    const target = route.query.redirect
    await router.replace(
      typeof target === 'string' &&
        target.startsWith('/') &&
        !target.startsWith('//') &&
        !target.startsWith('/login')
        ? target
        : '/',
    )
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
    form.password = ''
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <div class="brand">
        <span class="brand-mark">禾</span><span>禾序 <small>零售管理</small></span>
      </div>
      <div class="story-content">
        <div class="eyebrow">SUPERMARKET OPERATIONS</div>
        <h1>让每一件商品，<br />都有清晰的来与往。</h1>
        <p>从采购到销售，从库存到经营。<br />把门店日常，梳理得井井有条。</p>
        <div class="story-line"></div>
        <div class="story-points">
          <span>01 采购入库</span><span>02 库存追溯</span><span>03 经营分析</span>
        </div>
      </div>
      <div class="story-footer">单门店 · 精细管理 · 稳健经营</div>
    </section>
    <section class="login-form-area">
      <div class="login-card">
        <div class="eyebrow">WELCOME BACK</div>
        <h2>登录工作台</h2>
        <p class="muted">使用门店分配的账号，开始今天的工作。</p>
        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          :closable="false"
          show-icon
          class="login-error"
        />
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名" required
            ><el-input
              v-model="form.username"
              name="username"
              placeholder="请输入用户名"
              autocomplete="username"
              size="large"
              maxlength="20"
          /></el-form-item>
          <el-form-item label="密码" required
            ><el-input
              v-model="form.password"
              name="password"
              placeholder="请输入密码"
              type="password"
              autocomplete="current-password"
              size="large"
              maxlength="100"
          /></el-form-item>
          <el-button
            type="primary"
            size="large"
            native-type="submit"
            :loading="busy"
            :disabled="busy"
            class="login-submit"
            >{{ busy ? '登录中' : '登录' }}</el-button
          >
        </el-form>
        <p class="login-help">账号由管理员创建。如需帮助，请联系门店管理员。</p>
      </div>
      <div class="login-bottom">禾序 · 超市进销存管理系统</div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 48% 52%;
}
.login-story {
  background: #153d31;
  color: #fff;
  padding: 40px 48px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}
.login-story::after {
  content: '';
  width: 500px;
  height: 500px;
  border: 1px solid #ffffff0a;
  border-radius: 50%;
  position: absolute;
  right: -270px;
  bottom: -160px;
  box-shadow:
    0 0 0 60px #ffffff03,
    0 0 0 120px #ffffff03;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 23px;
  font-weight: 600;
}
.brand small {
  font-size: 12px;
  font-weight: 400;
  margin-left: 8px;
  color: #becdc3;
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  background: #d7e6b9;
  border-radius: 10px;
  color: #214c39;
  font-size: 23px;
}
.story-content {
  margin: auto 0;
  padding: 64px 0;
  z-index: 1;
}
.story-content .eyebrow {
  color: #a8c4aa;
}
.story-content h1 {
  font-size: clamp(30px, 3.2vw, 48px);
  font-weight: 500;
  line-height: 1.5;
  margin: 24px 0;
}
.story-content p {
  line-height: 2;
  color: #bccfc2;
  font-size: 15px;
}
.story-line {
  width: 48px;
  height: 2px;
  background: #bed2a0;
  margin: 40px 0 24px;
}
.story-points {
  display: flex;
  gap: 24px;
  font-size: 12px;
  color: #c4d5c6;
}
.story-footer {
  font-size: 12px;
  color: #88a694;
}
.login-form-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafbf8;
  padding: 40px;
  position: relative;
}
.login-card {
  width: 100%;
  max-width: 360px;
}
.login-card h2 {
  font-size: 28px;
  margin-bottom: 12px;
  font-weight: 500;
}
.login-card > p {
  margin-bottom: 36px;
  line-height: 1.8;
}
.login-submit {
  width: 100%;
  margin-top: 12px;
}
.login-help {
  font-size: 12px;
  color: #939c94;
  line-height: 1.8;
  margin-top: 24px;
}
.login-bottom {
  position: absolute;
  bottom: 32px;
  color: #a2aaa2;
  font-size: 12px;
}
.login-error {
  margin-bottom: 20px;
}
@media (max-width: 800px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .login-story {
    display: none;
  }
  .login-form-area {
    min-height: 100vh;
    padding: 24px;
  }
}
</style>
