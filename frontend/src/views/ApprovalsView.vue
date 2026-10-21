<template>
  <div class="page">
    <div class="summary">
      <div class="tiles">
        <div class="tile">
          <span class="tile-label">Waiting</span>
          <strong>{{ pendingCount }}</strong>
        </div>
        <div class="tile">
          <span class="tile-label">Decided today</span>
          <strong>{{ decidedToday }}</strong>
        </div>
        <div class="tile">
          <span class="tile-label">Oldest request</span>
          <strong class="small-value">{{ oldest || '—' }}</strong>
        </div>
      </div>
      <button class="btn" @click="load">
        <el-icon><Refresh /></el-icon> Refresh
      </button>
    </div>

    <div v-loading="loading" class="list">
      <article v-for="row in rows" :key="row.id" class="req card">
        <div class="who">
          <span class="avatar">{{ (row.userName || 'U').slice(0, 1).toUpperCase() }}</span>
          <div>
            <strong>{{ row.userName || `User #${row.userId}` }}</strong>
            <span class="muted">{{ purposeLabel(row.purpose) }} · {{ row.attendeesCount }} people</span>
          </div>
        </div>

        <div class="what">
          <p class="venue-line">
            <strong>{{ row.venueName }}</strong>
            <span class="pill" :class="bookingStatusOf(row.status).pill">{{ bookingStatusOf(row.status).label }}</span>
          </p>
          <p class="muted tabular">
            {{ formatDate(row.bookingDate) }} · {{ formatTime(row.startTime) }} – {{ formatTime(row.endTime) }}
          </p>
          <p v-if="row.classId || row.clubId" class="chip">
            {{ row.clubId ? 'Club booking' : 'Class booking' }}
          </p>
        </div>

        <div class="decide">
          <button class="btn btn-sm btn-primary" @click="approve(row)">Approve</button>
          <button class="btn btn-sm" @click="reject(row)">Reject</button>
        </div>
      </article>

      <div v-if="!loading && !rows.length" class="empty card">
        <strong>Nothing is waiting</strong>
        <span>Every request has been decided. New ones land here and in your notifications.</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listBookings, approveBooking, rejectBooking } from '../api'
import { bookingStatusOf, purposeLabel, formatDate, formatTime } from '../utils/format'

const rows = ref([])
const loading = ref(false)

const pendingCount = computed(() => rows.value.filter((r) => r.status === 'PENDING').length)

const decidedToday = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return rows.value.filter((r) => r.status !== 'PENDING' && String(r.approvedAt || '').startsWith(today)).length
})

const oldest = computed(() => {
  const pending = rows.value.filter((r) => r.status === 'PENDING')
  if (!pending.length) return ''
  const first = pending.reduce((a, b) => (a.bookingDate <= b.bookingDate ? a : b))
  return formatDate(first.bookingDate)
})

async function load() {
  loading.value = true
  try {
    const data = await listBookings({ status: 'PENDING', page: 0, size: 50 })
    rows.value = data.content || []
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  await approveBooking(row.id)
  ElMessage.success(`${row.venueName} approved`)
  load()
}

async function reject(row) {
  let reason
  try {
    const res = await ElMessageBox.prompt(
      'The student sees this. Say what would make the request work.',
      `Reject ${row.venueName}`,
      {
        confirmButtonText: 'Reject request',
        cancelButtonText: 'Back',
        inputType: 'textarea',
        inputPlaceholder: 'Pitch is booked for the inter-school match',
      },
    )
    reason = res.value
  } catch {
    return
  }
  await rejectBooking(row.id, reason || undefined)
  ElMessage.success('Rejected')
  load()
}

onMounted(load)
</script>

<style scoped>
.summary {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.tiles {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.tile {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 128px;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
}

.tile-label {
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--faint);
}

.tile strong {
  font-size: 22px;
  font-weight: 650;
  line-height: 1.2;
}

.small-value {
  font-size: 15px !important;
  padding-top: 5px;
}

.summary .btn {
  margin-left: auto;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}

.req {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 170px;
  gap: 20px;
  align-items: center;
  padding: 16px 20px;
}

.who {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}

.avatar {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: none;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-ink);
  font-size: 13px;
  font-weight: 700;
}

.who div {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
}

.who strong {
  font-size: 13.5px;
  font-weight: 600;
}

.who .muted {
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.what p {
  margin: 0;
  font-size: 12.5px;
}

.venue-line {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 2px !important;
}

.venue-line strong {
  font-size: 14.5px;
  font-weight: 650;
}

.what .chip {
  margin-top: 6px;
}

.decide {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .req {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .decide {
    justify-content: flex-start;
  }
}
</style>
