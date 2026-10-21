<template>
  <div class="page">
    <button class="back" @click="router.push('/venues')">
      <el-icon><ArrowLeft /></el-icon> All venues
    </button>

    <div v-if="venue" class="head card">
      <div class="head-main">
        <div class="title-line">
          <h2>{{ venue.name }}</h2>
          <span class="pill" :class="venueStatusOf(venue.status).pill">{{ venueStatusOf(venue.status).label }}</span>
        </div>
        <p class="muted">
          {{ venueTypeLabel(venue.type) }} · {{ venue.indoor ? 'Indoor' : 'Outdoor' }} ·
          {{ [venue.building, venue.location].filter(Boolean).join(', ') }}
        </p>
      </div>
      <div class="head-facts">
        <div>
          <span class="fact-label">Holds</span>
          <strong>{{ venue.capacity }}</strong>
        </div>
        <div>
          <span class="fact-label">Open</span>
          <strong class="tabular">{{ formatTime(venue.openTime) }}–{{ formatTime(venue.closeTime) }}</strong>
        </div>
      </div>
    </div>

    <div class="layout">
      <!-- ---------- availability ---------- -->
      <section class="card">
        <header class="card-head">
          <div>
            <h3>Availability</h3>
            <p class="muted">One column per hour. Click a green cell to select it.</p>
          </div>
          <div class="week-nav">
            <button class="btn btn-sm" @click="shiftWeek(-7)">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <span class="tabular">{{ formatDate(days[0]) }} – {{ formatDate(days[days.length - 1]) }}</span>
            <button class="btn btn-sm" @click="shiftWeek(7)">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </header>

        <div class="legend">
          <span><i class="sw free" /> Free</span>
          <span><i class="sw taken" /> Booked</span>
          <span><i class="sw blocked" /> Closed</span>
        </div>

        <div class="grid-wrap">
          <div class="grid-head" :style="gridStyle">
            <span />
            <span v-for="h in hours" :key="h" class="hour tabular">{{ pad(h) }}</span>
          </div>

          <div v-for="day in days" :key="day" class="grid-row" :style="gridStyle">
            <div class="day">
              <strong>{{ weekday(day) }}</strong>
              <span class="tabular">{{ shortDate(day) }}</span>
            </div>
            <template v-if="dayHasSlots(day)">
              <button
                v-for="h in hours"
                :key="h"
                class="cell"
                :class="cellClass(day, h)"
                :disabled="!isFree(day, h)"
                :title="cellTitle(day, h)"
                @click="pick(day, h)"
              >
                <span v-if="isSelected(day, h)" class="cell-label tabular">
                  {{ formatTime(form.startTime) }}–{{ formatTime(form.endTime) }}
                </span>
              </button>
            </template>
            <span v-else class="closed-strip">Closed all day</span>
          </div>
        </div>

        <p v-if="!slots.length && !loading" class="empty-note muted">
          This venue has no timetable for the selected week.
        </p>
      </section>

      <!-- ---------- booking panel ---------- -->
      <aside class="card panel">
        <h3>Book a slot</h3>

        <div class="picked" :class="{ empty: !form.startTime }">
          <template v-if="form.startTime">
            <span class="picked-label">Selected</span>
            <strong class="tabular">{{ formatTime(form.startTime) }} – {{ formatTime(form.endTime) }}</strong>
            <span class="muted">{{ longDate(form.bookingDate) }}</span>
          </template>
          <template v-else>
            <span class="picked-label">No slot yet</span>
            <strong>Pick a green cell</strong>
            <span class="muted">Then confirm the details below.</span>
          </template>
        </div>

        <label class="field">
          <span>Date</span>
          <el-date-picker v-model="form.bookingDate" type="date" value-format="YYYY-MM-DD"
            :disabled-date="outsideRange" :clearable="false" style="width: 100%" @change="syncSelection" />
        </label>

        <div class="two">
          <label class="field">
            <span>From</span>
            <el-time-select v-model="form.startTime" :start="windowStart" :step="'01:00'"
              :end="windowEnd" placeholder="From" style="width: 100%" />
          </label>
          <label class="field">
            <span>To</span>
            <el-time-select v-model="form.endTime" :start="form.startTime || windowStart" :step="'01:00'"
              :end="windowEnd" :min-time="form.startTime" placeholder="To" style="width: 100%" />
          </label>
        </div>

        <label class="field">
          <span>Purpose</span>
          <el-select v-model="form.purpose" style="width: 100%">
            <el-option v-for="(label, key) in purposeOptions" :key="key" :label="label" :value="key" />
          </el-select>
        </label>

        <label class="field">
          <span>People</span>
          <el-input-number v-model="form.attendeesCount" :min="1" :max="venue?.capacity || 50"
            controls-position="right" style="width: 100%" />
        </label>

        <label v-if="classes.length" class="field">
          <span>Class <em class="muted">optional</em></span>
          <el-select v-model="form.classId" clearable placeholder="None" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </label>

        <label v-if="clubs.length" class="field">
          <span>Club <em class="muted">optional</em></span>
          <el-select v-model="form.clubId" clearable placeholder="None" style="width: 100%">
            <el-option v-for="c in clubs" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </label>

        <p class="hint" :class="needsApproval ? 'pill-amber' : 'pill-green'">
          {{ needsApproval ? 'A teacher has to approve this request' : 'Confirms immediately' }}
        </p>

        <button class="btn btn-primary confirm" :disabled="!form.startTime || submitting" @click="submit">
          {{ submitting ? 'Sending…' : 'Request this slot' }}
        </button>

        <p v-if="venue?.rules" class="rules">
          <strong>House rules</strong>
          {{ venue.rules }}
        </p>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getVenue, getAvailability, createBooking, listClasses, listClubs } from '../api'
