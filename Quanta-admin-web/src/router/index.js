import { createRouter, createWebHistory } from 'vue-router'
import { staticRoutes } from './routes'
import { setupRouterGuard } from './guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

setupRouterGuard(router)

export default router

