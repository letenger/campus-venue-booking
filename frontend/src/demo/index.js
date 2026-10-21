/**
 * Demo backend.
 *
 * The real API is Spring Boot, which Vercel cannot run. For the public preview the app is
 * built with VITE_DEMO=1 and this module replaces the axios adapter with an in-memory
 * implementation of the same endpoints, same envelopes, same status codes. Every screen,
 * rule and transition is the real one — only the persistence is a JavaScript object.
 */
import http from '../api/http'

const KEY = 'campus-venue-demo-v1'
const HOUR = 3600 * 1000

const iso = (d) => {
  const x = new Date(d)
  x.setMinutes(x.getMinutes() - x.getTimezoneOffset())
  return x.toISOString().slice(0, 10)
}
const addDays = (base, n) => {
  const d = new Date(base)
  d.setDate(d.getDate() + n)
  return d
}
const time = (h) => String(h).padStart(2, '0') + ':00:00'

const USERS = [
  { id: 1, username: 'admin', realName: 'Dana Whitfield', role: 'ADMIN', status: 'ACTIVE', studentNo: null },
  { id: 2, username: 'teacher', realName: 'Mei Lin', role: 'TEACHER', status: 'ACTIVE', studentNo: null },
  { id: 3, username: 'student', realName: 'Amy Chen', role: 'STUDENT', status: 'ACTIVE', studentNo: '2026-0412', classId: 1 },
  { id: 4, username: 'jordan', realName: 'Jordan Pike', role: 'STUDENT', status: 'ACTIVE', studentNo: '2026-0388', classId: 2 },
  { id: 5, username: 'nina', realName: 'Nina Rahman', role: 'STUDENT', status: 'ACTIVE', studentNo: '2026-0451', classId: 3 },
  { id: 6, username: 'sam', realName: 'Sam Okafor', role: 'TEACHER', status: 'DISABLED', studentNo: null },
]

const VENUES = [
  { id: 1, name: 'Badminton Court 2', type: 'BADMINTON', indoor: true, building: 'Sports Hall', location: 'Court 2', capacity: 4, open: 8, close: 21, status: 'OPEN', rules: 'Non-marking shoes only' },
  { id: 2, name: 'Squash Court B1', type: 'SQUASH', indoor: true, building: 'Sports Hall', location: 'Basement', capacity: 2, open: 9, close: 22, status: 'OPEN', rules: 'Racket required, borrow from the desk' },
  { id: 3, name: 'Indoor Tennis 1', type: 'TENNIS_INDOOR', indoor: true, building: 'Tennis Centre', location: 'Hall A', capacity: 4, open: 8, close: 22, status: 'OPEN', rules: 'Lights metered separately' },
  { id: 4, name: 'Outdoor Tennis 2', type: 'TENNIS_OUTDOOR', indoor: false, building: 'Tennis Centre', location: 'Court 2', capacity: 4, open: 7, close: 20, status: 'OPEN', rules: 'Closed when it rains' },
  { id: 5, name: 'Football Pitch', type: 'FOOTBALL', indoor: false, building: 'Main Field', location: 'Centre', capacity: 22, open: 8, close: 19, status: 'OPEN', rules: 'Matches take priority over training' },
  { id: 6, name: 'Volleyball Court', type: 'VOLLEYBALL', indoor: false, building: 'Main Field', location: 'East side', capacity: 12, open: 8, close: 19, status: 'MAINTENANCE', rules: 'Used by PE classes most mornings' },
]

const PASSWORDS = {
  admin: 'admin123',
  teacher: 'teacher123',
  student: 'student123',
  jordan: 'student123',
  nina: 'student123',
}

const CLASSES = [
  { id: 1, name: 'Grade 10A', meta: 'Grade 10' },
  { id: 2, name: 'Grade 10B', meta: 'Grade 10' },
  { id: 3, name: 'Grade 11C', meta: 'Grade 11' },
]
const CLUBS = [
  { id: 1, name: 'Badminton Club', meta: 'Sport' },
  { id: 2, name: 'Tennis Academy', meta: 'Sport' },
]

const today = new Date()

