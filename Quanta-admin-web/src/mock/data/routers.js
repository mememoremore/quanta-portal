export const mockRouters = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: 'dashboard/index',
    meta: {
      title: '控制台',
      icon: 'DataAnalysis',
      group: '总览',
      permission: 'qt:dashboard:stats',
    },
  },
  {
    path: '/metrics',
    name: 'AccessMetrics',
    component: 'metrics/index',
    meta: {
      title: '访问指标',
      icon: 'Odometer',
      group: '总览',
      permission: 'qt:metrics:list',
    },
  },
  {
    path: '/members',
    name: 'Members',
    component: 'members/index',
    meta: {
      title: '成员管理',
      icon: 'UserFilled',
      group: '成员管理',
      permission: 'qt:member:list',
    },
  },
  {
    path: '/recruitment',
    name: 'Recruitment',
    component: 'recruitment/index',
    meta: {
      title: '招新管理',
      icon: 'Tickets',
      group: '招生端',
      permission: 'qt:interview:admin:list',
    },
  },
  {
    path: '/activity-signups',
    name: 'ActivitySignups',
    component: 'activity-signups/index',
    meta: {
      title: '活动报名',
      icon: 'List',
      group: '塔员端',
      permission: 'qt:signup:list',
    },
  },
  {
    path: '/learning-materials',
    name: 'LearningMaterials',
    component: 'learning-materials/index',
    meta: {
      title: '学习资料',
      icon: 'Notebook',
      group: '塔员端',
      permission: 'qt:material:list',
    },
  },
  {
    path: '/book-borrows',
    name: 'BookBorrows',
    component: 'book-borrows/index',
    meta: {
      title: '图书借阅',
      icon: 'Reading',
      group: '塔员端',
      permission: 'qt:borrow:list',
    },
  },
  {
    path: '/clothing-orders',
    name: 'ClothingOrders',
    component: 'clothing-orders/index',
    meta: {
      title: '塔服订购',
      icon: 'ShoppingBag',
      group: '塔员端',
      permission: 'qt:order:list',
    },
  },
]
