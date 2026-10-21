import { reactive, computed } from 'vue'

const TOKEN_KEY = 'sems_token'
const USER_KEY = 'sems_user'

function load() {
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    const raw = localStorage.getItem(USER_KEY)
    const user = raw ? JSON.parse(raw) : null
    return { token, user }
  } catch {
    return { token: null, user: null }
  }
}

const initial = load()

export const authState = reactive({
  token: initial.token,
  user: initial.user,
})

export const isAuthenticated = computed(() => !!authState.token && !!authState.user)
export const currentRole = computed(() => authState.user?.role || null)
export const currentUser = computed(() => authState.user)

export function setAuth(token, user) {
  authState.token = token
  authState.user = user
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function logout() {
  authState.token = null
  authState.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

// Where to land after login, by role.
export const ROLE_HOME = {
  ADMIN: '/admin/venues',
  TEACHER: '/approvals',
  STUDENT: '/venues',
}

export function homeForRole(role) {
  return ROLE_HOME[role] || '/venues'
}
