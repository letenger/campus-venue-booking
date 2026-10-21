<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">场馆管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增场馆</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="类型" width="130">
        <template #default="{ row }"><el-tag effect="plain" size="small">{{ venueTypeLabel(row.type) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="室内/外" width="90">
        <template #default="{ row }">{{ row.indoor ? '室内' : '室外' }}</template>
      </el-table-column>
      <el-table-column prop="building" label="楼栋" width="110" />
      <el-table-column prop="location" label="位置" width="110" />
      <el-table-column prop="capacity" label="容量" width="80" align="center" />
      <el-table-column label="开放时间" width="150">
        <template #default="{ row }">{{ formatTime(row.openTime) }}–{{ formatTime(row.closeTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="venueStatusOf(row.status).type" size="small">{{ venueStatusOf(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 'CLOSED' ? 'success' : 'warning'" size="small" @click="toggle(row)">
            {{ row.status === 'CLOSED' ? '重新开放' : '关闭' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑场馆' : '新增场馆'" width="520px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="(label, key) in typeOptions" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="室内/外">
          <el-switch v-model="form.indoor" active-text="室内" inactive-text="室外" />
        </el-form-item>
        <el-form-item label="楼栋"><el-input v-model="form.building" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="1" :max="500" /></el-form-item>
        <el-form-item label="开放时间">
          <el-time-select v-model="form.openTime" :start="'06:00'" :step="'01:00'" :end="'22:00'" placeholder="开放" style="width: 130px" />
          <span style="margin: 0 8px">–</span>
          <el-time-select v-model="form.closeTime" :start="'07:00'" :step="'01:00'" :end="'23:00'" placeholder="关闭" style="width: 130px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 140px">
            <el-option v-for="(v, k) in statusOptions" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用须知">
          <el-input v-model="form.rules" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
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
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function toggle(row) {
  const next = row.status === 'CLOSED' ? '重新开放' : '关闭'
  await ElMessageBox.confirm(`确定${next}「${row.name}」？`, next, { type: 'warning' }).catch(() => Promise.reject())
  // 后端 DELETE 仅将状态置为 CLOSED；重新开放走编辑接口
  if (row.status !== 'CLOSED') {
    await closeVenue(row.id)
  } else {
    await updateVenue(row.id, { ...row, status: 'OPEN' })
  }
  ElMessage.success('已更新状态')
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
