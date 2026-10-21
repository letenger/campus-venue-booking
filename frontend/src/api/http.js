import axios from 'axios'
import { ElMessage } from 'element-plus'
import { authState, logout } from '../store/auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  if (authState.token) {
    config.headers['Authorization'] = `Bearer ${authState.token}`
  }
  return config
})

// Unwrap the uniform ApiResponse envelope ({ success, data, message }) and
// surface the inner `data` to callers. On failure, show a message and reject.
http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.success === false) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return body ? body.data : body
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      // token missing/expired -> force re-login
      logout()
      if (location.hash !== '#/login' && location.pathname !== '/login') {
        ElMessage.error('登录已失效，请重新登录')
        location.href = '/login'
      }
    } else {
      const detail = error?.response?.data?.message
      const message = typeof detail === 'string' ? detail : error.message || '请求失败'
      if (status !== 403) {
        ElMessage.error(message)
      }
    }
    return Promise.reject(error)
  },
)

export default http
