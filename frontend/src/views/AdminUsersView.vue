<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">用户管理</h2>
      <span class="page-desc">查看系统用户并启停账号</span>
    </div>

    <div class="table-toolbar">
      <el-select v-model="role" placeholder="全部角色" clearable style="width: 150px" @change="load">
        <el-option v-for="(label, key) in roleOptions" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }"><el-tag effect="plain" size="small">{{ roleLabel(row.role) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="userStatusOf(row.status).type" size="small">{{ userStatusOf(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'ACTIVE'" link type="danger" size="small" @click="setStatus(row, 'DISABLED')">禁用</el-button>
          <el-button v-else link type="success" size="small" @click="setStatus(row, 'ACTIVE')">启用</el-button>
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
  const label = status === 'DISABLED' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${label}「${row.realName || row.username}」？`, label, { type: 'warning' }).catch(() => Promise.reject())
  await setUserStatus(row.id, status)
  ElMessage.success('已更新')
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
