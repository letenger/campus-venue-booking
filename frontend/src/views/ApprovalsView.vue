<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">Approvals</h2>
      <span class="page-desc">Decide on class, club, event and large-group requests</span>
    </div>

    <el-alert v-if="pendingCount > 0" type="warning" :closable="false" show-icon
      :title="`${pendingCount}  requests waiting`" class="top-alert" />

    <el-table :data="rows" v-loading="loading" stripe class="appr-table">
      <el-table-column prop="venueName" label="Venue" min-width="120" />
      <el-table-column label="Requested by" width="110">
        <template #default="{ row }">#{{ row.userId }}</template>
      </el-table-column>
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
      <el-table-column label="Actions" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="success" size="small" @click="approve(row)">Approve</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click="reject(row)">Reject</el-button>
          <span v-else class="muted">Handled</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" description="Nothing is waiting for a decision" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listBookings, approveBooking, rejectBooking } from '../api'
import { bookingStatusOf, purposeLabel, formatDate, formatTime } from '../utils/format'

const rows = ref([])
const loading = ref(false)
const pendingCount = computed(() => rows.value.filter((r) => r.status === 'PENDING').length)

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
  ElMessage.success('Approved')
  load()
}
async function reject(row) {
  const { value } = await ElMessageBox.prompt('Reason (optional)', 'Reject', {
    confirmButtonText: 'Reject',
    cancelButtonText: 'Cancel',
    inputType: 'textarea',
    required: false,
  }).catch(() => ({ value: null }))
  if (value === undefined) return
  await rejectBooking(row.id, value || undefined)
  ElMessage.success('Rejected')
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
.top-alert {
  margin-bottom: 14px;
}
.appr-table {
  border-radius: 10px;
}
.muted {
  color: #b6bac0;
}
</style>
