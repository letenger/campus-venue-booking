<template>
  <header class="app-header">
    <span class="app-page-title">{{ title }}</span>
    <div class="header-right">
      <el-badge :value="unread" :hidden="unread === 0" class="bell">
        <el-icon class="bell-icon" @click="goNotifications"><Bell /></el-icon>
      </el-badge>
      <span class="user-chip">
        <span class="avatar">{{ initial }}</span>
        {{ roleLabel(user?.role) }} · {{ user?.realName || user?.username }}
      </span>
      <span class="logout-link" @click="onLogout">退出登录</span>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { authState, logout } from '../store/auth'
import { roleLabel } from '../utils/format'
import { listNotifications } from '../api'

const router = useRouter()
const route = useRoute()
const user = computed(() => authState.user)
const initial = computed(() => (user.value?.realName || user.value?.username || '?').slice(0, 1))
const title = computed(() => route.meta.title || '')

const unread = ref(0)
async function refreshUnread() {
  try {
    const list = await listNotifications()
    unread.value = (list || []).filter((n) => !n.read).length
  } catch {
    unread.value = 0
  }
}
onMounted(refreshUnread)

function goNotifications() {
  router.push('/notifications')
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  height: 54px;
  background: #fff;
  border-bottom: 1px solid #ebedf0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
}
.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 16px;
}
.app-page-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f2329;
}
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f1f2f3;
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12px;
  color: #646a73;
}
.avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e6f1fb;
  color: #2f6fed;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}
.logout-link {
  font-size: 13px;
  color: #2f6fed;
  cursor: pointer;
}
.bell-icon {
  font-size: 20px;
  color: #646a73;
  cursor: pointer;
}
</style>
