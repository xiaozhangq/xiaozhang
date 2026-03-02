import axios from 'axios'
import { clearAdminToken, getAdminToken } from '../utils/auth'

// 未配置时用空字符串（同源），开发时配合 vite proxy；部署到服务器与后端同域时无需配置
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const url = config.url || ''
  const isAdminApi = url.startsWith('/api/admin')
  const isLoginApi = url === '/api/admin/auth/login'
  if (isAdminApi && !isLoginApi) {
    const token = getAdminToken()
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
    if (isAdminApi && (status === 401 || status === 403)) {
      clearAdminToken()
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
