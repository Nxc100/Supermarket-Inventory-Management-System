import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './styles/tokens.css'
import App from './App.vue'
import { router } from './router'
import { registerExpired } from './api/http'
import { useAuth } from './stores/auth'

const app = createApp(App)
app.use(createPinia()).use(router).use(ElementPlus, { locale: zhCn })
registerExpired(() => {
  useAuth().clear()
  ElMessage.warning('登录已失效，请重新登录')
  void router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
})
app.mount('#app')