import {
  venueTypeLabel, venueStatusOf, formatTime, formatDate, BOOKING_PURPOSE,
} from '../utils/format'

const route = useRoute()
const router = useRouter()
const venueId = route.params.id

const venue = ref(null)
const slots = ref([])
const classes = ref([])
const clubs = ref([])
const loading = ref(false)
const submitting = ref(false)
const weekStart = ref(startOfWeek(new Date()))

const form = reactive({
  venueId: Number(venueId),
  bookingDate: iso(new Date()),
  startTime: '',
  endTime: '',
  purpose: 'FREE_PLAY',
  attendeesCount: 1,
  classId: null,
  clubId: null,
})

const purposeOptions = BOOKING_PURPOSE

/* ---------- dates ---------- */

function iso(d) {
  const x = new Date(d)
  x.setMinutes(x.getMinutes() - x.getTimezoneOffset())
  return x.toISOString().slice(0, 10)
}

function startOfWeek(d) {
  const x = new Date(d)
  const shift = (x.getDay() + 6) % 7 // Monday first
  x.setDate(x.getDate() - shift)
  return x
}

const days = computed(() => {
  const list = []
  const d = new Date(weekStart.value)
  for (let i = 0; i < 7; i++) {
    list.push(iso(d))
    d.setDate(d.getDate() + 1)
  }
  return list
})

const shortDate = (value) => value.slice(8, 10) + '/' + value.slice(5, 7)

function weekday(value) {
  const names = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  return names[days.value.indexOf(value)] || ''
}

function longDate(value) {
  const d = new Date(value + 'T00:00:00')
  return d.toLocaleDateString('en-AU', { weekday: 'long', day: 'numeric', month: 'long' })
}

function outsideRange(time) {
  const t = iso(time)
  return t < days.value[0] || t > days.value[6]
}

/* ---------- hours and cells ---------- */

const hour = (value) => Number(formatTime(value).slice(0, 2))

