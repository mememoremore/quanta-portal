export const unknownRouteComponent = () => import('@/views/error/ComponentError.vue')

const componentMap = {
  Layout: () => import('@/layout/AppLayout.vue'),
  'dashboard/index': () => import('@/views/dashboard/index.vue'),
  'metrics/index': () => import('@/views/metrics/index.vue'),
  'members/index': () => import('@/views/members/index.vue'),
  'recruitment/index': () => import('@/views/recruitment/index.vue'),
  'activity-signups/index': () => import('@/views/activity-signups/index.vue'),
  'book-borrows/index': () => import('@/views/book-borrows/index.vue'),
  'learning-materials/index': () => import('@/views/learning-materials/index.vue'),
  'clothing-orders/index': () => import('@/views/clothing-orders/index.vue'),
  'placeholder/index': () => import('@/views/placeholder/index.vue'),
}

export function resolveRouteComponent(componentKey) {
  return componentMap[componentKey] || unknownRouteComponent
}

export function isKnownComponent(componentKey) {
  return Boolean(componentMap[componentKey])
}
