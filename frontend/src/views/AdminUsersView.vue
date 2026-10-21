<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">Users</h2>
      <span class="page-desc">See who has an account, switch it on or off</span>
    </div>

    <div class="table-toolbar">
      <el-select v-model="role" placeholder="All roles" clearable style="width: 150px" @change="load">
        <el-option v-for="(label, key) in roleOptions" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" :icon="Refresh" @click="load">Refresh</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="username" label="Username" width="140" />
      <el-table-column prop="realName" label="Name" width="120" />
      <el-table-column label="Role" width="100">
        <template #default="{ row }"><el-tag effect="plain" size="small">{{ roleLabel(row.role) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="studentNo" label="Student no." width="120" />
      <el-table-column prop="phone" label="Phone" width="130" />
      <el-table-column label="Status" width="100">
        <template #default="{ row }">
          <el-tag :type="userStatusOf(row.status).type" size="small">{{ userStatusOf(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'ACTIVE'" link type="danger" size="small" @click="setStatus(row, 'DISABLED')">Disable</el-button>
          <el-button v-else link type="success" size="small" @click="setStatus(row, 'ACTIVE')">Enable</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="total > size" class="pager" layout="prev, pager, next" :total="total"
      :page-size="size" :current-page="page" @current-change="onPage" background />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listUsers, setUserStatus } from '../api'
import { roleLabel, userStatusOf, ROLE_LABELS, USER_STATUS } from '../utils/format'

const rows = ref([])
const loading = ref(false)
const role = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const roleOptions = ROLE_LABELS
const statusOptions = USER_STATUS

async function load() {
  loading.value = true
  try {
    const data = await listUsers({
      role: role.value || undefined,
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
async function setStatus(row, status) {
  const label = status === 'DISABLED' ? 'Disable' : 'Enable'
  await ElMessageBox.confirm(`${label} ${row.realName || row.username}?`, label, { type: 'warning' }).catch(() => Promise.reject())
  await setUserStatus(row.id, status)
  ElMessage.success('Updated')
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
.pager {
  margin-top: 12px;
  justify-content: center;
}
</style>
