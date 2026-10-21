// Enum labels mirroring com.campus.venue.domain, plus small date helpers.

export const VENUE_TYPE_LABELS = {
  VOLLEYBALL: 'Volleyball court',
  BADMINTON: 'Badminton court',
  FOOTBALL: 'Football pitch',
  TENNIS_INDOOR: 'Indoor tennis court',
  TENNIS_OUTDOOR: 'Outdoor tennis court',
  SQUASH: 'Squash court',
  BASKETBALL: 'Basketball court',
  TABLE_TENNIS: 'Table tennis room',
  OTHER: 'Other',
}

export const VENUE_STATUS = {
  OPEN: { label: 'Open', pill: 'pill-green' },
  MAINTENANCE: { label: 'Maintenance', pill: 'pill-amber' },
  CLOSED: { label: 'Closed', pill: 'pill-slate' },
}

export const BOOKING_STATUS = {
  PENDING: { label: 'Pending', pill: 'pill-amber' },
  APPROVED: { label: 'Approved', pill: 'pill-green' },
  REJECTED: { label: 'Rejected', pill: 'pill-rose' },
  CANCELLED: { label: 'Cancelled', pill: 'pill-slate' },
  COMPLETED: { label: 'Completed', pill: 'pill-blue' },
  NO_SHOW: { label: 'No-show', pill: 'pill-rose' },
}

export const BOOKING_PURPOSE = {
  FREE_PLAY: 'Free play',
  PE_CLASS: 'PE class',
  CLUB_TRAINING: 'Club training',
  EVENT: 'School event',
  EXAM: 'PE exam',
  OTHER: 'Other',
}

export const ROLE_LABELS = {
  ADMIN: 'Admin',
  TEACHER: 'Teacher',
  STUDENT: 'Student',
}

export const USER_STATUS = {
  ACTIVE: { label: 'Active', pill: 'pill-green' },
  DISABLED: { label: 'Disabled', pill: 'pill-slate' },
}

export function venueTypeLabel(type) {
  return VENUE_TYPE_LABELS[type] || type || '—'
}
export function venueStatusOf(status) {
  return VENUE_STATUS[status] || { label: status || '—', pill: 'pill-slate' }
}
export function bookingStatusOf(status) {
  return BOOKING_STATUS[status] || { label: status || '—', pill: 'pill-slate' }
}
export function purposeLabel(purpose) {
  return BOOKING_PURPOSE[purpose] || purpose || '—'
}
export function roleLabel(role) {
  return ROLE_LABELS[role] || role || '—'
}
export function userStatusOf(status) {
  return USER_STATUS[status] || { label: status || '—', pill: 'pill-slate' }
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

// "08:00:00" (LocalTime) or "2026-10-21T08:00:00Z" (Instant) -> "08:00"
export function formatTime(value) {
  if (!value) return '—'
  const s = String(value)
  const m = s.match(/(\d{2}:\d{2})/)
  return m ? m[1] : s
}
