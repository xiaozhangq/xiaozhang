import { createRouter, createWebHistory } from 'vue-router'
import CustomerView from '../views/CustomerView.vue'
import CustomerLoginView from '../views/CustomerLoginView.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminCategoriesView from '../views/admin/AdminCategoriesView.vue'
import AdminOrdersView from '../views/admin/AdminOrdersView.vue'
import AdminMenuView from '../views/admin/AdminMenuView.vue'
import AdminCustomersView from '../views/admin/AdminCustomersView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import { hasAdminToken } from '../utils/auth'

const routes = [
  {
    path: '/',
    name: 'customer',
    component: CustomerView,
  },
  {
    path: '/login',
    name: 'customer-login',
    component: CustomerLoginView,
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAdminAuth: true },
    children: [
      { path: '', redirect: { name: 'admin-orders' } },
      { path: 'categories', name: 'admin-categories', component: AdminCategoriesView },
      { path: 'orders', name: 'admin-orders', component: AdminOrdersView },
      { path: 'menu', name: 'admin-menu', component: AdminMenuView },
      { path: 'customers', name: 'admin-customers', component: AdminCustomersView },
    ],
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: AdminLoginView,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const loggedIn = hasAdminToken()
  if (to.meta.requiresAdminAuth && !loggedIn) {
    return {
      name: 'admin-login',
      query: { redirect: to.fullPath },
    }
  }
  if (to.name === 'admin-login' && loggedIn) {
    return { name: 'admin-orders' }
  }
  return true
})

export default router
