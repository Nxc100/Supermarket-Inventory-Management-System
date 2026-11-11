<script setup lang="ts">
import { navigationFailure, router } from './router'
function reopen() {
  if (navigationFailure.value)
    window.location.assign(router.resolve(navigationFailure.value.target).href)
}
</script>
<template>
  <section v-if="navigationFailure" role="alert" class="navigation-failure">
    <div>
      <strong>“{{ navigationFailure.title }}”未能打开</strong>
      <p>页面资源加载失败，请检查连接后重新打开。当前页面内容仍保留。</p>
    </div>
    <div class="navigation-actions">
      <el-button type="primary" @click="reopen">重新打开页面</el-button>
      <el-button @click="navigationFailure = null">留在当前页面</el-button>
    </div>
  </section>
  <router-view />
</template>
<style scoped>
.navigation-failure {
  position: fixed;
  z-index: 3000;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  width: min(720px, calc(100% - 32px));
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding: 20px 24px;
  border: 1px solid #e7d5a8;
  border-radius: 12px;
  background: #fffaf0;
  color: #594a2d;
  box-shadow: 0 8px 32px #263b2526;
}
.navigation-failure p {
  margin: 8px 0 0;
  line-height: 1.7;
  font-size: 13px;
}
.navigation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.navigation-actions .el-button {
  margin: 0;
}
</style>
