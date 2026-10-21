<template>
  <el-menu :default-active="active" router class="app-menu">
    <div class="brand">校园体育场馆预定</div>
    <el-menu-item v-for="item in items" :key="item.path" :index="item.path">
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { authState } from '../store/auth'

const route = useRoute()
const active = computed(() => '/' + (route.path.split('/')[1] || 'venues'))

// Mirror of the router table, filtered by the current role.
const ALL = [
  { path: '/venues', label: '场馆一览', icon: 'House', roles: null },
  { path: '/bookings', label: '我的预订', icon: 'Tickets', roles: null },
  { path: '/approvals', label: '预订审批', icon: 'Stamp', roles: ['TEACHER', 'ADMIN'] },
  { path: '/admin/venues', label: '场馆管理', icon: 'OfficeBuilding', roles: ['ADMIN'] },
  { path: '/admin/users', label: '用户管理', icon: 'User', roles: ['ADMIN'] },
  { path: '/reports', label: '使用报表', icon: 'DataLine', roles: ['TEACHER', 'ADMIN'] },
  { path: '/notifications', label: '通知中心', icon: 'Bell', roles: null },
]

const role = computed(() => authState.user?.role)
const items = computed(() =>
  ALL.filter((m) => !m.roles || m.roles.includes(role.value)),
)
</script>

<style scoped>
.app-menu {
  border-right: none;
  padding: 8px;
  height: 100%;
}
.brand {
  font-size: 15px;
  font-weight: 600;
  color: #2f6fed;
  padding: 14px 12px 18px;
  white-space: nowrap;
}
.app-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin-bottom: 4px;
  height: 44px;
}
</style>
