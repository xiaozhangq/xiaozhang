<template>
  <div class="app-shell">
    <header class="app-header">
      <h1>SpringBoot + Vue3 点餐系统</h1>
      <nav class="nav-tabs">
        <RouterLink to="/" class="tab">前台点餐</RouterLink>
        <RouterLink to="/admin" class="tab">后台管理</RouterLink>
      </nav>
    </header>
    <main class="app-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.app-header h1 {
  margin: 0;
  font-size: 20px;
}

.nav-tabs {
  display: flex;
  gap: 10px;
}

.tab {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  color: #374151;
}

.tab.router-link-active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.app-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
</style>
