<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">Venues</h2>
      <span class="page-desc">Browse campus venues, see what is open, book a slot</span>
    </div>

    <div class="table-toolbar">
      <el-input v-model="keyword" placeholder="Search name or building" clearable style="width: 240px"
        @keyup.enter="load" @clear="load" />
      <el-select v-model="status" placeholder="All statuses" clearable style="width: 150px" @change="load">
        <el-option v-for="(v, k) in statusOptions" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load">Search</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="v in venues" :key="v.id" :xs="24" :sm="12" :md="8" :lg="6" class="venue-col">
        <el-card shadow="hover" class="venue-card" @click="open(v.id)">
          <div class="venue-top">
            <span class="venue-name">{{ v.name }}</span>
            <el-tag :type="venueStatusOf(v.status).type" size="small">{{ venueStatusOf(v.status).label }}</el-tag>
          </div>
          <div class="venue-meta">
            <el-tag size="small" effect="plain">{{ venueTypeLabel(v.type) }}</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ v.indoor ? 'Indoor' : 'Outdoor' }}</el-tag>
          </div>
          <div class="venue-info">
            <div><el-icon><Location /></el-icon> {{ v.building || '—' }} · {{ v.location || '—' }}</div>
            <div><el-icon><User /></el-icon> {{ v.capacity }} people</div>
            <div><el-icon><Clock /></el-icon> {{ formatTime(v.openTime) }} – {{ formatTime(v.closeTime) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && venues.length === 0" description="No venues match" />

    <el-pagination v-if="total > size" class="pager" layout="prev, pager, next" :total="total"
      :page-size="size" :current-page="page" @current-change="onPage" background />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Location, User, Clock } from '@element-plus/icons-vue'
import { listVenues } from '../api'
import { venueTypeLabel, venueStatusOf, formatTime } from '../utils/format'

const router = useRouter()
const venues = ref([])
const loading = ref(false)
const keyword = ref('')
const status = ref('')
const page = ref(1)
const size = ref(12)
const total = ref(0)

const statusOptions = {
  OPEN: { label: 'Open' },
  MAINTENANCE: { label: 'Maintenance' },
  CLOSED: { label: 'Closed' },
}

async function load() {
  loading.value = true
  try {
    const data = await listVenues({
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      page: page.value - 1,
      size: size.value,
    })
    venues.value = data.content || []
    total.value = data.totalElements || 0
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  page.value = p
  load()
}
function open(id) {
  router.push(`/venues/${id}`)
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
.venue-col {
  margin-bottom: 16px;
}
.venue-card {
  cursor: pointer;
  border-radius: 10px;
}
.venue-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.venue-name {
  font-size: 15px;
  font-weight: 600;
}
.venue-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}
.venue-info {
  font-size: 12px;
  color: #646a73;
  line-height: 1.9;
}
.venue-info .el-icon {
  margin-right: 4px;
  vertical-align: -2px;
}
.pager {
  margin-top: 8px;
  justify-content: center;
}
</style>
