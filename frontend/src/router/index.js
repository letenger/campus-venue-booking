import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router'
import { authState, isAuthenticated, homeForRole } from '../store/auth'

import LoginView from '../views/LoginView.vue'
import AppLayout from '../components/AppLayout.vue'
import VenuesView from '../views/VenuesView.vue'
import VenueDetailView from '../views/VenueDetailView.vue'
import MyBookingsView from '../views/MyBookingsView.vue'
import ApprovalsView from '../views/ApprovalsView.vue'
import AdminVenuesView from '../views/AdminVenuesView.vue'
import AdminUsersView from '../views/AdminUsersView.vue'
import NotificationsView from '../views/NotificationsView.vue'
import ReportsView from '../views/ReportsView.vue'

const routes = [
  { path: '/', redirect: '/venues' },
  { path: '/login', name: 'login', component: LoginView, meta: { public: true, title: 'Sign in', subtitle: 'Campus sports venue booking' } },
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: 'venues',
        name: 'venues',
        component: VenuesView,
        meta: { title: 'Venues', subtitle: 'Find a court or a pitch and see what is free', icon: 'House' },
      },
      {
        path: 'venues/:id',
        name: 'venue-detail',
        component: VenueDetailView,
        meta: { title: 'Venue detail', subtitle: 'Pick a slot and send the request', icon: 'Calendar' },
      },
      {
        path: 'bookings',
        name: 'my-bookings',
        component: MyBookingsView,
        meta: { title: 'My bookings', subtitle: 'Everything you booked, and what happened to it', icon: 'Tickets' },
      },
      {
        path: 'approvals',
        name: 'approvals',
        component: ApprovalsView,
        meta: { title: 'Approvals', subtitle: 'Requests waiting for a teacher', icon: 'Stamp', roles: ['TEACHER', 'ADMIN'] },
      },
      {
        path: 'admin/venues',
        name: 'admin-venues',
        component: AdminVenuesView,
        meta: { title: 'Manage venues', subtitle: 'Catalogue, opening hours and weekly timetable', icon: 'OfficeBuilding', roles: ['ADMIN'] },
      },
      {
        path: 'admin/users',
        name: 'admin-users',
        component: AdminUsersView,
        meta: { title: 'Users', subtitle: 'Accounts and access', icon: 'User', roles: ['ADMIN'] },
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: NotificationsView,
        meta: { title: 'Notifications', subtitle: 'What changed since you last looked', icon: 'Bell' },
      },
      {
        path: 'reports',
        name: 'reports',
        component: ReportsView,
        meta: { title: 'Reports', subtitle: 'How much each venue is actually used', icon: 'DataLine', roles: ['TEACHER', 'ADMIN'] },
      },
    ],
  },
]

// The demo build is hosted as plain static files, so it uses hash routes and never
// needs a server that rewrites deep links to index.html.
const history = import.meta.env.VITE_DEMO === '1' ? createWebHashHistory() : createWebHistory()

const router = createRouter({ history, routes })

router.beforeEach((to) => {
  if (to.meta.public) {
    if (isAuthenticated.value) return homeForRole(authState.user?.role)
    return true
  }
  if (!isAuthenticated.value) return '/login'
  if (to.meta.roles && !to.meta.roles.includes(authState.user?.role)) {
    return homeForRole(authState.user?.role)
  }
  return true
})

export default router
