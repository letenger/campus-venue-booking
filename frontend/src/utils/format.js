// Enum label / tag-type maps mirroring the Spring Boot backend (com.example.sems.domain.*).

export const VENUE_TYPE_LABELS = {
  VOLLEYBALL: '排球场',
  BADMINTON: '羽毛球场',
  FOOTBALL: '足球场',
  TENNIS_INDOOR: '室内网球场',
  TENNIS_OUTDOOR: '室外网球场',
  SQUASH: '壁球场',
  BASKETBALL: '篮球场',
  TABLE_TENNIS: '乒乓球室',
  OTHER: '其他',
}

export const VENUE_STATUS = {
  OPEN: { label: '开放中', type: 'success' },
  MAINTENANCE: { label: '维护中', type: 'warning' },
  CLOSED: { label: '已关闭', type: 'info' },
}

export const BOOKING_STATUS = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' },
  COMPLETED: { label: '已完成', type: 'success' },
  NO_SHOW: { label: '爽约', type: 'danger' },
}

export const BOOKING_PURPOSE = {
  FREE_PLAY: '自由活动',
  CLASS: '课程/班级活动',
  CLUB_TRAINING: '社团训练',
  EVENT: '赛事活动',
  EXAM: '体育考试',
  OTHER: '其他',
}

export const ROLE_LABELS = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生',
}

export const USER_STATUS = {
  ACTIVE: { label: '正常', type: 'success' },
  DISABLED: { label: '已禁用', type: 'danger' },
}

export function venueTypeLabel(type) {
  return VENUE_TYPE_LABELS[type] || type || '—'
}
export function venueStatusOf(status) {
  return VENUE_STATUS[status] || { label: status || '—', type: 'info' }
}
export function bookingStatusOf(status) {
  return BOOKING_STATUS[status] || { label: status || '—', type: 'info' }
}
export function purposeLabel(purpose) {
  return BOOKING_PURPOSE[purpose] || purpose || '—'
}
export function roleLabel(role) {
  return ROLE_LABELS[role] || role || '—'
}
export function userStatusOf(status) {
  return USER_STATUS[status] || { label: status || '—', type: 'info' }
}

export function formatDateTime(value) {
  if (!value) return '—'
  // value may be an ISO string with offset (Instant) or plain date/time
  const s = String(value)
  return s.replace('T', ' ').replace(/\.\d+.*$/, '').slice(0, 16)
}

export function formatDate(value) {
  if (!value) return '—'
  const s = String(value)
  return s.slice(0, 10)
}

export function formatTime(value) {
  if (!value) return '—'
  const s = String(value)
  return s.slice(11, 16)
}
