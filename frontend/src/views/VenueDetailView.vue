<template>
  <div v-loading="loading">
    <el-page-header class="back" @back="router.back()" title="返回" />

    <el-card v-if="venue" class="info-card">
      <div class="info-top">
        <div>
          <h2 style="margin: 0 8px 0 0; display: inline-block">{{ venue.name }}</h2>
          <el-tag :type="venueStatusOf(venue.status).type" size="small">{{ venueStatusOf(venue.status).label }}</el-tag>
        </div>
        <div class="info-tags">
          <el-tag effect="plain">{{ venueTypeLabel(venue.type) }}</el-tag>
          <el-tag effect="plain" type="info">{{ venue.indoor ? '室内' : '室外' }}</el-tag>
          <el-tag effect="plain" type="info">可容纳 {{ venue.capacity }} 人</el-tag>
        </div>
      </div>
      <div class="info-meta">
        <span><el-icon><Location /></el-icon> {{ venue.building }} · {{ venue.location }}</span>
        <span><el-icon><Clock /></el-icon> {{ formatTime(venue.openTime) }} – {{ formatTime(venue.closeTime) }}</span>
      </div>
      <p v-if="venue.rules" class="rules">使用须知：{{ venue.rules }}</p>
    </el-card>

    <el-card class="avail-card">
      <div class="table-toolbar">
        <span class="card-title">可预订时段</span>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
          :clearable="false" :start-placeholder="'开始日期'" :end-placeholder="'结束日期'"
          @change="loadAvailability" style="width: 260px" />
      </div>

      <div v-for="(day, di) in grouped" :key="di" class="avail-day">
        <div class="avail-date">{{ formatDate(day.date) }} · 周{{ weekday(day.date) }}</div>
        <div class="slot-wrap">
          <span v-for="(s, si) in day.slots" :key="si"
            class="slot" :class="slotClass(s)"
            @click="pickSlot(s)">
            {{ formatTime(s.startTime) }}–{{ formatTime(s.endTime) }}
          </span>
        </div>
      </div>
      <el-empty v-if="grouped.length === 0" description="该时段暂无可排期（场馆未开放或不在开放日内）" />
    </el-card>

    <el-card class="book-card">
      <span class="card-title">提交预订</span>
      <el-form :model="form" label-width="92px" class="book-form">
        <el-form-item label="日期">
          <el-date-picker v-model="form.bookingDate" type="date" value-format="YYYY-MM-DD"
            :disabled-date="disabledDate" placeholder="选择日期" style="width: 200px" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="form.startTime" :start="startWindow" :step="'01:00'"
            :end="endWindow" placeholder="开始" style="width: 130px" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select v-model="form.endTime" :start="form.startTime || startWindow" :step="'01:00'"
            :end="endWindow" :min-time="form.startTime" placeholder="结束" style="width: 130px" />
        </el-form-item>
        <el-form-item label="用途">
          <el-select v-model="form.purpose" style="width: 200px">
            <el-option v-for="(label, key) in purposeOptions" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="参与人数">
          <el-input-number v-model="form.attendeesCount" :min="1" :max="venue?.capacity || 50" />
        </el-form-item>
        <el-form-item label="班级" v-if="classes.length">
          <el-select v-model="form.classId" placeholder="可选" clearable style="width: 200px">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="社团" v-if="clubs.length">
          <el-select v-model="form.clubId" placeholder="可选" clearable style="width: 200px">
            <el-option v-for="c in clubs" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">提交预订</el-button>
          <span class="hint">{{ needApprovalHint }}</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, Clock } from '@element-plus/icons-vue'
import { getVenue, getAvailability, createBooking, listClasses, listClubs } from '../api'
import {
  venueTypeLabel, venueStatusOf, formatTime, formatDate, purposeLabel, BOOKING_PURPOSE,
} from '../utils/format'

const route = useRoute()
const router = useRouter()
const venueId = route.params.id

const venue = ref(null)
const loading = ref(false)
const submitting = ref(false)
const slots = ref([])
const range = ref([todayStr(), addDays(7)])
const classes = ref([])
const clubs = ref([])