function seed() {
  const bookings = [
    {
      id: 1, userId: 3, venueId: 1, bookingDate: iso(addDays(today, -2)), start: 10, end: 11,
      purpose: 'FREE_PLAY', attendees: 2, status: 'COMPLETED', createdAt: hoursAgo(50), checkInAt: hoursAgo(50),
    },
    {
      id: 2, userId: 3, venueId: 3, bookingDate: iso(addDays(today, 1)), start: 16, end: 17,
      purpose: 'FREE_PLAY', attendees: 2, status: 'APPROVED', createdAt: hoursAgo(20),
    },
    {
      id: 3, userId: 4, venueId: 5, bookingDate: iso(addDays(today, 1)), start: 15, end: 17,
      purpose: 'CLUB_TRAINING', attendees: 18, clubId: 1, status: 'PENDING', createdAt: hoursAgo(9),
    },
    {
      id: 4, userId: 5, venueId: 2, bookingDate: iso(addDays(today, 2)), start: 12, end: 13,
      purpose: 'EXAM', attendees: 2, status: 'PENDING', createdAt: hoursAgo(4),
    },
    {
      id: 5, userId: 3, venueId: 4, bookingDate: iso(addDays(today, -4)), start: 9, end: 10,
      purpose: 'FREE_PLAY', attendees: 1, status: 'NO_SHOW', createdAt: hoursAgo(110),
    },
    {
      id: 6, userId: 4, venueId: 3, bookingDate: iso(addDays(today, -1)), start: 18, end: 19,
      purpose: 'PE_CLASS', attendees: 12, classId: 2, status: 'REJECTED',
      rejectReason: 'The hall is set up for the music concert that evening.',
      createdAt: hoursAgo(40),
    },
    {
      id: 7, userId: 3, venueId: 1, bookingDate: iso(today), start: 17, end: 18,
      purpose: 'FREE_PLAY', attendees: 2, status: 'APPROVED', createdAt: hoursAgo(3),
    },
  ]

  const notifications = [
    note(1, 3, 'APPROVED', 'Booking confirmed', 'Indoor Tennis 1 is yours', 2, 20),
    note(2, 3, 'BOOKING_CREATED', 'Booking submitted', 'Badminton Court 2 is waiting for a teacher', 7, 3),
    note(3, 2, 'APPROVAL_REQUIRED', 'Booking needs a decision', 'Football Pitch has a new request', 3, 9),
    note(4, 2, 'APPROVAL_REQUIRED', 'Booking needs a decision', 'Squash Court B1 has a new request', 4, 4),
    note(5, 3, 'NO_SHOW', 'Marked as no-show', 'The venue was not used during the booked time', 5, 100),
    note(6, 4, 'REJECTED', 'Booking rejected', 'Reason: The hall is set up for the music concert that evening.', 6, 40),
  ]

  return { bookings, notifications, users: USERS.map((u) => ({ ...u })), venues: VENUES.map((v) => ({ ...v })) }
}

function hoursAgo(n) {
  return new Date(Date.now() - n * HOUR).toISOString()
}

function note(id, userId, type, title, content, bookingId, hours) {
  return {
    id, userId, type, title, content, relatedBookingId: bookingId,
    read: hours > 30, createdAt: hoursAgo(hours),
  }
}

let state = load()
let nextBookingId = Math.max(0, ...state.bookings.map((b) => b.id)) + 1
let nextNotificationId = Math.max(0, ...state.notifications.map((n) => n.id)) + 1

function load() {
  try {
    const raw = localStorage.getItem(KEY)
    if (raw) return JSON.parse(raw)
  } catch {
    /* ignore a broken snapshot */
  }
  const fresh = seed()
  save(fresh)
  return fresh
}

function save(next = state) {
  try {
    localStorage.setItem(KEY, JSON.stringify(next))
  } catch {
    /* storage full or blocked — the demo still works in memory */
  }
}

/* ---------- helpers ---------- */

const page = (rows, index = 0, size = 20) => ({
  content: rows.slice(index * size, index * size + size),
  page: index,
  size,
  totalElements: rows.length,
  totalPages: Math.ceil(rows.length / size),
  last: (index + 1) * size >= rows.length,
})

const venueOf = (id) => state.venues.find((v) => v.id === Number(id))
const userOf = (id) => state.users.find((u) => u.id === Number(id))
const active = (b) => b.status === 'PENDING' || b.status === 'APPROVED'

function venueDto(v) {
  return {
    id: v.id, name: v.name, type: v.type, indoor: v.indoor, building: v.building,
    location: v.location, capacity: v.capacity, openTime: time(v.open), closeTime: time(v.close),
    status: v.status, rules: v.rules, imageUrls: null,
  }
}

