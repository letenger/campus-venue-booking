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
  OPEN: { label: 'Open', type: 'success' },
  MAINTENANCE: { label: 'Maintenance', type: 'warning' },
  CLOSED: { label: 'Closed', type: 'info' },
}

export const BOOKING_STATUS = {
  PENDING: { label: 'Pending', type: 'warning' },
  APPROVED: { label: 'Approved', type: 'success' },
  REJECTED: { label: 'Rejected', type: 'danger' },
  CANCELLED: { label: 'Cancelled', type: 'info' },
  COMPLETED: { label: 'Completed', type: 'success' },
  NO_SHOW: { label: 'No-show', type: 'danger' },
}

export const BOOKING_PURPOSE = {
  FREE_PLAY: 'Free play',
  CLASS: 'Class activity',
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
  ACTIVE: { label: 'Active', type: 'success' },
  DISABLED: { label: 'Disabled', type: 'danger' },
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
