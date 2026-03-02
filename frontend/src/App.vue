<template>
  <div class="app-shell">
    <main class="app-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f5f5f5;
}

.app-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
  width: 100%;
  overflow-x: hidden;
}

@media (max-width: 600px) {
  .app-shell {
    min-height: 100vh;
    min-height: 100dvh;
  }

  .app-content {
    max-width: 100%;
    min-height: 100vh;
    min-height: 100dvh;
  }
}
</style>
