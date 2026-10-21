<template>
  <div class="menu">
    <div class="brand">
      <span class="mark">CV</span>
      <span class="brand-text">
        <strong>Campus Venue</strong>
        <small>Booking system</small>
      </span>
    </div>

    <nav>
      <template v-for="group in groups" :key="group.label">
        <p class="group-label">{{ group.label }}</p>
        <router-link
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          class="item"
          :class="{ active: isActive(item.path) }"
          @click="$emit('navigate')"
        >
          <el-icon class="item-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </template>
    </nav>

    <div class="footer">
      <p class="muted">Signed in as</p>
      <p class="who">{{ user?.realName || user?.username }}</p>
      <p class="muted small">{{ roleLabel(user?.role) }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  House, Tickets, Stamp, OfficeBuilding, User, DataLine, Bell,
} from '@element-plus/icons-vue'
import { authState } from '../store/auth'
import { roleLabel } from '../utils/format'

defineEmits(['navigate'])

const route = useRoute()
const user = computed(() => authState.user)
const role = computed(() => authState.user?.role)
const isStaff = computed(() => ['TEACHER', 'ADMIN'].includes(role.value))

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')

const groups = computed(() => {
  const list = [
    {
      label: 'Book',
      items: [
        { path: '/venues', label: 'Venues', icon: House },
        { path: '/bookings', label: 'My bookings', icon: Tickets },
      ],
    },
  ]
  if (isStaff.value) {
    list.push({
      label: 'Staff',
      items: [
        { path: '/approvals', label: 'Approvals', icon: Stamp },
        { path: '/reports', label: 'Reports', icon: DataLine },
      ],
    })
  }
  if (role.value === 'ADMIN') {
    list.push({
      label: 'Admin',
      items: [
        { path: '/admin/venues', label: 'Manage venues', icon: OfficeBuilding },
        { path: '/admin/users', label: 'Users', icon: User },
      ],
    })
  }
  list.push({
    label: 'Account',
    items: [{ path: '/notifications', label: 'Notifications', icon: Bell }],
  })
  return list
})
</script>

<style scoped>
.menu {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 18px 12px 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 8px 18px;
}

.mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--primary);
  color: #fff;
  font-size: 12.5px;
  font-weight: 700;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand-text strong {
  font-size: 14px;
  font-weight: 650;
}

.brand-text small {
  font-size: 11.5px;
  color: var(--muted);
}

.group-label {
  margin: 14px 8px 6px;
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--faint);
}

.item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 38px;
  padding: 0 10px;
  border-radius: 9px;
  color: var(--ink-2);
  font-size: 13.5px;
  font-weight: 500;
  transition: background 0.12s, color 0.12s;
}

.item:hover {
  background: var(--surface-2);
  color: var(--ink);
}

.item.active {
  background: var(--primary-soft);
  color: var(--primary-ink);
  font-weight: 600;
}

.item-icon {
  font-size: 16px;
  opacity: 0.85;
}

.footer {
  margin-top: auto;
  padding: 14px 10px 4px;
  border-top: 1px solid var(--line);
  line-height: 1.35;
}

.footer p {
  margin: 0;
  font-size: 12px;
}

.who {
  font-size: 13px !important;
  font-weight: 600;
  color: var(--ink);
}

.small {
  font-size: 11.5px !important;
}
</style>
