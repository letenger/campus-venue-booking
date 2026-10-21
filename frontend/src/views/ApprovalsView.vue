<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">预订审批</h2>
      <span class="page-desc">处理待审批的班级 / 社团 / 赛事 / 大批量预订</span>
    </div>

    <el-alert v-if="pendingCount > 0" type="warning" :closable="false" show-icon
      :title="`当前有 ${pendingCount} 条待审批预订`" class="top-alert" />

    <el-table :data="rows" v-loading="loading" stripe class="appr-table">
      <el-table-column prop="venueName" label="场馆" min-width="120" />
      <el-table-column label="预订人" width="110">
        <template #default="{ row }">#{{ row.userId }}</template>
      </el-table-column>
      <el-table-column label="日期" width="110">
        <template #default="{ row }">{{ formatDate(row.bookingDate) }}</template>
      </el-table-column>
      <el-table-column label="时间" width="140">
        <template #default="{ row }">{{ formatTime(row.startTime) }}–{{ formatTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="用途" width="130">
        <template #default="{ row }"><el-tag effect="plain" size="small">{{ purposeLabel(row.purpose) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="attendeesCount" label="人数" width="80" align="center" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="bookingStatusOf(row.status).type" size="small">{{ bookingStatusOf(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="success" size="small" @click="approve(row)">通过</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click="reject(row)">拒绝</el-button>
          <span v-else class="muted">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" description="暂无待审批预订" />
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
  ElMessage.success('已通过')
  load()
}
async function reject(row) {
  const { value } = await ElMessageBox.prompt('拒绝原因（可选）', '拒绝预订', {
    confirmButtonText: '拒绝',
    cancelButtonText: '取消',
    inputType: 'textarea',
    required: false,
  }).catch(() => ({ value: null }))
  if (value === undefined) return
  await rejectBooking(row.id, value || undefined)
  ElMessage.success('已拒绝')
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
