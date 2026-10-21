<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="status" placeholder="All statuses" clearable style="width: 170px" @change="reload">
        <el-option v-for="(v, k) in statusOptions" :key="k" :label="v.label" :value="k" />
      </el-select>

      <el-switch v-if="isStaff" v-model="mineOnly" active-text="Only mine" @change="reload" />

      <span class="spacer" />
      <span class="muted count">{{ total }} booking{{ total === 1 ? '' : 's' }}</span>
      <button class="btn btn-sm" @click="reload">
        <el-icon><Refresh /></el-icon> Refresh
      </button>
    </div>

    <div v-loading="loading" class="card">
      <div class="thead">
        <span>When</span>
        <span>Venue</span>
        <span>Purpose</span>
        <span>Status</span>
        <span class="right">Actions</span>
      </div>

      <div v-for="row in rows" :key="row.id" class="trow">
        <div class="when">
          <strong>{{ niceDate(row.bookingDate) }}</strong>
          <span class="muted tabular">{{ formatTime(row.startTime) }} – {{ formatTime(row.endTime) }}</span>
        </div>

        <div class="venue">
          <router-link :to="`/venues/${row.venueId}`">{{ row.venueName || 'Venue' }}</router-link>
          <span class="muted">{{ row.attendeesCount }} {{ row.attendeesCount === 1 ? 'person' : 'people' }}</span>
        </div>

        <div class="purpose">
          <span class="chip">{{ purposeLabel(row.purpose) }}</span>
          <span v-if="row.rejectReason" class="reason">Rejected: {{ row.rejectReason }}</span>
        </div>

        <div>
          <span class="pill" :class="bookingStatusOf(row.status).pill">{{ bookingStatusOf(row.status).label }}</span>
        </div>

        <div class="actions">
          <button v-if="row.canCancel" class="btn btn-sm" @click="cancel(row)">Cancel</button>
          <button v-if="canCheckIn(row)" class="btn btn-sm" @click="checkIn(row)">Check in</button>
          <button v-if="canCheckIn(row)" class="btn btn-sm btn-ghost" @click="noShow(row)">No-show</button>
          <span v-if="!row.canCancel && !canCheckIn(row)" class="muted">—</span>
        </div>
      </div>

      <div v-if="!loading && !rows.length" class="empty">
        <strong>No bookings yet</strong>
        <span>Pick a venue and reserve an hour — it takes two clicks.</span>
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
import { listBookings, cancelBooking, checkInBooking, markNoShowBooking } from '../api'
import { authState } from '../store/auth'
import {
  bookingStatusOf, purposeLabel, formatDate, formatTime,
} from '../utils/format'

const isStaff = computed(() => ['TEACHER', 'ADMIN'].includes(authState.user?.role))
const rows = ref([])
const loading = ref(false)
const status = ref('')
const mineOnly = ref(true)
const page = ref(1)
const size = ref(15)
const total = ref(0)

const statusOptions = {
  PENDING: { label: 'Pending' },
  APPROVED: { label: 'Approved' },
  REJECTED: { label: 'Rejected' },
  CANCELLED: { label: 'Cancelled' },
  COMPLETED: { label: 'Completed' },
  NO_SHOW: { label: 'No-show' },
}

const pages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

function niceDate(value) {
  if (!value) return '—'
  const date = new Date(value + 'T00:00:00')
  const options = { weekday: 'short', day: 'numeric', month: 'short' }
  // only show the year when it is not the current one
  if (date.getFullYear() !== new Date().getFullYear()) options.year = 'numeric'
  return date.toLocaleDateString('en-AU', options)
}

// Staff can close a booking once it has started; the API still enforces the role.
function canCheckIn(row) {
  if (!isStaff.value || row.status !== 'APPROVED') return false
  const start = new Date(`${row.bookingDate}T${formatTime(row.endTime)}:00`)
  return start < new Date()
}

async function reload() {
  loading.value = true
  try {
    const data = await listBookings({
      status: status.value || undefined,
      mine: isStaff.value ? (mineOnly.value ? true : undefined) : undefined,
      page: page.value - 1,
      size: size.value,
    })
    rows.value = data.content || []
    total.value = data.totalElements || 0
  } finally {
    loading.value = false
  }
}

function go(p) {
  page.value = p
  reload()
}

async function cancel(row) {
  try {
    await ElMessageBox.confirm('Give this slot back? Someone else can book it right away.', 'Cancel booking', {
      confirmButtonText: 'Cancel it',
      cancelButtonText: 'Keep it',
      type: 'warning',
    })
  } catch {
    return
  }
  await cancelBooking(row.id)
  ElMessage.success('Booking cancelled')
  reload()
}

async function checkIn(row) {
  await checkInBooking(row.id)
  ElMessage.success('Checked in')
  reload()
}

async function noShow(row) {
  try {
    await ElMessageBox.confirm('The venue was not used?', 'Mark no-show', {
      confirmButtonText: 'Mark it',
      cancelButtonText: 'Back',
      type: 'warning',
    })
  } catch {
    return
  }
  await markNoShowBooking(row.id)
  ElMessage.success('Marked as no-show')
  reload()
}

onMounted(reload)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.count {
  font-size: 12.5px;
}

.thead,
.trow {
  display: grid;
  grid-template-columns: 150px minmax(140px, 1.1fr) minmax(140px, 1fr) 110px 220px;
  gap: 16px;
  align-items: center;
  padding: 12px 22px;
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

.when,
.venue,
.purpose {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.when strong {
  font-size: 13.5px;
  font-weight: 600;
}

.when .muted,
.venue .muted {
  font-size: 12px;
}

.venue a {
  font-weight: 550;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.purpose {
  align-items: flex-start;
  gap: 4px;
}

.reason {
  font-size: 11.5px;
  color: var(--rose);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.actions {
  display: flex;
  gap: 6px;
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

  .when,
  .venue,
  .purpose {
    grid-column: 1;
  }

  .trow > div:nth-child(4) {
    grid-column: 2;
    grid-row: 1;
    justify-self: end;
  }

  .actions {
    grid-column: 2;
    grid-row: 2 / span 2;
    flex-direction: column;
    align-self: start;
  }
}
</style>
