import axios from 'axios'
import { ElMessage } from 'element-plus'
import { authState, logout } from '../store/auth'

const http = axios.create({
  // Same origin in dev (Vite proxies /api) and in the split deployment the build
  // points at the API host with VITE_API_BASE.
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  if (authState.token) {
    config.headers.Authorization = `Bearer ${authState.token}`
  }
  return config
})

// The API always answers with { success, data } or { success: false, error }.
// Callers work with the inner data and never see the envelope.
http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.success === false) {
      ElMessage.error(body.error?.message || 'Request failed')
      return Promise.reject(body.error)
    }
    return body ? body.data : body
  },
  (error) => {
    const status = error?.response?.status
    const apiError = error?.response?.data?.error
    const isLoginCall = (error?.config?.url || '').includes('/auth/login')

    // A 401 anywhere but the login form means the token is gone or expired.
    if (status === 401 && !isLoginCall) {
      logout()
      ElMessage.error('Your session expired, please sign in again')
      if (location.hash.startsWith('#/')) {
        location.hash = '#/login'
      } else if (!location.pathname.endsWith('/login')) {
        location.href = '/login'
      }
      return Promise.reject(error)
    }

    const details = apiError?.details
    ElMessage.error(details?.length ? details.join('; ') : apiError?.message || 'Request failed')
    return Promise.reject(error)
  },
)

export default http
