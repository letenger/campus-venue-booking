<template>
  <div v-loading="loading">
    <el-page-header class="back" @back="router.back()" title="Back" />

    <el-card v-if="venue" class="info-card">
      <div class="info-top">
        <div>
          <h2 style="margin: 0 8px 0 0; display: inline-block">{{ venue.name }}</h2>
          <el-tag :type="venueStatusOf(venue.status).type" size="small">{{ venueStatusOf(venue.status).label }}</el-tag>
        </div>
        <div class="info-tags">
          <el-tag effect="plain">{{ venueTypeLabel(venue.type) }}</el-tag>
          <el-tag effect="plain" type="info">{{ venue.indoor ? 'Indoor' : 'Outdoor' }}</el-tag>
          <el-tag effect="plain" type="info">{{ venue.capacity }} people</el-tag>
        </div>
      </div>
      <div class="info-meta">
        <span><el-icon><Location /></el-icon> {{ venue.building }} · {{ venue.location }}</span>
        <span><el-icon><Clock /></el-icon> {{ formatTime(venue.openTime) }} – {{ formatTime(venue.closeTime) }}</span>
      </div>
      <p v-if="venue.rules" class="rules">Rules: {{ venue.rules }}</p>
    </el-card>

    <el-card class="avail-card">
      <div class="table-toolbar">
        <span class="card-title">Availability</span>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
          :clearable="false" :start-placeholder="'From'" :end-placeholder="'To'"
          @change="loadAvailability" style="width: 260px" />
      </div>

      <div v-for="(day, di) in grouped" :key="di" class="avail-day">
        <div class="avail-date">{{ formatDate(day.date) }} · {{ weekday(day.date) }}</div>
        <div class="slot-wrap">
          <span v-for="(s, si) in day.slots" :key="si"
            class="slot" :class="slotClass(s)"
            @click="pickSlot(s)">
            {{ formatTime(s.startTime) }}–{{ formatTime(s.endTime) }}
          </span>
        </div>
      </div>
      <el-empty v-if="grouped.length === 0" description="No slots this day — the venue is closed or not scheduled" />
    </el-card>

    <el-card class="book-card">
      <span class="card-title">Book this slot</span>
      <el-form :model="form" label-width="92px" class="book-form">
        <el-form-item label="Date">
          <el-date-picker v-model="form.bookingDate" type="date" value-format="YYYY-MM-DD"
            :disabled-date="disabledDate" placeholder="Date" style="width: 200px" />
        </el-form-item>
        <el-form-item label="Start">
          <el-time-select v-model="form.startTime" :start="startWindow" :step="'01:00'"
            :end="endWindow" placeholder="Start" style="width: 130px" />
        </el-form-item>
        <el-form-item label="End">
          <el-time-select v-model="form.endTime" :start="form.startTime || startWindow" :step="'01:00'"
            :end="endWindow" :min-time="form.startTime" placeholder="End" style="width: 130px" />
        </el-form-item>
        <el-form-item label="Purpose">
          <el-select v-model="form.purpose" style="width: 200px">
            <el-option v-for="(label, key) in purposeOptions" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="People">
          <el-input-number v-model="form.attendeesCount" :min="1" :max="venue?.capacity || 50" />
        </el-form-item>
        <el-form-item label="Class" v-if="classes.length">
          <el-select v-model="form.classId" placeholder="optional" clearable style="width: 200px">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Club" v-if="clubs.length">
          <el-select v-model="form.clubId" placeholder="optional" clearable style="width: 200px">
            <el-option v-for="c in clubs" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">Book this slot</el-button>
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
  if (['CLUB_TRAINING', 'EVENT', 'EXAM'].includes(form.purpose)) return "This purpose needs a teacher's approval"
  if (form.classId || form.clubId || form.attendeesCount >= 10) return "This request needs a teacher's approval"
  return 'Small free-play bookings confirm automatically'
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
  const names = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
  return names[new Date(dateStr).getDay()]
}
function slotClass(s) {
  if (s.available) return 'free'
  return s.reason === 'Already booked' ? 'taken' : 'closed'
}
function pickSlot(s) {
  if (!s.available) {
    ElMessage.warning(s.reason || 'That slot is not available')
    return
  }
  form.bookingDate = s.date
  form.startTime = s.startTime
  form.endTime = s.endTime
  ElMessage.info('Slot copied into the form — confirm and submit')
}

async function loadAvailability() {
  const [from, to] = range.value
  slots.value = await getAvailability(venueId, from, to)
}

async function submit() {
  if (!form.bookingDate || !form.startTime || !form.endTime) {
    ElMessage.warning('Pick a date and a time')
    return
  }
  if (form.endTime <= form.startTime) {
    ElMessage.warning('End time must be after start time')
    return
  }
  submitting.value = true
  try {
    const res = await createBooking({ ...form })
    ElMessage.success(res.status === 'APPROVED' ? 'Confirmed automatically' : 'Submitted — waiting for a teacher')
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
