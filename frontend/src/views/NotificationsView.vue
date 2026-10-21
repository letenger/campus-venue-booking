<template>
  <div class="page">
    <div class="toolbar">
      <span class="muted">{{ unread }} unread of {{ rows.length }}</span>
      <span class="spacer" />
      <button class="btn btn-sm" :disabled="!hasUnread" @click="readAll">Mark all read</button>
    </div>

    <div class="card" v-loading="loading">
      <button
        v-for="n in rows"
        :key="n.id"
        class="notif"
        :class="{ unread: !n.read }"
        @click="read(n)"
      >
        <span class="kind" :class="kindClass(n.type)">{{ kindLabel(n.type) }}</span>
        <span class="body">
          <strong>{{ n.title }}</strong>
          <span class="muted">{{ n.content }}</span>
        </span>
        <span class="time muted tabular">{{ relative(n.createdAt) }}</span>
        <span v-if="!n.read" class="dot" />
      </button>

      <div v-if="!loading && !rows.length" class="empty">
        <strong>Nothing yet</strong>
        <span>Approvals, rejections and no-shows show up here.</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listNotifications, markNotificationRead, markAllNotificationsRead,
} from '../api'

const rows = ref([])
const loading = ref(false)

const unread = computed(() => rows.value.filter((n) => !n.read).length)
const hasUnread = computed(() => unread.value > 0)

const KINDS = {
  BOOKING_CREATED: { label: 'Submitted', cls: 'k-blue' },
  APPROVAL_REQUIRED: { label: 'Needs you', cls: 'k-amber' },
  APPROVED: { label: 'Approved', cls: 'k-green' },
  REJECTED: { label: 'Rejected', cls: 'k-rose' },
  CANCELLED: { label: 'Cancelled', cls: 'k-slate' },
  NO_SHOW: { label: 'No-show', cls: 'k-rose' },
}

const kindLabel = (type) => KINDS[type]?.label || 'Update'
const kindClass = (type) => KINDS[type]?.cls || 'k-slate'

function relative(value) {
  if (!value) return ''
  const then = new Date(value).getTime()
  const mins = Math.round((Date.now() - then) / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins} min ago`
  const hours = Math.round(mins / 60)
  if (hours < 24) return `${hours} h ago`
  const days = Math.round(hours / 24)
  return days === 1 ? 'yesterday' : `${days} days ago`
}

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
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
}

.notif {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 90px 12px;
  gap: 14px;
  align-items: center;
  width: 100%;
  padding: 14px 22px;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: transparent;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.notif:last-of-type {
  border-bottom: 0;
}

.notif:hover {
  background: var(--surface-2);
}

.notif.unread {
  background: #fbfcff;
}

.kind {
  justify-self: start;
  padding: 3px 9px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.02em;
}

.k-blue { background: var(--primary-soft); color: var(--primary-ink); }
.k-green { background: var(--green-soft); color: var(--green); }
.k-amber { background: var(--amber-soft); color: var(--amber); }
.k-rose { background: var(--rose-soft); color: var(--rose); }
.k-slate { background: var(--slate-soft); color: var(--slate); }

.body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.body strong {
  font-size: 13.5px;
  font-weight: 600;
}

.body .muted {
  font-size: 12.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  font-size: 11.5px;
  text-align: right;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--primary);
}

@media (max-width: 720px) {
  .notif {
    grid-template-columns: auto minmax(0, 1fr) 12px;
    gap: 10px;
    padding: 14px 16px;
  }

  .time {
    display: none;
  }
}
</style>
