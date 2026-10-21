<template>
  <header class="topbar">
    <button class="icon-btn menu-btn" aria-label="Open menu" @click="$emit('open-menu')">
      <el-icon><Expand /></el-icon>
    </button>

    <div class="heading">
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="muted">{{ subtitle }}</p>
    </div>

    <div class="actions">
      <button class="icon-btn" aria-label="Notifications" @click="router.push('/notifications')">
        <el-icon><Bell /></el-icon>
        <span v-if="unread" class="dot">{{ unread > 9 ? '9+' : unread }}</span>
      </button>

      <el-dropdown trigger="click" @command="onCommand">
        <button class="account">
          <span class="avatar">{{ initial }}</span>
          <span class="who">
            <strong>{{ user?.realName || user?.username }}</strong>
            <small>{{ roleLabel(user?.role) }}</small>
          </span>
          <el-icon class="caret"><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="notifications">Notifications</el-dropdown-item>
            <el-dropdown-item command="signout" divided>Sign out</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Bell, ArrowDown, Expand } from '@element-plus/icons-vue'
import { authState, logout } from '../store/auth'
import { unreadCount, loadNotifications, resetNotifications } from '../store/notifications'
import { roleLabel } from '../utils/format'

defineEmits(['open-menu'])

const route = useRoute()
const router = useRouter()
const user = computed(() => authState.user)
const initial = computed(() => (user.value?.realName || user.value?.username || '?').slice(0, 1).toUpperCase())
const title = computed(() => route.meta.title || 'Campus Venue')
const subtitle = computed(() => route.meta.subtitle || '')

const unread = unreadCount

// Reload the inbox when the account changes, and again on every navigation so the
// badge reflects anything that happened elsewhere (an approval, a rejection).
watch(
  () => authState.user?.id,
  (id) => {
    if (id) loadNotifications()
    else resetNotifications()
  },
  { immediate: true },
)

watch(
  () => route.path,
  (path) => {
    if (authState.user?.id && !path.startsWith('/notifications')) loadNotifications()
  },
)

async function onCommand(command) {
  if (command === 'notifications') {
    router.push('/notifications')
    return
  }
  try {
    await ElMessageBox.confirm('Sign out of this device?', 'Sign out', {
      confirmButtonText: 'Sign out',
      cancelButtonText: 'Stay',
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
.topbar {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 66px;
  padding: 0 30px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}

.heading h1 {
  font-size: 17px;
  font-weight: 650;
}

.heading p {
  margin: 1px 0 0;
  font-size: 12.5px;
}

.actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  position: relative;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: var(--ink-2);
  font-size: 17px;
  cursor: pointer;
}

.icon-btn:hover {
  background: var(--slate-soft);
}

.menu-btn {
  display: none;
}

.dot {
  position: absolute;
  top: 3px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--rose);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.account {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 40px;
  padding: 0 10px 0 6px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  cursor: pointer;
}

.account:hover {
  background: var(--surface-2);
}

.avatar {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-ink);
  font-size: 12.5px;
  font-weight: 700;
}

.who {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.who strong {
  font-size: 13px;
  font-weight: 600;
}

.who small {
  font-size: 11px;
  color: var(--muted);
}

.caret {
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 960px) {
  .topbar {
    padding: 0 16px;
    height: 60px;
  }

  .menu-btn {
    display: grid;
  }

  .who {
    display: none;
  }
}
</style>