const hours = computed(() => {
  const open = venue.value ? hour(venue.value.openTime) : 8
  const close = venue.value ? hour(venue.value.closeTime) : 21
  return Array.from({ length: Math.max(1, close - open) }, (_, i) => open + i)
})

const gridStyle = computed(() => ({
  gridTemplateColumns: `96px repeat(${hours.value.length}, minmax(34px, 1fr))`,
}))

const byDate = computed(() => {
  const map = {}
  for (const slot of slots.value) {
    const key = slot.date
    const h = hour(slot.startTime)
    if (!map[key]) map[key] = {}
    map[key][h] = slot
  }
  return map
})

const pad = (h) => String(h).padStart(2, '0') + ':00'

function cell(day, h) {
  return byDate.value[day]?.[h]
}

function state(day, h) {
  const slot = cell(day, h)
  if (!slot) return 'closed'
  if (slot.available) return 'free'
  return /booked/i.test(slot.reason || '') ? 'taken' : 'blocked'
}

const isPast = (day) => day < iso(new Date())
const dayHasSlots = (day) => Boolean(byDate.value[day] && Object.keys(byDate.value[day]).length)

const isFree = (day, h) => state(day, h) === 'free' && !isPast(day)

function cellClass(day, h) {
  return [state(day, h), { selected: isSelected(day, h), past: isPast(day) }]
}

function cellTitle(day, h) {
  if (isPast(day)) return 'That day has passed'
  const slot = cell(day, h)
  if (!slot) return 'No timetable on this day'
  return slot.available ? `${pad(h)} is free` : slot.reason || 'Not available'
}

function isSelected(day, h) {
  if (form.bookingDate !== day || !form.startTime) return false
  return hour(form.startTime) === h
}

/* ---------- selection ---------- */

const windowStart = computed(() => formatTime(venue.value?.openTime) || '08:00')
const windowEnd = computed(() => formatTime(venue.value?.closeTime) || '22:00')

function pick(day, h) {
  const slot = cell(day, h)
  if (!slot) return
  form.bookingDate = day
  form.startTime = slot.startTime
  form.endTime = slot.endTime
}

function syncSelection() {
  // keep the picked hour when the user changes the date, if it is still free
  if (!form.startTime) return
  const h = hour(form.startTime)
  const slot = cell(form.bookingDate, h)
  if (slot?.available) {
    form.startTime = slot.startTime
    form.endTime = slot.endTime
  } else {
    form.startTime = ''
    form.endTime = ''
  }
}

const needsApproval = computed(() => {
  if (['PE_CLASS', 'CLUB_TRAINING', 'EVENT', 'EXAM'].includes(form.purpose)) return true
  return Boolean(form.classId || form.clubId) || form.attendeesCount >= 10
})

/* ---------- data ---------- */

function shiftWeek(days_) {
  const d = new Date(weekStart.value)
  d.setDate(d.getDate() + days_)
  weekStart.value = d
  loadAvailability()
}

async function loadAvailability() {
  slots.value = await getAvailability(venueId, days.value[0], days.value[6])
}

async function submit() {
  if (!form.startTime || !form.endTime) {
    ElMessage.warning('Pick a slot first')
    return
  }
  if (form.endTime <= form.startTime) {
    ElMessage.warning('End time must be after start time')
    return
  }
  submitting.value = true
  try {
    const res = await createBooking({ ...form })
    ElMessage.success(
      res.status === 'APPROVED' ? 'Booked — see you there' : 'Sent to a teacher for approval',
    )
    router.push('/bookings')
  } catch {
    // interceptor shows the reason
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  padding: 0;
  border: 0;
  background: none;
  color: var(--muted);
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
}

.back:hover {
  color: var(--ink);
}

/* ---------- head ---------- */

.head {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 24px;
  flex-wrap: wrap;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.head h2 {
  font-size: 21px;
}

.head p {
  margin: 6px 0 0;
  font-size: 13px;
}

.head-facts {
  margin-left: auto;
  display: flex;
  gap: 28px;
}

.head-facts div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.fact-label {
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--faint);
}

.head-facts strong {
  font-size: 14.5px;
  font-weight: 600;
}

/* ---------- layout ---------- */

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: 18px;
  align-items: start;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px 0;
}

