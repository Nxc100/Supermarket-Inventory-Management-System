<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Grid,
  CollectionTag,
  User,
  SwitchButton,
  ArrowRight,
  Shop,
  Goods,
  Van,
} from '@element-plus/icons-vue'
import { useAuth } from '../stores/auth'
import { showError } from '../utils/feedback'
import { confirmUnsaved } from '../composables/useDirtyGuard'
import UserAvatar from '../components/UserAvatar.vue'

const auth = useAuth()
const route = useRoute()
const router = useRouter()
const busy = ref(false)
const title = computed(() => route.meta.title ?? '工作台')
async function signOut() {
  try {
    await ElMessageBox.confirm('退出后，该账号在其他页面的登录也将失效。', '退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (!(await confirmUnsaved())) return
    busy.value = true
    await auth.signOut()
    await router.replace('/login')
  } catch (error) {
    showError(error)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <router-link to="/" class="side-brand"
        ><span class="mark">禾</span><span>禾序<small>超市进销存</small></span></router-link
      >
      <div class="store-label">
        <el-icon><Shop /></el-icon> 单门店管理 <span>LOCAL</span>
      </div>
      <el-menu router :default-active="route.path" class="nav-menu">
        <el-menu-item
          v-if="auth.can('system.config') && auth.user?.roleCode === 'ADMIN'"
          index="/system/ai"
          ><el-icon><CollectionTag /></el-icon><span>模型配置与用量</span></el-menu-item
        >
        <el-menu-item
          v-if="auth.can('system.role') && auth.user?.roleCode === 'ADMIN'"
          index="/system/roles"
          ><el-icon><User /></el-icon><span>角色权限</span></el-menu-item
        >
        <el-menu-item
          v-if="auth.can('system.user') && auth.user?.roleCode === 'ADMIN'"
          index="/system/users"
          ><el-icon><User /></el-icon><span>用户管理</span></el-menu-item
        >
        <el-menu-item
          v-if="auth.can('system.config') && auth.user?.roleCode === 'ADMIN'"
          index="/system/defaults"
          ><el-icon><CollectionTag /></el-icon><span>业务参数</span></el-menu-item
        >
        <el-menu-item
          v-if="auth.can('system.log') && auth.user?.roleCode === 'ADMIN'"
          index="/system/logs"
          ><el-icon><CollectionTag /></el-icon><span>操作记录</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('common.dashboard')" index="/"
          ><el-icon><Grid /></el-icon><span>工作台</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('base.category')" index="/catalog/categories"
          ><el-icon><CollectionTag /></el-icon><span>商品分类</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('base.product')" index="/catalog/products"
          ><el-icon><Goods /></el-icon><span>商品档案</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('base.supplier')" index="/catalog/suppliers"
          ><el-icon><Van /></el-icon><span>供应商管理</span></el-menu-item
        >
        <el-menu-item
          v-if="auth.can('inventory.query')"
          :index="
            auth.user?.roleCode === 'CASHIER'
              ? '/inventory/saleable-products'
              : '/inventory/products'
          "
          ><el-icon><Goods /></el-icon><span>库存查询</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('purchase.order')" index="/purchases"
          ><el-icon><Van /></el-icon><span>采购单管理</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('purchase.order.review')" index="/purchases/review"
          ><el-icon><CollectionTag /></el-icon><span>入库审核</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('common.profile')" index="/profile"
          ><el-icon><User /></el-icon><span>个人中心</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('sale.order.complete')" index="/sales/new"
          ><el-icon><Shop /></el-icon><span>前台开单</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('sale.order')" index="/sales"
          ><el-icon><CollectionTag /></el-icon><span>销售记录</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('inventory.stocktake')" index="/stocktakes"
          ><el-icon><CollectionTag /></el-icon><span>库存盘点</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('inventory.warning')" index="/inventory/warnings"
          ><el-icon><Goods /></el-icon><span>库存预警</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('statistics.sales')" index="/statistics/sales"
          ><el-icon><Grid /></el-icon><span>销售统计</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('statistics.inventory')" index="/statistics/inventory"
          ><el-icon><Grid /></el-icon><span>库存统计</span></el-menu-item
        >
        <el-menu-item v-if="auth.can('ai.inventory') || auth.can('ai.sales')" index="/suggestions"
          ><el-icon><CollectionTag /></el-icon><span>智能经营建议</span></el-menu-item
        >
      </el-menu>
      <div class="sidebar-bottom">
        <span class="connection-dot"></span> 本地业务环境<small>SUPERMARKET INVENTORY</small>
      </div>
    </aside>
    <div class="main-shell">
      <header class="topbar">
        <div class="breadcrumbs">
          <span>门店管理</span><el-icon><ArrowRight /></el-icon><strong>{{ title }}</strong>
        </div>
        <div class="account-actions">
          <router-link to="/profile" class="account">
            <UserAvatar
              :path="auth.user?.avatarPath ?? null"
              :name="auth.user?.realName ?? ''"
              :size="32"
            />
            <span
              >{{ auth.user?.realName }}<small>{{ auth.user?.roleName }}</small></span
            >
          </router-link>
          <el-button text :loading="busy" @click="signOut"
            ><el-icon><SwitchButton /></el-icon><span>退出登录</span></el-button
          >
        </div>
      </header>
      <main class="content"><router-view :key="route.path" /></main>
      <footer class="app-footer">禾序 · 让门店经营更有条理</footer>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
}
.sidebar {
  width: 216px;
  position: fixed;
  inset: 0 auto 0 0;
  background: #fff;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: 28px 16px;
}
.side-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  color: #244b39;
  font-weight: 600;
  padding: 0 8px;
}
.side-brand small {
  display: block;
  font-size: 10px;
  color: #8a968c;
  letter-spacing: 2px;
  margin-top: 2px;
  font-weight: 400;
}
.mark {
  display: grid;
  place-items: center;
  background: #e7efd9;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  font-size: 26px;
}
.store-label {
  border: 1px solid var(--border);
  border-radius: 6px;
  margin: 32px 0 24px;
  padding: 12px 8px;
  color: #6d7d70;
  display: flex;
  gap: 8px;
  font-size: 12px;
}
.store-label span {
  font-size: 9px;
  color: #98a38f;
  margin-left: auto;
}
.nav-menu {
  border: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.side-brand,
.store-label,
.sidebar-bottom {
  flex-shrink: 0;
}
.nav-menu .el-menu-item {
  border-radius: 6px;
  height: 46px;
  margin-bottom: 6px;
}
.nav-menu .el-menu-item.is-active {
  background: #edf5f0;
  font-weight: 600;
}
.sidebar-bottom {
  margin-top: auto;
  font-size: 11px;
  color: #79897c;
  padding: 24px 8px 0;
}
.sidebar-bottom small {
  display: block;
  font-size: 8px;
  color: #a0aca1;
  letter-spacing: 1px;
  padding-top: 12px;
}
.connection-dot {
  display: inline-block;
  background: #5b9274;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}
.main-shell {
  width: calc(100% - 216px);
  margin-left: 216px;
  display: flex;
  flex-direction: column;
}
.topbar {
  height: 76px;
  background: #ffffffc9;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  gap: 16px;
}
.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #91a094;
  font-size: 12px;
}
.breadcrumbs strong {
  color: #526455;
  font-weight: 400;
}
.account-actions,
.account {
  display: flex;
  align-items: center;
  gap: 12px;
}
.account {
  color: #425745;
  font-size: 12px;
}
.account small {
  display: block;
  color: #97a195;
  font-size: 10px;
  margin-top: 4px;
}
.account .el-avatar {
  background: #edf1e7;
  color: #718263;
}
.account-actions > .el-button {
  border-left: 1px solid var(--border);
  margin-left: 8px;
  border-radius: 0;
  gap: 6px;
  font-size: 12px;
}
.content {
  padding: 32px;
  flex: 1;
  min-width: 0;
}
.app-footer {
  color: #a2aba0;
  font-size: 10px;
  padding: 0 32px 20px;
}
@media (max-width: 1000px) {
  .sidebar {
    width: 176px;
    padding: 24px 12px;
  }
  .main-shell {
    margin-left: 176px;
    width: calc(100% - 176px);
  }
  .content {
    padding: 24px 16px;
  }
  .topbar {
    padding: 0 16px;
  }
  .store-label {
    font-size: 10px;
  }
  .breadcrumbs > span {
    display: none;
  }
}
</style>
