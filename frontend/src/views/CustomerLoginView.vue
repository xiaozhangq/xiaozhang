<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customerLogin, customerRegister } from '../api'
import { setCustomerToken } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const tab = ref('login')
const submitting = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', phone: '' })

onMounted(() => {
  document.title = '登录 - 点餐'
})

async function doLogin() {
  if (!loginForm.username || !loginForm.password) {
    alert('请输入用户名和密码')
    return
  }
  submitting.value = true
  try {
    const { data } = await customerLogin({
      username: loginForm.username,
      password: loginForm.password,
    })
    setCustomerToken(data.token)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    alert(e.message)
  } finally {
    submitting.value = false
  }
}

async function doRegister() {
  if (!registerForm.username || !registerForm.password) {
    alert('请输入用户名和密码')
    return
  }
  if (registerForm.username.length < 2 || registerForm.username.length > 32) {
    alert('用户名2-32位')
    return
  }
  if (registerForm.password.length < 6 || registerForm.password.length > 64) {
    alert('密码6-64位')
    return
  }
  submitting.value = true
  try {
    await customerRegister({
      username: registerForm.username.trim(),
      password: registerForm.password,
      phone: registerForm.phone?.trim() || undefined,
    })
    alert('注册成功，请等待管理员审核通过后再登录')
    tab.value = 'login'
    loginForm.username = registerForm.username
  } catch (e) {
    alert(e.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="customer-login-page">
    <div class="customer-login-card">
      <h2>点餐用户</h2>
      <div class="tabs">
        <button type="button" :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</button>
        <button type="button" :class="{ active: tab === 'register' }" @click="tab = 'register'">注册</button>
      </div>

      <div v-show="tab === 'login'" class="form-panel">
        <input v-model="loginForm.username" placeholder="用户名" />
        <input v-model="loginForm.password" type="password" placeholder="密码" @keyup.enter="doLogin" />
        <button type="button" :disabled="submitting" class="btn-primary" @click="doLogin">
          {{ submitting ? '登录中...' : '登录' }}
        </button>
      </div>

      <div v-show="tab === 'register'" class="form-panel">
        <input v-model="registerForm.username" placeholder="用户名（2-32位）" />
        <input v-model="registerForm.password" type="password" placeholder="密码（6-64位）" />
        <input v-model="registerForm.phone" placeholder="手机号（选填）" />
        <button type="button" :disabled="submitting" class="btn-primary" @click="doRegister">
          {{ submitting ? '提交中...' : '注册' }}
        </button>
        <p class="register-tip">注册后需管理员审核通过方可登录</p>
      </div>

      <router-link to="/" class="back-home">返回首页</router-link>
    </div>
  </div>
</template>

<style scoped>
.customer-login-page {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.customer-login-card {
  width: 360px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.customer-login-card h2 {
  margin: 0 0 16px 0;
  font-size: 20px;
  color: #333;
  text-align: center;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tabs button {
  flex: 1;
  padding: 8px;
  border: 1px solid #e0e0e0;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
}

.tabs button.active {
  background: #ffc107;
  border-color: #ffc107;
  color: #333;
}

.form-panel input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
}

.form-panel input:focus {
  outline: none;
  border-color: #ffc107;
}

.btn-primary {
  width: 100%;
  padding: 10px;
  margin-top: 4px;
  font-size: 14px;
  background: #ffc107;
  color: #333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary:hover:not(:disabled) {
  background: #e6ac00;
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.register-tip {
  margin: 10px 0 0 0;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.back-home {
  display: block;
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #666;
  text-decoration: none;
}

.back-home:hover {
  color: #ffc107;
}
</style>