.card-head h3 {
  font-size: 15.5px;
}

.card-head p {
  margin: 3px 0 0;
  font-size: 12.5px;
}

.week-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--ink-2);
}

.week-nav .btn {
  width: 30px;
  padding: 0;
}

.legend {
  display: flex;
  gap: 18px;
  padding: 14px 22px 10px;
  font-size: 12px;
  color: var(--muted);
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.sw {
  width: 12px;
  height: 12px;
  border-radius: 4px;
  display: inline-block;
}

.sw.free { background: var(--green-soft); border: 1px solid #b6e6cd; }
.sw.taken { background: var(--rose-soft); border: 1px solid #f3c6d0; }
.sw.blocked { background: var(--slate-soft); border: 1px solid #dde1e8; }

/* ---------- grid ---------- */

.grid-wrap {
  padding: 4px 22px 22px;
  overflow-x: auto;
}

.grid-head,
.grid-row {
  display: grid;
  gap: 3px;
  align-items: center;
}

.grid-head {
  margin-bottom: 4px;
}

.hour {
  font-size: 10.5px;
  color: var(--faint);
  text-align: center;
}

.grid-row {
  margin-bottom: 3px;
}

.day {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  padding-right: 10px;
}

.day strong {
  font-size: 12.5px;
  font-weight: 600;
}

.day span {
  font-size: 11px;
  color: var(--faint);
}

.cell {
  height: 30px;
  border: 1px solid transparent;
  border-radius: 6px;
  font-family: inherit;
  font-size: 10.5px;
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.1s;
  padding: 0;
}

.cell.free {
  background: var(--green-soft);
  border-color: #c4ead6;
}

.cell.free:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(13, 143, 92, 0.22);
}

.cell.taken {
  background: var(--rose-soft);
  border-color: #f4ccd5;
  cursor: not-allowed;
}

.cell.blocked,
.cell.closed {
  background: var(--slate-soft);
  border-color: #e4e7ed;
  cursor: not-allowed;
}

.cell.past {
  opacity: 0.38;
}

.closed-strip {
  grid-column: 2 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 30px;
  border: 1px dashed var(--line-strong);
  border-radius: 6px;
  font-size: 11px;
  color: var(--faint);
}

.cell.selected {
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-soft);
}

.cell-label {
  color: #fff;
  font-size: 10px;
  font-weight: 650;
}

.empty-note {
  padding: 0 22px 22px;
  font-size: 13px;
}

/* ---------- panel ---------- */

.panel {
  position: sticky;
  top: 16px;
  padding: 18px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel h3 {
  font-size: 15.5px;
}

.picked {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--primary-soft);
  border: 1px solid #d8e0fb;
}

.picked.empty {
  background: var(--surface-2);
  border-color: var(--line);
}

.picked-label {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--primary-ink);
}

.picked.empty .picked-label {
  color: var(--faint);
}

.picked strong {
  font-size: 16px;
  font-weight: 650;
}

.picked .muted {
  font-size: 12.5px;
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

.field em {
  font-style: normal;
  font-size: 11px;
}

.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.hint {
  display: block;
  margin: 2px 0 0;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 550;
  text-align: center;
}

.confirm {
  width: 100%;
  height: 40px;
}

.rules {
  margin: 4px 0 0;
  padding-top: 12px;
  border-top: 1px solid var(--line);
  font-size: 12px;
  line-height: 1.55;
  color: var(--muted);
}

.rules strong {
  display: block;
  font-size: 10.5px;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--faint);
  margin-bottom: 3px;
}

@media (max-width: 1080px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .panel {
    position: static;
  }
}
</style>