const form = reactive({
  venueId: Number(venueId),
  bookingDate: todayStr(),
  startTime: '',
  endTime: '',
  purpose: 'FREE_PLAY',
  attendeesCount: 1,
  classId: null,
  clubId: null,
})

const purposeOptions = BOOKING_PURPOSE
const startWindow = '08:00'
const endWindow = '22:00'

const grouped = computed(() => {
  const map = {}
  for (const s of slots.value) {
    const key = s.date
    if (!map[key]) map[key] = { date: s.date, slots: [] }
    map[key].slots.push(s)
  }
  return Object.values(map).sort((a, b) => (a.date < b.date ? -1 : 1))
})

const needApprovalHint = computed(() => {
  if (['CLUB_TRAINING', 'EVENT', 'EXAM'].includes(form.purpose)) return '该用途需教师审批后生效'
  if (form.classId || form.clubId || form.attendeesCount >= 10) return '该预订需教师审批后生效'
  return '小型自由活动将自动通过'
})

function todayStr() {
  return new Date().toISOString().slice(0, 10)
}
function addDays(n) {
  const d = new Date()
  d.setDate(d.getDate() + n)
  return d.toISOString().slice(0, 10)
}
function disabledDate(time) {
  const t = new Date(time).getTime()
  const lo = new Date(range.value[0]).getTime()
  const hi = new Date(range.value[1]).getTime()
  return t < lo || t > hi
}
function weekday(dateStr) {
  const names = ['日', '一', '二', '三', '四', '五', '六']
  return names[new Date(dateStr).getDay()]
}
function slotClass(s) {
  if (s.available) return 'free'
  return s.reason === '已被预订' ? 'taken' : 'closed'
}
function pickSlot(s) {
  if (!s.available) {
    ElMessage.warning(s.reason || '该时段不可预订')
    return
  }
  form.bookingDate = s.date
  form.startTime = s.startTime
  form.endTime = s.endTime
  ElMessage.info('已填入预订表单，请确认后提交')
}

async function loadAvailability() {
  const [from, to] = range.value
  slots.value = await getAvailability(venueId, from, to)
}

async function submit() {
  if (!form.bookingDate || !form.startTime || !form.endTime) {
    ElMessage.warning('请选择日期与时间')
    return
  }
  if (form.endTime <= form.startTime) {
    ElMessage.warning('结束时间需晚于开始时间')
    return
  }
  submitting.value = true
  try {
    const res = await createBooking({ ...form })
    ElMessage.success(res.status === 'APPROVED' ? '预订已自动通过' : '预订已提交，等待审批')
    router.push('/bookings')
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    venue.value = await getVenue(venueId)
    const [c, cl] = await Promise.all([listClasses(), listClubs()])
    classes.value = c || []
    clubs.value = cl || []
    await loadAvailability()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.back {
  margin-bottom: 12px;
}
.info-card,
.avail-card,
.book-card {
  margin-bottom: 16px;
  border-radius: 10px;
}
.info-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.info-tags {
  display: flex;
  gap: 6px;
}
.info-meta {
  color: #646a73;
  font-size: 13px;
  margin-top: 8px;
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.info-meta .el-icon {
  vertical-align: -2px;
  margin-right: 4px;
}
.rules {
  background: #fbf6e9;
  color: #8a6d3b;
  font-size: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  margin: 12px 0 0;
}
.card-title {
  font-weight: 600;
  font-size: 15px;
}
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 8px;
}
.avail-day {
  margin-bottom: 14px;
}
.avail-date {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
  color: #1f2329;
}
.slot-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.slot {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
}
.slot.free {
  background: #eafaf0;
  color: #1a8a4f;
  border-color: #bfe9cd;
}
.slot.taken {
  background: #fdeaea;
  color: #c0392b;
  border-color: #f3c9c9;
  cursor: not-allowed;
}
.slot.closed {
  background: #f1f2f3;
  color: #9aa0a6;
  border-color: #e3e5e8;
  cursor: not-allowed;
}
.book-form {
  margin-top: 16px;
  max-width: 480px;
}
.hint {
  font-size: 12px;
  color: #8a9099;
  margin-left: 12px;
}
</style>
