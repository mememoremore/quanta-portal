import { portalRoutes } from './portal-routes'

export const staticRoutes = [
  {
    path: '/login',
    redirect: '/admin/login',
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '管理端登录', public: true, portalAudience: 'admin' },
  },
  {
    path: '/login/freshman',
    name: 'FreshmanLogin',
    component: () => import('@/views/portal-login/index.vue'),
    meta: { title: '新生登录', public: true, portalAudience: 'freshman' },
  },
  {
    path: '/login/member',
    name: 'MemberLogin',
    component: () => import('@/views/portal-login/index.vue'),
    meta: { title: '塔员登录', public: true, portalAudience: 'member' },
  },
  {
    path: '/',
    name: 'Entry',
    component: () => import('@/views/entry/index.vue'),
    meta: { title: 'Quanta 门户', public: true },
  },
  {
    path: '/admin',
    name: 'Root',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/admin/dashboard',
    children: [],
  },
  ...portalRoutes,
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', public: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' },
  },
]
