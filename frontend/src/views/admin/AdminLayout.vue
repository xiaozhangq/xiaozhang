<script setup>
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAdminProfile } from '../../api'
import { clearAdminToken } from '../../utils/auth'

const adminName = ref('')
const router = useRouter()
const route = useRoute()

onMounted(async () => {
  document.title = '后台管理'
  try {
    const { data } = await getAdminProfile()
    adminName.value = data.username
  } catch (e) {
    if (e.status === 401 || e.status === 403) {
      clearAdminToken()
      router.replace('/admin/login')
    }
  }
})

async function logout() {
  clearAdminToken()
  alert('已退出登录')
  await router.replace('/admin/login')
}

function navTo(name) {
  router.push({ name })
}
</script>

<template>
  <div class="admin-layout">
    <header class="admin-header">
      <div class="admin-title">
        <strong>后台管理</strong>
        <span class="admin-user">当前：{{ adminName || '-' }}</span>
      </div>
      <nav class="admin-nav">
        <button
          class="nav-btn"
          :class="{ active: route.name === 'admin-categories' }"
          @click="navTo('admin-categories')"
        >
          分类管理
        </button>
        <button
          class="nav-btn"
          :class="{ active: route.name === 'admin-orders' }"
          @click="navTo('admin-orders')"
        >
          订单管理
        </button>
        <button
          class="nav-btn"
          :class="{ active: route.name === 'admin-menu' }"
          @click="navTo('admin-menu')"
        >
          菜单管理
        </button>
        <button
          class="nav-btn"
          :class="{ active: route.name === 'admin-customers' }"
          @click="navTo('admin-customers')"
        >
          用户审核
        </button>
      </nav>
      <button class="btn-logout" @click="logout">退出登录</button>
    </header>
    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f5f5f5;
}

.admin-header {
  background: #fff;
  padding: 12px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.admin-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-user {
  font-size: 14px;
  color: #666;
}

.admin-nav {
  display: flex;
  gap: 8px;
}

.nav-btn {
  padding: 8px 16px;
  border: 1px solid #94a3b8;
  background: #f1f5f9;
  color: #334155;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.nav-btn:hover {
  background: #e2e8f0;
  border-color: #64748b;
}

.nav-btn.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.btn-logout {
  margin-left: auto;
  padding: 8px 16px;
  background: #f1f5f9;
  border: 1px solid #94a3b8;
  color: #334155;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.btn-logout:hover {
  background: #e2e8f0;
}

.admin-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
</style>
