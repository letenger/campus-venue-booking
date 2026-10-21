<template>
  <div class="page">
    <div class="toolbar">
      <span class="muted">{{ rows.length }} venues in the catalogue</span>
      <span class="spacer" />
      <button class="btn btn-sm" @click="load">
        <el-icon><Refresh /></el-icon> Refresh
      </button>
      <button class="btn btn-sm btn-primary" @click="openCreate">
        <el-icon><Plus /></el-icon> New venue
      </button>
    </div>

    <div class="card" v-loading="loading">
      <div class="thead">
        <span>Venue</span>
        <span>Where</span>
        <span>Hours</span>
        <span>Capacity</span>
        <span>Status</span>
        <span class="right">Actions</span>
      </div>

      <div v-for="row in rows" :key="row.id" class="trow">
        <div class="cell-main">
          <strong>{{ row.name }}</strong>
          <span class="muted">{{ venueTypeLabel(row.type) }} · {{ row.indoor ? 'Indoor' : 'Outdoor' }}</span>
        </div>
        <div class="cell-main">
          <span>{{ row.building || '—' }}</span>
          <span class="muted">{{ row.location || '—' }}</span>
        </div>
        <div class="tabular">{{ formatTime(row.openTime) }} – {{ formatTime(row.closeTime) }}</div>
        <div class="tabular">{{ row.capacity }}</div>
        <div>
          <span class="pill" :class="venueStatusOf(row.status).pill">{{ venueStatusOf(row.status).label }}</span>
        </div>
        <div class="actions">
          <button class="btn btn-sm" @click="openEdit(row)">Edit</button>
          <button class="btn btn-sm btn-ghost" @click="toggle(row)">
            {{ row.status === 'CLOSED' ? 'Reopen' : 'Close' }}
          </button>
        </div>
      </div>

      <div v-if="!loading && !rows.length" class="empty">
        <strong>No venues yet</strong>
        <span>Add the first court to start taking bookings.</span>
      </div>
    </div>

    <el-drawer v-model="drawer" :title="editing ? 'Edit venue' : 'New venue'" size="440px">
      <div class="form">
        <label class="field">
          <span>Name</span>
          <el-input v-model="form.name" placeholder="Badminton Court 3" />
        </label>

        <div class="two">
          <label class="field">
            <span>Type</span>
            <el-select v-model="form.type" style="width: 100%">
              <el-option v-for="(label, key) in typeOptions" :key="key" :label="label" :value="key" />
            </el-select>
          </label>
          <label class="field">
            <span>Status</span>
            <el-select v-model="form.status" style="width: 100%">
              <el-option v-for="(v, key) in statusOptions" :key="key" :label="v.label" :value="key" />
            </el-select>
          </label>
        </div>

        <div class="two">
          <label class="field">
            <span>Building</span>
            <el-input v-model="form.building" placeholder="Sports Hall" />
          </label>
          <label class="field">
            <span>Location</span>
            <el-input v-model="form.location" placeholder="Court 3" />
          </label>
        </div>

        <div class="two">
          <label class="field">
            <span>Opens</span>
            <el-time-select v-model="form.openTime" start="06:00" step="00:30" end="23:00" style="width: 100%" />
          </label>
          <label class="field">
            <span>Closes</span>
            <el-time-select v-model="form.closeTime" start="06:00" step="00:30" end="23:00" style="width: 100%" />
          </label>
        </div>

        <div class="two">
          <label class="field">
            <span>Capacity</span>
            <el-input-number v-model="form.capacity" :min="1" :max="200" controls-position="right" style="width: 100%" />
          </label>
          <label class="field">
            <span>Setting</span>
            <el-switch v-model="form.indoor" active-text="Indoor" inactive-text="Outdoor" />
          </label>
        </div>

        <label class="field">
          <span>House rules</span>
          <el-input v-model="form.rules" type="textarea" :rows="3" placeholder="Non-marking shoes only" />
        </label>
      </div>

      <template #footer>
        <div class="drawer-foot">
          <button class="btn" @click="drawer = false">Cancel</button>
          <button class="btn btn-primary" :disabled="saving" @click="save">
            {{ saving ? 'Saving…' : 'Save venue' }}
          </button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listVenues, createVenue, updateVenue, closeVenue } from '../api'
import { venueTypeLabel, venueStatusOf, formatTime, VENUE_TYPE_LABELS, VENUE_STATUS } from '../utils/format'

const rows = ref([])
const loading = ref(false)
const drawer = ref(false)
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
  drawer.value = true
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
    openTime: formatTime(row.openTime),
    closeTime: formatTime(row.closeTime),
    status: row.status,
    rules: row.rules,
  })
  editing.value = true
  drawer.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('Give the venue a name')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateVenue(form.id, { ...form })
    } else {
      const { id, ...payload } = form
      await createVenue(payload)
    }
    ElMessage.success('Saved')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  const closing = row.status !== 'CLOSED'
  try {
    await ElMessageBox.confirm(
      closing
        ? 'Closing hides the venue from booking. Existing bookings stay.'
        : 'Reopening lets students book this venue again.',
      closing ? `Close ${row.name}?` : `Reopen ${row.name}?`,
      { confirmButtonText: closing ? 'Close it' : 'Reopen', cancelButtonText: 'Back', type: 'warning' },
    )
  } catch {
    return
  }
  if (closing) {
    await closeVenue(row.id)
  } else {
    await updateVenue(row.id, { ...row, openTime: formatTime(row.openTime), closeTime: formatTime(row.closeTime), status: 'OPEN' })
  }
  ElMessage.success('Status updated')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
}

.thead,
.trow {
  display: grid;
  grid-template-columns: minmax(160px, 1.3fr) minmax(130px, 1fr) 120px 90px 110px 170px;
  gap: 14px;
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

.cell-main {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
}

.cell-main strong {
  font-weight: 600;
}

.cell-main .muted {
  font-size: 11.5px;
}

.actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.right {
  text-align: right;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field > span {
  font-size: 12px;
  font-weight: 550;
  color: var(--ink-2);
}

.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.drawer-foot {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .thead {
    display: none;
  }

  .trow {
    grid-template-columns: 1fr auto;
    gap: 6px 12px;
    padding: 14px 16px;
  }

  .trow > div:nth-child(2),
  .trow > div:nth-child(3),
  .trow > div:nth-child(4) {
    display: none;
  }

  .trow > div:nth-child(5) {
    grid-column: 1;
  }

  .actions {
    grid-column: 2;
    grid-row: 1 / span 2;
    flex-direction: column;
    align-self: center;
  }
}
</style>
