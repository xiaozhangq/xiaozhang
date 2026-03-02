<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminLogin } from '../api'
import { setAdminToken } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
})

async function submit() {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码')
    return
  }

  submitting.value = true
  try {
    const { data } = await adminLogin({
      username: form.username,
      password: form.password,
    })
    setAdminToken(data.token)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin'
    await router.replace(redirect)
  } catch (error) {
    alert(error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h2>后台登录</h2>
      <p class="tip">请输入管理员账号密码</p>
      <input v-model="form.username" placeholder="用户名" />
      <input
        v-model="form.password"
        type="password"
        placeholder="密码"
        @keyup.enter="submit"
      />
      <button :disabled="submitting" @click="submit">
        {{ submitting ? '登录中...' : '登录' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 360px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.login-card h2 {
  margin: 0;
}

.tip {
  margin: 0;
  color: #6b7280;
}

.login-card input {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px;
  font: inherit;
}
</style>
