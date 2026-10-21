import { createRouter, createWebHistory } from 'vue-router'
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
  { path: '/login', name: 'login', component: LoginView, meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: 'venues',
        name: 'venues',
        component: VenuesView,
        meta: { title: '场馆一览', icon: 'House' },
      },
      {
        path: 'venues/:id',
        name: 'venue-detail',
        component: VenueDetailView,
        meta: { title: '场馆详情与预订', icon: 'Calendar' },
      },
      {
        path: 'bookings',
        name: 'my-bookings',
        component: MyBookingsView,
        meta: { title: '我的预订', icon: 'Tickets' },
      },
      {
        path: 'approvals',
        name: 'approvals',
        component: ApprovalsView,
        meta: { title: '预订审批', icon: 'Stamp', roles: ['TEACHER', 'ADMIN'] },
      },
      {
        path: 'admin/venues',
        name: 'admin-venues',
        component: AdminVenuesView,
        meta: { title: '场馆管理', icon: 'OfficeBuilding', roles: ['ADMIN'] },
      },
      {
        path: 'admin/users',
        name: 'admin-users',
        component: AdminUsersView,
        meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'] },
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: NotificationsView,
        meta: { title: '通知中心', icon: 'Bell' },
      },
      {
        path: 'reports',
        name: 'reports',
        component: ReportsView,
        meta: { title: '使用报表', icon: 'DataLine', roles: ['TEACHER', 'ADMIN'] },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

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
