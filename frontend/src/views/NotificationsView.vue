<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">Notifications</h2>
      <el-button type="primary" plain :icon="Check" :disabled="!hasUnread" @click="readAll">Mark all read</el-button>
    </div>

    <el-timeline v-loading="loading">
      <el-timeline-item v-for="n in rows" :key="n.id" :timestamp="formatDateTime(n.createdAt)"
        :type="n.read ? 'info' : 'primary'" :hollow="n.read">
        <div class="notif" :class="{ unread: !n.read }" @click="read(n)">
          <div class="notif-title">
            {{ n.title }}
            <el-tag v-if="!n.read" size="small" type="danger" effect="dark">unread</el-tag>
          </div>
          <div class="notif-content">{{ n.content }}</div>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="!loading && rows.length === 0" description="Nothing here yet" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { listNotifications, markNotificationRead, markAllNotificationsRead } from '../api'
import { formatDateTime } from '../utils/format'

const rows = ref([])
const loading = ref(false)
const hasUnread = computed(() => rows.value.some((n) => !n.read))

async function load() {
  loading.value = true
  try {
    rows.value = (await listNotifications()) || []
  } finally {
    loading.value = false
  }
}
async function read(n) {
  if (n.read) return
  await markNotificationRead(n.id)
  n.read = true
}
async function readAll() {
  await markAllNotificationsRead()
  ElMessage.success('All marked as read')
  load()
}

onMounted(load)
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  margin: 0;
  font-size: 20px;
}
.notif {
  cursor: pointer;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebedf0;
}
.notif.unread {
  background: #f3f8ff;
  border-color: #cfe0ff;
}
.notif-title {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}
.notif-content {
  color: #646a73;
  font-size: 13px;
  margin-top: 4px;
}
</style>
