import { createRouter, createWebHistory } from 'vue-router'
import { ref } from 'vue'
import { useAuth } from '../stores/auth'

export const navigationFailure = ref<{ target: string; title: string } | null>(null)

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/auth/LoginView.vue') },
    {
      path: '/session-unavailable',
      component: () => import('../views/auth/SessionUnavailableView.vue'),
    },
    {
      path: '/',
      component: () => import('../layouts/AppLayout.vue'),
      children: [
        {
          path: 'system/ai',
          component: () => import('../views/system/AiSettingsView.vue'),
          meta: { title: '模型配置与用量', permission: 'system.config', roles: ['ADMIN'] },
        },
        {
          path: 'system/roles',
          component: () => import('../views/system/RoleView.vue'),
          meta: { title: '角色权限', permission: 'system.role', roles: ['ADMIN'] },
        },
        {
          path: 'system/users',
          component: () => import('../views/system/StaffView.vue'),
          meta: { title: '用户管理', permission: 'system.user', roles: ['ADMIN'] },
        },
        {
          path: 'system/defaults',
          component: () => import('../views/system/SystemDefaultsView.vue'),
          meta: { title: '业务参数', permission: 'system.config', roles: ['ADMIN'] },
        },
        {
          path: 'system/logs',
          component: () => import('../views/system/OperationLogView.vue'),
          meta: { title: '操作记录', permission: 'system.log', roles: ['ADMIN'] },
        },
        {
          path: 'suggestions',
          component: () => import('../views/suggestion/SuggestionView.vue'),
          meta: {
            title: '智能经营建议',
            anyPermission: ['ai.inventory', 'ai.sales'],
            roles: ['ADMIN', 'PURCHASER'],
          },
        },
        {
          path: 'suggestions/:id',
          component: () => import('../views/suggestion/SuggestionDetailView.vue'),
          meta: {
            title: '建议历史详情',
            anyPermission: ['ai.inventory', 'ai.sales'],
            roles: ['ADMIN', 'PURCHASER'],
          },
        },
        {
          path: 'statistics/sales',
          component: () => import('../views/statistics/SalesStatisticsView.vue'),
          meta: { title: '销售统计', permission: 'statistics.sales', roles: ['ADMIN', 'CASHIER'] },
        },
        {
          path: 'statistics/inventory',
          component: () => import('../views/statistics/InventoryStatisticsView.vue'),
          meta: {
            title: '库存统计',
            permission: 'statistics.inventory',
            roles: ['ADMIN', 'PURCHASER'],
          },
        },
        {
          path: 'inventory/warnings',
          component: () => import('../views/inventory/StockWarningView.vue'),
          meta: {
            title: '库存预警',
            permission: 'inventory.warning',
            roles: ['ADMIN', 'PURCHASER'],
          },
        },
        {
          path: 'stocktakes',
          component: () => import('../views/stocktake/StocktakeListView.vue'),
          meta: {
            title: '库存盘点',
            permission: 'inventory.stocktake',
            roles: ['ADMIN', 'PURCHASER'],
          },
        },
        {
          path: 'stocktakes/new',
          component: () => import('../views/stocktake/StocktakeStartView.vue'),
          meta: {
            title: '新建盘点',
            permission: 'inventory.stocktake.start',
            roles: ['ADMIN', 'PURCHASER'],
          },
        },
        {
          path: 'stocktakes/:id',
          component: () => import('../views/stocktake/StocktakeDetailView.vue'),
          meta: {
            title: '盘点详情',
            permission: 'inventory.stocktake',
            roles: ['ADMIN', 'PURCHASER'],
          },
        },
        {
          path: 'sales/new',
          component: () => import('../views/sale/SaleCheckoutView.vue'),
          meta: { title: '前台开单', permission: 'sale.order.complete' },
        },
        {
          path: 'sales',
          component: () => import('../views/sale/SaleListView.vue'),
          meta: { title: '销售记录', permission: 'sale.order' },
        },
        {
          path: 'sales/:id',
          component: () => import('../views/sale/SaleDetailView.vue'),
          meta: { title: '销售单详情', permission: 'sale.order' },
        },
        {
          path: 'inventory/products',
          component: () => import('../views/inventory/StockInventoryView.vue'),
          meta: { title: '当前库存', permission: 'inventory.query', roles: ['ADMIN', 'PURCHASER'] },
        },
        {
          path: 'inventory/products/:id/batches',
          component: () => import('../views/inventory/StockBatchView.vue'),
          meta: { title: '批次详情', permission: 'inventory.batch' },
        },
        {
          path: 'inventory/movements',
          component: () => import('../views/inventory/StockMovementView.vue'),
          meta: { title: '库存流水', permission: 'inventory.movement' },
        },
        {
          path: 'purchases',
          component: () => import('../views/purchase/PurchaseListView.vue'),
          meta: { title: '采购单管理', permission: 'purchase.order' },
        },
        {
          path: 'purchases/review',
          component: () => import('../views/purchase/PurchaseListView.vue'),
          props: { review: true },
          meta: { title: '入库审核', permission: 'purchase.order.review' },
        },
        {
          path: 'purchases/summary',
          component: () => import('../views/purchase/PurchaseSummaryView.vue'),
          meta: { title: '供应商核对汇总', permission: 'purchase.order.reconcile' },
        },
        {
          path: 'purchases/new',
          component: () => import('../views/purchase/PurchaseEditorView.vue'),
          meta: { title: '新建采购', permission: 'purchase.order.create' },
        },
        {
          path: 'purchases/:id/edit',
          component: () => import('../views/purchase/PurchaseEditorView.vue'),
          meta: { title: '编辑采购与到货登记', permission: 'purchase.order.edit' },
        },
        {
          path: 'purchases/:id',
          component: () => import('../views/purchase/PurchaseDetailView.vue'),
          meta: { title: '采购单详情', permission: 'purchase.order' },
        },
        {
          path: 'inventory/saleable-products',
          component: () => import('../views/inventory/SaleableInventoryView.vue'),
          meta: { title: '库存查询', permission: 'inventory.query' },
        },
        {
          path: '',
          name: 'dashboard',
          component: () => import('../views/home/DashboardView.vue'),
          meta: { title: '工作台', permission: 'common.dashboard' },
        },
        {
          path: 'profile',
          component: () => import('../views/profile/ProfileView.vue'),
          meta: { title: '个人中心', permission: 'common.profile' },
        },
        {
          path: 'catalog/categories',
          component: () => import('../views/catalog/CategoryView.vue'),
          meta: { title: '商品分类', permission: 'base.category' },
        },
        {
          path: 'catalog/products',
          component: () => import('../views/catalog/ProductView.vue'),
          meta: { title: '商品档案', permission: 'base.product' },
        },
        {
          path: 'catalog/suppliers',
          component: () => import('../views/catalog/SupplierView.vue'),
          meta: { title: '供应商管理', permission: 'base.supplier' },
        },
        {
          path: 'forbidden',
          component: () => import('../views/home/ForbiddenView.vue'),
          meta: { title: '访问受限' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
router.beforeEach(async (to) => {
  const auth = useAuth()
  if (to.path === '/login') return auth.loggedIn ? '/' : true
  if (!auth.loggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.path === '/session-unavailable') return true
  if (!auth.user) {
    try {
      await auth.load()
    } catch {
      return {
        path: auth.loggedIn ? '/session-unavailable' : '/login',
        query: { redirect: to.fullPath },
      }
    }
  }
  const permission = to.meta.permission
  if (
    Array.isArray(to.meta.anyPermission) &&
    !to.meta.anyPermission.some((p) => typeof p === 'string' && auth.can(p))
  )
    return '/forbidden'
  if (typeof permission === 'string' && !auth.can(permission)) return '/forbidden'
  if (Array.isArray(to.meta.roles) && !to.meta.roles.includes(auth.user?.roleCode))
    return '/forbidden'
})
router.onError((_error, to) => {
  navigationFailure.value = {
    target: to.fullPath,
    title: typeof to.meta.title === 'string' ? to.meta.title : '目标页面',
  }
})
router.afterEach((_to, _from, failure) => {
  if (!failure) navigationFailure.value = null
})
