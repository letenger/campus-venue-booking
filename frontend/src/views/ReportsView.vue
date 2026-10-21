<template>
  <div class="page">
    <div class="toolbar">
      <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
        :clearable="false" start-placeholder="From" end-placeholder="To"
        style="width: 260px" @change="load" />
      <button class="btn btn-sm" @click="load">
        <el-icon><Refresh /></el-icon> Refresh
      </button>
      <span class="spacer" />
      <span class="muted">Utilisation is completed bookings over all bookings.</span>
    </div>

    <div class="tiles">
      <div class="tile card">
        <span class="tile-label">Bookings</span>
        <strong>{{ summary.total || 0 }}</strong>
        <span class="tile-foot muted">{{ pendingText }}</span>
      </div>
      <div class="tile card">
        <span class="tile-label">Completed</span>
        <strong>{{ totalCompleted }}</strong>
        <span class="tile-foot muted">checked in on time</span>
      </div>
      <div class="tile card">
        <span class="tile-label">No-shows</span>
        <strong>{{ totalNoShow }}</strong>
        <span class="tile-foot" :class="totalNoShow ? 'danger' : 'muted'">
          {{ totalNoShow ? 'venues sat empty' : 'none — well used' }}
        </span>
      </div>
      <div class="tile card">
        <span class="tile-label">Average utilisation</span>
        <strong>{{ averageRate }}%</strong>
        <span class="tile-foot muted">across {{ util.length }} venues</span>
      </div>
    </div>

    <div class="card" v-loading="loading">
      <div class="card-head">
        <h3>By venue</h3>
        <div class="legend">
          <span><i class="sw ok" /> completed</span>
          <span><i class="sw miss" /> no-show</span>
          <span><i class="sw off" /> cancelled</span>
        </div>
      </div>

      <div class="rows">
        <div v-for="row in util" :key="row.venueId" class="vrow">
          <div class="vname">
            <strong>{{ row.venueName }}</strong>
            <span class="muted">{{ venueTypeLabel(row.type) }}</span>
          </div>

          <div class="bar" :title="`${row.completed} of ${row.total} completed`">
            <span class="seg ok" :style="{ width: pct(row.completed, row.total) }" />
            <span class="seg miss" :style="{ width: pct(row.noShow, row.total) }" />
            <span class="seg off" :style="{ width: pct(row.cancelled, row.total) }" />
          </div>

          <div class="nums tabular">
            <span class="rate">{{ row.utilizationRate }}%</span>
            <span class="muted">{{ row.total }} total</span>
          </div>
        </div>

        <div v-if="!util.length && !loading" class="empty">
          <strong>No bookings in this range</strong>
          <span>Try a wider window.</span>
        </div>
      </div>
    </div>

    <div v-if="topVenues.length" class="card card-pad">
      <p class="section-title">Most booked</p>
      <div class="top">
        <div v-for="(v, i) in topVenues" :key="v.name" class="top-item">
          <span class="rank">{{ i + 1 }}</span>
          <span class="top-name">{{ v.name }}</span>
          <span class="muted tabular">{{ v.count }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { utilizationReport, bookingSummary } from '../api'
import { venueTypeLabel } from '../utils/format'

const loading = ref(false)
const range = ref([lastMonth(), todayStr()])
const util = ref([])
const summary = ref({})

const totalCompleted = computed(() => util.value.reduce((n, r) => n + (r.completed || 0), 0))
const totalNoShow = computed(() => util.value.reduce((n, r) => n + (r.noShow || 0), 0))
const averageRate = computed(() => {
  if (!util.value.length) return 0
  const sum = util.value.reduce((n, r) => n + (r.utilizationRate || 0), 0)
  return Math.round((sum / util.value.length) * 10) / 10
})

const pendingText = computed(() => {
  const pending = summary.value.byStatus?.PENDING || 0
  return pending ? `${pending} still pending` : 'all decided'
})

const topVenues = computed(() =>
  Object.entries(summary.value.topVenues || {})
    .map(([name, count]) => ({ name, count }))
    .slice(0, 5),
)

function pct(part, total) {
  if (!total) return '0%'
  return `${Math.round((part / total) * 100)}%`
}

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
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12.5px;
}

.tiles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.tile {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 16px 18px;
}

.tile-label {
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--faint);
}

.tile strong {
  font-size: 26px;
  font-weight: 650;
  line-height: 1.15;
}

.tile-foot {
  font-size: 12px;
}

.danger {
  color: var(--rose);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px 6px;
}

.card-head h3 {
  font-size: 15.5px;
}

.legend {
  display: flex;
  gap: 14px;
  font-size: 11.5px;
  color: var(--muted);
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.sw {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}

.sw.ok { background: var(--green); }
.sw.miss { background: var(--rose); }
.sw.off { background: var(--line-strong); }

.rows {
  padding: 8px 22px 20px;
}

.vrow {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 130px;
  gap: 18px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}

.vrow:last-of-type {
  border-bottom: 0;
}

.vname {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.vname strong {
  font-size: 13.5px;
  font-weight: 600;
}

.vname .muted {
  font-size: 11.5px;
}

.bar {
  display: flex;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: var(--slate-soft);
}

.seg {
  height: 100%;
}

.seg.ok { background: var(--green); }
.seg.miss { background: var(--rose); }
.seg.off { background: var(--line-strong); }

.nums {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 8px;
  font-size: 12px;
}

.rate {
  font-size: 13.5px;
  font-weight: 650;
}

.top {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
}

.top-item {
  display: grid;
  grid-template-columns: 24px 1fr auto;
  gap: 10px;
  align-items: center;
  font-size: 13px;
}

.rank {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  font-size: 11px;
  font-weight: 650;
  color: var(--muted);
}

.top-name {
  font-weight: 550;
}

@media (max-width: 820px) {
  .vrow {
    grid-template-columns: 1fr 90px;
    gap: 8px 12px;
  }

  .bar {
    grid-column: 1 / -1;
    order: 3;
  }
}
</style>
