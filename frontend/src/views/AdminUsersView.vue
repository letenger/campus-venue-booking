<template>
  <div class="page">
    <div class="toolbar">
      <div class="segmented">
        <button
          v-for="tab in roleTabs"
          :key="tab.value"
          class="seg"
          :class="{ active: role === tab.value }"
          @click="pickRole(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <span class="spacer" />
      <span class="muted">{{ total }} accounts</span>
      <button class="btn btn-sm" @click="load">
        <el-icon><Refresh /></el-icon> Refresh
      </button>
    </div>

    <div class="card" v-loading="loading">
      <div class="thead">
        <span>Person</span>
        <span>Username</span>
        <span>Role</span>
        <span>Class</span>
        <span>Status</span>
        <span class="right">Actions</span>
      </div>

      <div v-for="row in rows" :key="row.id" class="trow">
        <div class="person">
          <span class="avatar">{{ (row.realName || row.username).slice(0, 1).toUpperCase() }}</span>
          <div class="who">
            <strong>{{ row.realName || '—' }}</strong>
            <span class="muted">{{ row.studentNo || row.phone || '—' }}</span>
          </div>
        </div>
        <div class="mono">{{ row.username }}</div>
        <div><span class="chip">{{ roleLabel(row.role) }}</span></div>
        <div class="muted">{{ row.classId ? `Class ${row.classId}` : '—' }}</div>
        <div>
          <span class="pill" :class="userStatusOf(row.status).pill">{{ userStatusOf(row.status).label }}</span>
        </div>
        <div class="actions">
          <button
            class="btn btn-sm"
            :class="{ 'btn-ghost': row.status !== 'ACTIVE' }"
            @click="setStatus(row, row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')"
          >
            {{ row.status === 'ACTIVE' ? 'Disable' : 'Enable' }}
          </button>
        </div>
      </div>

      <div v-if="!loading && !rows.length" class="empty">
        <strong>No accounts match</strong>
        <span>Try another role filter.</span>
      </div>
    </div>

    <div v-if="total > size" class="pager">
      <button class="btn btn-sm" :disabled="page === 1" @click="go(page - 1)">Previous</button>
      <span class="muted tabular">Page {{ page }} of {{ pages }}</span>
      <button class="btn btn-sm" :disabled="page === pages" @click="go(page + 1)">Next</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listUsers, setUserStatus } from '../api'
import { roleLabel, userStatusOf } from '../utils/format'

const rows = ref([])
const loading = ref(false)
const role = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const roleTabs = [
  { label: 'Everyone', value: '' },
  { label: 'Students', value: 'STUDENT' },
  { label: 'Teachers', value: 'TEACHER' },
  { label: 'Admins', value: 'ADMIN' },
]

const pages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

async function load() {
  loading.value = true
  try {
    const data = await listUsers({
      role: role.value || undefined,
      page: page.value - 1,
      size: size.value,
    })
    rows.value = data.content || []
    total.value = data.totalElements || 0
  } finally {
    loading.value = false
  }
}

function pickRole(value) {
  role.value = value
  page.value = 1
  load()
}

function go(p) {
  page.value = p
  load()
}

async function setStatus(row, status) {
  const disabling = status === 'DISABLED'
  try {
    await ElMessageBox.confirm(
      disabling
        ? 'A disabled account cannot sign in. Bookings already made are kept.'
        : 'This person will be able to sign in again.',
      `${disabling ? 'Disable' : 'Enable'} ${row.realName || row.username}?`,
      { confirmButtonText: disabling ? 'Disable' : 'Enable', cancelButtonText: 'Back', type: 'warning' },
    )
  } catch {
    return
  }
  await setUserStatus(row.id, status)
  ElMessage.success('Updated')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12.5px;
}

.segmented {
  display: inline-flex;
  padding: 3px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--surface);
}

.seg {
  height: 30px;
  padding: 0 14px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 13px;
  font-weight: 550;
  cursor: pointer;
}

.seg.active {
  background: var(--primary);
  color: #fff;
}

.thead,
.trow {
  display: grid;
  grid-template-columns: minmax(180px, 1.4fr) 130px 100px 100px 110px 110px;
  gap: 14px;
  align-items: center;
  padding: 11px 22px;
}

.thead {
  border-bottom: 1px solid var(--line);
  background: var(--surface-2);
  border-radius: var(--radius) var(--radius) 0 0;
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--muted);
}

.trow {
  border-bottom: 1px solid var(--line);
  font-size: 13.5px;
}

.trow:last-of-type {
  border-bottom: 0;
}

.trow:hover {
  background: var(--surface-2);
}

.person {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 999px;
  background: var(--slate-soft);
  color: var(--ink-2);
  font-size: 12.5px;
  font-weight: 700;
}

.who {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
}

.who strong {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.who .muted {
  font-size: 11.5px;
}

.mono {
  font-variant-numeric: tabular-nums;
  color: var(--ink-2);
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.right {
  text-align: right;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  font-size: 12.5px;
}

@media (max-width: 900px) {
  .thead {
    display: none;
  }

  .trow {
    grid-template-columns: 1fr auto;
    gap: 8px 12px;
    padding: 14px 16px;
  }

  .trow > div:nth-child(2),
  .trow > div:nth-child(4) {
    display: none;
  }

  .trow > div:nth-child(3) {
    grid-column: 1;
  }

  .actions {
    grid-column: 2;
    grid-row: 1 / span 2;
    align-items: center;
  }
}
</style>
