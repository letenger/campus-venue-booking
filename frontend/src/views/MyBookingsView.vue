<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">My bookings</h2>
      <span class="page-desc">{{ isStaff ? 'See every booking, record check-ins and no-shows' : 'Your bookings, in one place' }}</span>
    </div>

    <div class="table-toolbar">
      <el-select v-model="status" placeholder="All statuses" clearable style="width: 150px" @change="load">
        <el-option v-for="(v, k) in statusOptions" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-checkbox v-if="!isStaff" v-model="mineOnly" @change="load">Only mine</el-checkbox>
      <el-button type="primary" :icon="Refresh" @click="load">Refresh</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe class="book-table">
      <el-table-column prop="venueName" label="Venue" min-width="120" />
      <el-table-column label="Date" width="110">
        <template #default="{ row }">{{ formatDate(row.bookingDate) }}</template>
      </el-table-column>
      <el-table-column label="Time" width="140">
        <template #default="{ row }">{{ formatTime(row.startTime) }}–{{ formatTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="Purpose" width="130">
        <template #default="{ row }"><el-tag effect="plain" size="small">{{ purposeLabel(row.purpose) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="attendeesCount" label="People" width="80" align="center" />
      <el-table-column label="Status" width="100">
        <template #default="{ row }">
          <el-tag :type="bookingStatusOf(row.status).type" size="small">{{ bookingStatusOf(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.canCancel" link type="danger" size="small" @click="cancel(row)">Cancel</el-button>
          <el-button v-if="isStaff && row.status === 'APPROVED'" link type="success" size="small" @click="checkIn(row)">Check in</el-button>
          <el-button v-if="isStaff && row.status === 'APPROVED'" link type="warning" size="small" @click="noShow(row)">No-show</el-button>
          <span v-if="!row.canCancel && !(isStaff && row.status === 'APPROVED')" class="muted">—</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="total > size" class="pager" layout="prev, pager, next" :total="total"
      :page-size="size" :current-page="page" @current-change="onPage" background />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listBookings, cancelBooking, checkInBooking, markNoShowBooking } from '../api'
import { authState } from '../store/auth'
import { bookingStatusOf, purposeLabel, formatDate, formatTime } from '../utils/format'

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

async function load() {
  loading.value = true
  try {
    const data = await listBookings({
      status: status.value || undefined,
      mine: isStaff.value ? undefined : true,
      page: page.value - 1,
      size: size.value,
    })
    rows.value = data.content || []
    total.value = data.totalElements || 0
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  page.value = p
  load()
}

async function cancel(row) {
  await ElMessageBox.confirm('Cancel this booking?', 'Cancel', { type: 'warning' }).catch(() => Promise.reject())
  await cancelBooking(row.id)
  ElMessage.success('Cancelled')
  load()
}
async function checkIn(row) {
  await checkInBooking(row.id)
  ElMessage.success('Checked in')
  load()
}
async function noShow(row) {
  await ElMessageBox.confirm('Mark as no-show?', 'No-show', { type: 'warning' }).catch(() => Promise.reject())
  await markNoShowBooking(row.id)
  ElMessage.success('Marked as no-show')
  load()
}

onMounted(load)
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 20px;
}
.page-desc {
  color: #8a9099;
  font-size: 13px;
}
.book-table {
  border-radius: 10px;
}
.pager {
  margin-top: 12px;
  justify-content: center;
}
.muted {
  color: #b6bac0;
}
</style>
