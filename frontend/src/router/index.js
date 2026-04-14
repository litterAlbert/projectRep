import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/Register.vue')
    },
    {
      path: '/admin',
      component: () => import('../layout/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'admin' },
      redirect: '/admin/book',
      children: [
        {
          path: 'book',
          name: 'adminBook',
          component: () => import('../views/admin/BookManage.vue')
        },
        {
          path: 'user',
          name: 'adminUser',
          component: () => import('../views/admin/UserManage.vue')
        },
        {
          path: 'borrow',
          name: 'adminBorrow',
          component: () => import('../views/admin/BorrowManage.vue')
        },
        {
          path: 'ai',
          name: 'adminAI',
          component: () => import('../views/admin/AIAssistant.vue')
        },
        {
          path: 'knowledge',
          name: 'adminKnowledge',
          component: () => import('../views/admin/KnowledgeManage.vue')
        }
      ]
    },
    {
      path: '/user',
      component: () => import('../layout/UserLayout.vue'),
      meta: { requiresAuth: true, role: 'user' },
      redirect: '/user/borrow',
      children: [
        {
          path: 'borrow',
          name: 'userBorrow',
          component: () => import('../views/user/BookBorrow.vue')
        },
        {
          path: 'return',
          name: 'userReturn',
          component: () => import('../views/user/BookReturn.vue')
        },
        {
          path: 'profile',
          name: 'userProfile',
          component: () => import('../views/user/Profile.vue')
        },
        {
          path: 'ai',
          name: 'userAI',
          component: () => import('../views/user/AIAssistant.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth) {
    if (!userStore.token) {
      next('/login')
    } else {
      if (to.meta.role && userStore.userInfo.role !== to.meta.role) {
        if (userStore.userInfo.role === 'admin') {
          next('/admin')
        } else {
          next('/user')
        }
      } else {
        next()
      }
    }
  } else {
    // If logged in, redirect away from login/register
    if (userStore.token && (to.path === '/login' || to.path === '/register')) {
      if (userStore.userInfo.role === 'admin') {
        next('/admin')
      } else {
        next('/user')
      }
    } else {
      next()
    }
  }
})

export default router
