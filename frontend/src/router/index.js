import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/books' },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/books',
    name: 'books',
    component: () => import('../views/BookList.vue'),
    meta: { title: '书籍市场' }
  },
  {
    path: '/books/:id',
    name: 'book-detail',
    component: () => import('../views/BookDetail.vue'),
    meta: { title: '书籍详情' }
  },
  {
    path: '/my-books',
    name: 'my-books',
    component: () => import('../views/MyBooks.vue'),
    meta: { title: '我的发布' }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/OrderList.vue'),
    meta: { title: '我的订单' }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/Profile.vue'),
    meta: { title: '个人中心' }
  },
  {
    path: '/admin/reports',
    name: 'admin-reports',
    component: () => import('../views/AdminReports.vue'),
    meta: { title: '举报管理' }
  },
  {
    // 兜底：所有未匹配的地址都进 404 页面
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFound.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：没有 token 的访问一律回登录页，并记住原地址
router.beforeEach((to) => {
  const token = localStorage.getItem('token')

  if (!to.meta.public && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 已登录再访问登录页，直接进市场
  if (to.name === 'login' && token) {
    return { name: 'books' }
  }

  return true
})

export default router
