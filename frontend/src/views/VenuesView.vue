<template>
  <div class="page">
    <div class="toolbar">
      <div class="segmented">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="seg"
          :class="{ active: status === tab.value }"
          @click="setStatus(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="search">
        <el-icon><Search /></el-icon>
        <input
          v-model="keyword"
          type="search"
          placeholder="Search a venue or a building"
          @keyup.enter="reload"
        />
      </div>

      <span class="spacer" />
      <span class="count muted">{{ total }} venues</span>
    </div>

    <div v-loading="loading" class="grid">
      <button v-for="v in venues" :key="v.id" class="venue" @click="open(v.id)">
        <div class="band" :style="bandStyle(v.type)">
          <span class="sport">{{ venueTypeLabel(v.type) }}</span>
          <span class="pill plain" :class="venueStatusOf(v.status).pill">
            {{ venueStatusOf(v.status).label }}
          </span>
        </div>

        <div class="body">
          <h3>{{ v.name }}</h3>
          <div class="chips">
            <span class="chip">{{ v.indoor ? 'Indoor' : 'Outdoor' }}</span>
            <span class="chip">Holds {{ v.capacity }}</span>
          </div>

          <dl class="meta">
            <div>
              <dt><el-icon><Location /></el-icon></dt>
              <dd>{{ [v.building, v.location].filter(Boolean).join(' · ') || 'Campus' }}</dd>
            </div>
            <div>
              <dt><el-icon><Clock /></el-icon></dt>
              <dd class="tabular">{{ formatTime(v.openTime) }} – {{ formatTime(v.closeTime) }}</dd>
            </div>
          </dl>
        </div>

        <div class="foot">
          <span>See availability</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </button>
    </div>

    <div v-if="!loading && !venues.length" class="empty">
      <strong>No venues match that search</strong>
      <span>Try another name, or clear the filter.</span>
    </div>

    <div v-if="total > size" class="pager">
      <button class="btn btn-sm" :disabled="page === 1" @click="go(page - 1)">Previous</button>
      <span class="muted tabular">Page {{ page }} of {{ pages }}</span>
      <button class="btn btn-sm" :disabled="page === pages" @click="go(page + 1)">Next</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Location, Clock, ArrowRight } from '@element-plus/icons-vue'
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

const tabs = [
  { label: 'All', value: '' },
  { label: 'Open', value: 'OPEN' },
  { label: 'Maintenance', value: 'MAINTENANCE' },
  { label: 'Closed', value: 'CLOSED' },
]

const pages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

// Each sport gets its own tint so a grid of cards is scannable at a glance.
const BANDS = {
  BADMINTON: ['#eef1fd', '#dde4fb'],
  SQUASH: ['#fdf3e2', '#fae4c4'],
  TENNIS_INDOOR: ['#e7f7ef', '#cfefdf'],
  TENNIS_OUTDOOR: ['#e8f3ff', '#d3e9ff'],
  FOOTBALL: ['#eef8e8', '#d8efc9'],
  VOLLEYBALL: ['#fdeef1', '#fadde3'],
  BASKETBALL: ['#fff1e6', '#ffe0c7'],
  TABLE_TENNIS: ['#f3eefe', '#e6dcfc'],
  OTHER: ['#f1f3f7', '#e6e9f0'],
}

function bandStyle(type) {
  const [from, to] = BANDS[type] || BANDS.OTHER
  return { background: `linear-gradient(135deg, ${from}, ${to})` }
}

function setStatus(value) {
  status.value = value
  page.value = 1
  reload()
}

function go(p) {
  page.value = p
  reload()
}

async function reload() {
  loading.value = true
  try {
    const data = await listVenues({
      keyword: keyword.value.trim() || undefined,
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

const open = (id) => router.push(`/venues/${id}`)

onMounted(reload)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.segmented {
  display: inline-flex;
  padding: 3px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--surface);
}

.seg {
  height: 30px;
  padding: 0 14px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 13px;
  font-weight: 550;
  cursor: pointer;
}

.seg:hover {
  color: var(--ink);
}

.seg.active {
  background: var(--primary);
  color: #fff;
}

.search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 12px;
  min-width: 280px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--surface);
  color: var(--muted);
}

.search:focus-within {
  border-color: var(--primary);
}

.search input {
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  font-family: inherit;
  font-size: 13.5px;
  color: var(--ink);
}

.count {
  font-size: 12.5px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(272px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.venue {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  text-align: left;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  font-family: inherit;
  transition: transform 0.14s ease, box-shadow 0.14s ease, border-color 0.14s ease;
}

.venue:hover {
  transform: translateY(-2px);
  border-color: #d2d8e2;
  box-shadow: var(--shadow);
}

.band {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  height: 84px;
  padding: 12px 14px;
  overflow: hidden;
}

.band::after {
  content: '';
  position: absolute;
  right: -34px;
  bottom: -58px;
  width: 148px;
  height: 148px;
  border: 1px solid rgba(20, 24, 31, 0.09);
  border-radius: 50%;
  box-shadow: 0 0 0 22px rgba(255, 255, 255, 0.35);
}

.sport {
  align-self: flex-end;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: rgba(20, 24, 31, 0.52);
}

.body {
  padding: 14px 16px 12px;
}

.body h3 {
  font-size: 15.5px;
  margin-bottom: 8px;
}

.chips {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-2);
}

.meta > div {
  display: flex;
  align-items: center;
  gap: 7px;
}

.meta dt {
  display: flex;
  color: var(--faint);
}

.meta dd {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding: 11px 16px;
  border-top: 1px solid var(--line);
  background: var(--surface-2);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--primary-ink);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  font-size: 12.5px;
}
</style>
