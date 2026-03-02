import { createRouter, createWebHistory } from 'vue-router'
import CustomerView from '../views/CustomerView.vue'
import AdminView from '../views/AdminView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import { hasAdminToken } from '../utils/auth'

const routes = [
  {
    path: '/',
    name: 'customer',
    component: CustomerView,
  },
  {
    path: '/admin',
    name: 'admin',
    component: AdminView,
    meta: { requiresAdminAuth: true },
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
    return { name: 'admin' }
  }
  return true
})

export default router
