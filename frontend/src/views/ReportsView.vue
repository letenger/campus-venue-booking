<template>
  <div v-loading="loading">
    <div class="page-head">
      <h2 class="page-title">使用报表</h2>
      <div class="table-toolbar" style="margin: 0">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
          :clearable="false" start-placeholder="开始" end-placeholder="结束" @change="load" style="width: 260px" />
        <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="summary-cards">
      <el-col :span="6"><el-card shadow="never"><div class="kpi-num">{{ summary.total || 0 }}</div><div class="kpi-label">预订总数</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="kpi-num">{{ summary.byStatus?.APPROVED || 0 }}</div><div class="kpi-label">已通过</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="kpi-num">{{ summary.byStatus?.PENDING || 0 }}</div><div class="kpi-label">待审批</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="kpi-num">{{ summary.byStatus?.NO_SHOW || 0 }}</div><div class="kpi-label">爽约</div></el-card></el-col>
    </el-row>

    <el-card class="report-card">
      <span class="card-title">场馆利用率</span>
      <el-table :data="util" stripe>
        <el-table-column prop="venueName" label="场馆" min-width="140" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }"><el-tag effect="plain" size="small">{{ venueTypeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="total" label="总预订" width="100" align="center" />
        <el-table-column prop="completed" label="已完成" width="100" align="center" />
        <el-table-column prop="cancelled" label="已取消" width="100" align="center" />
        <el-table-column prop="noShow" label="爽约" width="100" align="center" />
        <el-table-column label="利用率" width="160">
          <template #default="{ row }">
            <el-progress :percentage="row.utilizationRate || 0" :stroke-width="14" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { utilizationReport, bookingSummary } from '../api'
import { venueTypeLabel } from '../utils/format'

const loading = ref(false)
const range = ref([lastMonth(), todayStr()])
const util = ref([])
const summary = ref({})

function todayStr() {
  return new Date().toISOString().slice(0, 10)
}
function lastMonth() {
  const d = new Date()
  d.setDate(d.getDate() - 30)
  return d.toISOString().slice(0, 10)
}

async function load() {
  loading.value = true
  try {
    const [u, s] = await Promise.all([
      utilizationReport({ from: range.value[0], to: range.value[1] }),
      bookingSummary({ from: range.value[0], to: range.value[1] }),
    ])
    util.value = u || []
    summary.value = s || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.page-title {
  margin: 0;
  font-size: 20px;
}
.summary-cards {
  margin-bottom: 16px;
}
.kpi-num {
  font-size: 26px;
  font-weight: 600;
  color: #2f6fed;
}
.kpi-label {
  font-size: 12px;
  color: #8a9099;
  margin-top: 2px;
}
.report-card {
  border-radius: 10px;
}
.card-title {
  font-weight: 600;
  font-size: 15px;
}
</style>
