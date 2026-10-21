<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">Manage venues</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">New venue</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="name" label="Name" min-width="140" />
      <el-table-column label="Type" width="130">
        <template #default="{ row }"><el-tag effect="plain" size="small">{{ venueTypeLabel(row.type) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="Indoor" width="90">
        <template #default="{ row }">{{ row.indoor ? 'Indoor' : 'Outdoor' }}</template>
      </el-table-column>
      <el-table-column prop="building" label="Building" width="110" />
      <el-table-column prop="location" label="Location" width="110" />
      <el-table-column prop="capacity" label="Capacity" width="80" align="center" />
      <el-table-column label="Open hours" width="150">
        <template #default="{ row }">{{ formatTime(row.openTime) }}–{{ formatTime(row.closeTime) }}</template>
      </el-table-column>
      <el-table-column label="Status" width="100">
        <template #default="{ row }">
          <el-tag :type="venueStatusOf(row.status).type" size="small">{{ venueStatusOf(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">Edit</el-button>
          <el-button link :type="row.status === 'CLOSED' ? 'success' : 'warning'" size="small" @click="toggle(row)">
            {{ row.status === 'CLOSED' ? 'Reopen' : 'Close' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? 'Edit venue' : 'New venue'" width="520px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="Name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="(label, key) in typeOptions" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="Indoor">
          <el-switch v-model="form.indoor" active-text="Indoor" inactive-text="Outdoor" />
        </el-form-item>
        <el-form-item label="Building"><el-input v-model="form.building" /></el-form-item>
        <el-form-item label="Location"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="Capacity"><el-input-number v-model="form.capacity" :min="1" :max="500" /></el-form-item>
        <el-form-item label="Open hours">
          <el-time-select v-model="form.openTime" :start="'06:00'" :step="'01:00'" :end="'22:00'" placeholder="Open" style="width: 130px" />
          <span style="margin: 0 8px">–</span>
          <el-time-select v-model="form.closeTime" :start="'07:00'" :step="'01:00'" :end="'23:00'" placeholder="Close" style="width: 130px" />
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="form.status" style="width: 140px">
            <el-option v-for="(v, k) in statusOptions" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="Rules">
          <el-input v-model="form.rules" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="save">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listVenues, createVenue, updateVenue, closeVenue } from '../api'
import { venueTypeLabel, venueStatusOf, formatTime, VENUE_TYPE_LABELS, VENUE_STATUS } from '../utils/format'

const rows = ref([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref(false)
const saving = ref(false)

const typeOptions = VENUE_TYPE_LABELS
const statusOptions = VENUE_STATUS

const emptyForm = () => ({
  id: null,
  name: '',
  type: 'BADMINTON',
  indoor: true,
  building: '',
  location: '',
  capacity: 4,
  openTime: '08:00',
  closeTime: '21:00',
  status: 'OPEN',
  rules: '',
})
const form = reactive(emptyForm())

async function load() {
  loading.value = true
  try {
    const data = await listVenues({ page: 0, size: 100 })
    rows.value = data.content || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, emptyForm())
  editing.value = false
  dialog.value = true
}
function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    type: row.type,
    indoor: row.indoor,
    building: row.building,
    location: row.location,
    capacity: row.capacity,
    openTime: row.openTime,
    closeTime: row.closeTime,
    status: row.status,
    rules: row.rules,
  })
  editing.value = true
  dialog.value = true
}
async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await updateVenue(form.id, { ...form })
    } else {
      const { id, ...payload } = form
      await createVenue(payload)
    }
    ElMessage.success('Saved')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function toggle(row) {
  const next = row.status === 'CLOSED' ? 'Reopen' : 'Close'
  await ElMessageBox.confirm(`${next} ${row.name}?`, next, { type: 'warning' }).catch(() => Promise.reject())
  // Delete only sets the status to CLOSED; use Edit to reopen it
  if (row.status !== 'CLOSED') {
    await closeVenue(row.id)
  } else {
    await updateVenue(row.id, { ...row, status: 'OPEN' })
  }
  ElMessage.success('Status updated')
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
</style>
