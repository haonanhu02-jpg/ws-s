import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from './views/DashboardView.vue'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import GuardView from './views/GuardView.vue'
import DormitoryView from './views/DormitoryView.vue'
import RegistrationAdminView from './views/RegistrationAdminView.vue'
import ReportsView from './views/ReportsView.vue'
import SystemView from './views/SystemView.vue'
import QrView from './views/QrView.vue'

const internalRoles = ['GUARD', 'DORM_ADMIN', 'ADMIN', 'SYSTEM_ADMIN']

const router = createRouter({
  history: createWebHistory('/visitor/'),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/register', component: RegisterView, meta: { public: true } },
    { path: '/qr', component: QrView, meta: { public: true } },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/dashboard', component: DashboardView, meta: { roles: internalRoles } },
    { path: '/guard', component: GuardView, meta: { roles: ['GUARD', 'SYSTEM_ADMIN'] } },
    { path: '/dormitory', component: DormitoryView, meta: { roles: ['DORM_ADMIN', 'SYSTEM_ADMIN'] } },
    { path: '/registrations', component: RegistrationAdminView, meta: { roles: ['ADMIN', 'SYSTEM_ADMIN'] } },
    { path: '/reports', component: ReportsView, meta: { roles: ['ADMIN', 'SYSTEM_ADMIN'] } },
    { path: '/system', component: SystemView, meta: { roles: ['SYSTEM_ADMIN'] } },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach((to) => {
  const rawUser = sessionStorage.getItem('visitor-user')
  if (!to.meta.public && rawUser === null) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!to.meta.public) {
    const role = JSON.parse(rawUser || '{}').role || ''
    const roles = to.meta.roles as string[] | undefined
    if (roles && !roles.includes(role)) return '/dashboard'
  }
})

export default router