function bookingDto(b, me) {
  const owner = userOf(b.userId)
  return {
    id: b.id,
    userId: b.userId,
    userName: owner?.realName || owner?.username || null,
    venueId: b.venueId,
    venueName: venueOf(b.venueId)?.name || '',
    classId: b.classId || null,
    clubId: b.clubId || null,
    bookingDate: b.bookingDate,
    startTime: time(b.start),
    endTime: time(b.end),
    purpose: b.purpose,
    attendeesCount: b.attendees,
    status: b.status,
    rejectReason: b.rejectReason || null,
    approvedBy: b.approvedBy || null,
    approvedAt: b.approvedAt || null,
    checkInAt: b.checkInAt || null,
    cancelledAt: b.cancelledAt || null,
    createdAt: b.createdAt,
    canCancel: me?.id === b.userId && (b.status === 'PENDING' || b.status === 'APPROVED'),
    canApprove: b.status === 'PENDING',
  }
}

function notify(userId, type, title, content, bookingId) {
  state.notifications.unshift({
    id: nextNotificationId++, userId, type, title, content,
    relatedBookingId: bookingId, read: false, createdAt: new Date().toISOString(),
  })
}

const fail = (status, code, message) => ({ status, code, message })

/* ---------- router ---------- */

const routes = [
  ['POST', /^\/auth\/login$/, ({ body }) => {
    const user = state.users.find((u) => u.username === body.username)
    if (!user || PASSWORDS[user.username] !== body.password) {
      throw fail(401, 'UNAUTHORIZED', 'Invalid credentials')
    }
    if (user.status === 'DISABLED') throw fail(403, 'FORBIDDEN', 'Account is disabled')
    currentUserId = user.id
    return { token: `demo.${user.id}`, user }
  }],
  ['POST', /^\/auth\/logout$/, () => null],
  ['GET', /^\/auth\/me$/, () => userOf(currentUserId)],

  ['GET', /^\/venues$/, ({ query }) => {
    let rows = state.venues
    if (query.status) rows = rows.filter((v) => v.status === query.status)
    if (query.keyword) {
      const k = String(query.keyword).toLowerCase()
      rows = rows.filter((v) => `${v.name} ${v.building}`.toLowerCase().includes(k))
    }
    return page(rows.map(venueDto), Number(query.page || 0), Number(query.size || 12))
  }],
  ['GET', /^\/venues\/(\d+)$/, ({ params }) => {
    const v = venueOf(params[0])
    if (!v) throw fail(404, 'NOT_FOUND', 'Venue not found')
    return venueDto(v)
  }],
  ['GET', /^\/venues\/(\d+)\/availability$/, ({ params, query }) => {
    const v = venueOf(params[0])
    if (!v) throw fail(404, 'NOT_FOUND', 'Venue not found')
    const from = query.from || iso(today)
    const to = query.to || iso(addDays(today, 7))
    const slots = []
    for (let d = new Date(from + 'T00:00:00'); iso(d) <= to; d.setDate(d.getDate() + 1)) {
      const date = iso(d)
      const weekday = d.getDay()
      if (weekday === 0) continue // no Sunday timetable, as seeded on the server
      const close = weekday === 6 ? Math.min(v.close, 12) : v.close // short Saturday
      for (let h = v.open; h < close; h++) {
        const taken = state.bookings.some(
          (b) => b.venueId === v.id && b.bookingDate === date && active(b) && h < b.end && h + 1 > b.start,
        )
        let reason = null
        if (v.status !== 'OPEN') reason = v.status === 'MAINTENANCE' ? 'Venue under maintenance' : 'Venue closed'
        else if (taken) reason = 'Already booked'
        slots.push({ date, startTime: time(h), endTime: time(h + 1), available: !reason, reason })
      }
    }
    return slots
  }],
  ['POST', /^\/venues$/, ({ body }) => {
    const v = { ...body, id: state.venues.length + 1, open: hh(body.openTime), close: hh(body.closeTime) }
    state.venues.push(v)
    save()
    return venueDto(v)
  }],
  ['PUT', /^\/venues\/(\d+)$/, ({ params, body }) => {
    const v = venueOf(params[0])
    Object.assign(v, { ...body, open: hh(body.openTime), close: hh(body.closeTime) })
    save()
    return venueDto(v)
  }],
  ['DELETE', /^\/venues\/(\d+)$/, ({ params }) => {
    venueOf(params[0]).status = 'CLOSED'
    save()
    return null
  }],

  ['GET', /^\/bookings$/, ({ query }) => {
    const me = userOf(currentUserId)
    let rows = [...state.bookings]
    if (me.role === 'STUDENT') rows = rows.filter((b) => b.userId === me.id)
    else if (String(query.mine) === 'true') rows = rows.filter((b) => b.userId === me.id)
    if (query.status) rows = rows.filter((b) => b.status === query.status)
    rows.sort((a, b) => (a.bookingDate < b.bookingDate ? 1 : a.bookingDate > b.bookingDate ? -1 : b.start - a.start))
    return page(rows.map((b) => bookingDto(b, me)), Number(query.page || 0), Number(query.size || 15))
  }],
  ['GET', /^\/bookings\/(\d+)$/, ({ params }) => {
    const b = state.bookings.find((x) => x.id === Number(params[0]))
    if (!b) throw fail(404, 'NOT_FOUND', 'Booking not found')
    return bookingDto(b, userOf(currentUserId))
  }],
  ['POST', /^\/bookings$/, ({ body }) => {
    const venue = venueOf(body.venueId)
    if (!venue) throw fail(404, 'NOT_FOUND', 'Venue not found')
    if (venue.status !== 'OPEN') throw fail(400, 'BAD_REQUEST', 'This venue is not open for booking')

    const start = hh(body.startTime)
    const end = hh(body.endTime)
    if (end <= start) throw fail(400, 'BAD_REQUEST', 'End time must be after start time')
    if (start < venue.open || end > venue.close) throw fail(400, 'BAD_REQUEST', 'Requested time is outside the opening hours')
    if (body.attendeesCount > venue.capacity) throw fail(400, 'BAD_REQUEST', 'That many people do not fit in this venue')
    const clash = state.bookings.some(
      (b) => b.venueId === venue.id && b.bookingDate === body.bookingDate && active(b)
        && start < b.end && end > b.start,
    )
    if (clash) throw fail(409, 'CONFLICT', 'That slot is already booked')

    const needsApproval = ['PE_CLASS', 'CLUB_TRAINING', 'EVENT', 'EXAM'].includes(body.purpose)
      || Boolean(body.classId || body.clubId) || body.attendeesCount >= 10

    const booking = {
      id: nextBookingId++,
      userId: currentUserId,
      venueId: venue.id,
      bookingDate: body.bookingDate,
      start, end,
      purpose: body.purpose,
      attendees: body.attendeesCount,
      classId: body.classId || null,
      clubId: body.clubId || null,
      status: needsApproval ? 'PENDING' : 'APPROVED',
      approvedBy: needsApproval ? null : currentUserId,
      approvedAt: needsApproval ? null : new Date().toISOString(),
      createdAt: new Date().toISOString(),
    }
    state.bookings.push(booking)

    if (needsApproval) {
      state.users.filter((u) => u.role === 'TEACHER').forEach((t) =>
        notify(t.id, 'APPROVAL_REQUIRED', 'Booking needs a decision', `${venue.name} has a new request`, booking.id))
    } else {
      notify(currentUserId, 'APPROVED', 'Booking confirmed', `${venue.name} is yours`, booking.id)
    }
    save()
    return bookingDto(booking, userOf(currentUserId))
  }],
  ['POST', /^\/bookings\/(\d+)\/cancel$/, ({ params }) => transition(params, (b) => {
    b.status = 'CANCELLED'
    b.cancelledAt = new Date().toISOString()
    notify(b.userId, 'CANCELLED', 'Booking cancelled', 'The slot has been released', b.id)
  }, ['PENDING', 'APPROVED'])],
  ['POST', /^\/bookings\/(\d+)\/approve$/, ({ params }) => transition(params, (b) => {
    b.status = 'APPROVED'
    b.approvedBy = currentUserId
    b.approvedAt = new Date().toISOString()
    notify(b.userId, 'APPROVED', 'Booking approved', 'Your request was approved', b.id)
  }, ['PENDING'])],
  ['POST', /^\/bookings\/(\d+)\/reject$/, ({ params, body }) => transition(params, (b) => {
    b.status = 'REJECTED'
    b.rejectReason = body.reason || null
    notify(b.userId, 'REJECTED', 'Booking rejected', `Reason: ${body.reason || 'not given'}`, b.id)
  }, ['PENDING'])],
  ['POST', /^\/bookings\/(\d+)\/check-in$/, ({ params }) => transition(params, (b) => {
    b.status = 'COMPLETED'
    b.checkInAt = new Date().toISOString()
  }, ['APPROVED'])],
  ['POST', /^\/bookings\/(\d+)\/no-show$/, ({ params }) => transition(params, (b) => {
    b.status = 'NO_SHOW'
    notify(b.userId, 'NO_SHOW', 'Marked as no-show', 'The venue was not used during the booked time', b.id)
  }, ['APPROVED'])],

  ['GET', /^\/notifications$/, () => state.notifications
    .filter((n) => n.userId === currentUserId)
    .sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))],
  ['POST', /^\/notifications\/(\d+)\/read$/, ({ params }) => {
    const n = state.notifications.find((x) => x.id === Number(params[0]))
    if (n) n.read = true
    save()
    return null
  }],
  ['POST', /^\/notifications\/read-all$/, () => {
    state.notifications.filter((n) => n.userId === currentUserId).forEach((n) => { n.read = true })
    save()
    return null
  }],

  ['GET', /^\/classes$/, () => CLASSES],
  ['GET', /^\/clubs$/, () => CLUBS],
  ['GET', /^\/equipment$/, () => []],
  ['GET', /^\/equipment\/borrows$/, () => []],

  ['GET', /^\/reports\/utilization$/, ({ query }) => {
    const from = query.from || iso(addDays(today, -30))
    const to = query.to || iso(today)
    return state.venues.map((v) => {
      const rows = state.bookings.filter((b) => b.venueId === v.id && b.bookingDate >= from && b.bookingDate <= to)
      const completed = rows.filter((b) => b.status === 'COMPLETED').length
      const cancelled = rows.filter((b) => b.status === 'CANCELLED').length
      const noShow = rows.filter((b) => b.status === 'NO_SHOW').length
      return {
        venueId: v.id, venueName: v.name, type: v.type,
        total: rows.length, completed, cancelled, noShow,
        utilizationRate: rows.length ? Math.round((completed / rows.length) * 1000) / 10 : 0,
      }
    })
  }],
  ['GET', /^\/reports\/bookings-summary$/, ({ query }) => {
    const from = query.from || iso(addDays(today, -30))
    const to = query.to || iso(today)
    const rows = state.bookings.filter((b) => b.bookingDate >= from && b.bookingDate <= to)
    const byStatus = {}
    const byVenue = {}
    rows.forEach((b) => {
      byStatus[b.status] = (byStatus[b.status] || 0) + 1
      const name = venueOf(b.venueId)?.name || 'Venue'
      byVenue[name] = (byVenue[name] || 0) + 1
    })
    const topVenues = Object.fromEntries(
      Object.entries(byVenue).sort((a, b) => b[1] - a[1]).slice(0, 5),
    )
    return { total: rows.length, byStatus, topVenues }
  }],

  ['GET', /^\/admin\/users$/, ({ query }) => {
    let rows = state.users
    if (query.role) rows = rows.filter((u) => u.role === query.role)
    return page(rows, Number(query.page || 0), Number(query.size || 20))
  }],
  ['PATCH', /^\/admin\/users\/(\d+)$/, ({ params, query }) => {
    const u = userOf(params[0])
    u.status = query.status
    save()
    return u
  }],
]

