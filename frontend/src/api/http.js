import axios from 'axios'
import { clearAdminToken, clearCustomerToken, getAdminToken, getCustomerToken } from '../utils/auth'

// 未配置时用空字符串（同源），开发时配合 vite proxy；部署到服务器与后端同域时无需配置
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const url = (config.url || '').toString()
  const fullUrl = (config.baseURL || '') + url
  const isAdminApi = url.startsWith('/api/admin') || fullUrl.includes('/api/admin')
  const isCustomerApi = url.startsWith('/api/customer') || fullUrl.includes('/api/customer')
  const isLoginApi = url.includes('/api/admin/auth/login') || fullUrl.includes('/api/admin/auth/login')
  if (isAdminApi && !isLoginApi) {
    const token = getAdminToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  if (isCustomerApi) {
    const token = getCustomerToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const requestUrl = error.config?.url || ''
    const isAdminApi = requestUrl.startsWith('/api/admin')
    const isCustomerApi = requestUrl.startsWith('/api/customer')
    if (isAdminApi && status === 401) {
      clearAdminToken()
    }
    if (isCustomerApi && (status === 401 || status === 403)) {
      clearCustomerToken()
    }

    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      '请求失败'
    const normalizedError = new Error(message)
    normalizedError.status = status
    return Promise.reject(normalizedError)
  }
)

export default http
