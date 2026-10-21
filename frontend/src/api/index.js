import http from './http'

// ---------- Auth ----------
export const authLogin = (username, password) =>
  http.post('/auth/login', { username, password })

export const authRegister = (payload) => http.post('/auth/register', payload)
export const authMe = () => http.get('/auth/me')

// ---------- Venues ----------
export const listVenues = (params) => http.get('/venues', { params })
export const getVenue = (id) => http.get(`/venues/${id}`)
export const getAvailability = (id, from, to) =>
  http.get(`/venues/${id}/availability`, { params: { from, to } })
export const createVenue = (payload) => http.post('/venues', payload)
export const updateVenue = (id, payload) => http.put(`/venues/${id}`, payload)
export const closeVenue = (id) => http.delete(`/venues/${id}`)

// ---------- Bookings ----------
export const listBookings = (params) => http.get('/bookings', { params })
export const getBooking = (id) => http.get(`/bookings/${id}`)
export const createBooking = (payload) => http.post('/bookings', payload)
export const cancelBooking = (id) => http.post(`/bookings/${id}/cancel`)
export const approveBooking = (id) => http.post(`/bookings/${id}/approve`)
export const rejectBooking = (id, reason) =>
  http.post(`/bookings/${id}/reject`, reason ? { reason } : {})
export const checkInBooking = (id) => http.post(`/bookings/${id}/check-in`)
export const markNoShowBooking = (id) => http.post(`/bookings/${id}/no-show`)

// ---------- Equipment ----------
export const listEquipment = () => http.get('/equipment')
export const borrowEquipment = (payload) => http.post('/equipment/borrow', payload)
export const returnEquipment = (id) => http.post(`/equipment/${id}/return`)

// ---------- Organizations ----------
export const listClasses = () => http.get('/classes')
export const listClubs = () => http.get('/clubs')

// ---------- Notifications ----------
export const listNotifications = () => http.get('/notifications')
export const markNotificationRead = (id) => http.post(`/notifications/${id}/read`)
export const markAllNotificationsRead = () => http.post('/notifications/read-all')

// ---------- Reports ----------
export const utilizationReport = (params) => http.get('/reports/utilization', { params })
export const bookingSummary = (params) => http.get('/reports/bookings-summary', { params })

// ---------- Admin ----------
export const listUsers = (params) => http.get('/admin/users', { params })
export const setUserStatus = (id, status) =>
  http.patch(`/admin/users/${id}`, null, { params: { status } })