let currentUserId = 3

const hh = (value) => Number(String(value).slice(0, 2))

function transition(params, apply, allowed) {
  const b = state.bookings.find((x) => x.id === Number(params[0]))
  if (!b) throw fail(404, 'NOT_FOUND', 'Booking not found')
  if (!allowed.includes(b.status)) throw fail(400, 'BAD_REQUEST', 'This booking can no longer make that move')
  apply(b)
  save()
  return bookingDto(b, userOf(currentUserId))
}

export function installDemo(instance) {
  // keep whoever the demo last signed in as, so a reload does not bounce to the login form
  const stored = localStorage.getItem('sems_user')
  if (stored) currentUserId = JSON.parse(stored).id

  instance.defaults.adapter = async (config) => {
    const method = (config.method || 'get').toUpperCase()
    const [path, search] = (config.url || '').split('?')
    const clean = path.startsWith('/') ? path : '/' + path
    const query = { ...Object.fromEntries(new URLSearchParams(search || '')), ...(config.params || {}) }
    const body = typeof config.data === 'string' ? JSON.parse(config.data || '{}') : config.data || {}

    await new Promise((r) => setTimeout(r, 120)) // a little latency so loading states are visible

    for (const [verb, pattern, handler] of routes) {
      if (verb !== method) continue
      const match = clean.match(pattern)
      if (!match) continue
      try {
        const data = handler({ params: match.slice(1), query, body })
        return {
          data: { success: true, data, message: 'ok', timestamp: new Date().toISOString() },
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
        }
      } catch (error) {
        const status = error.status || 500
        return {
          data: {
            success: false,
            error: { code: error.code || 'INTERNAL_ERROR', message: error.message, details: [] },
          },
          status,
          statusText: 'ERROR',
          headers: {},
          config,
        }
      }
    }

    return {
      data: { success: false, error: { code: 'NOT_FOUND', message: `No demo route for ${method} ${clean}`, details: [] } },
      status: 404,
      statusText: 'NOT_FOUND',
      headers: {},
      config,
    }
  }
}
