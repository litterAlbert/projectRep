<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="admin-info">
        <span class="dot"></span>
        <span class="username-text">管理员 {{ userStore.userInfo.username }} 已登录</span>
        <el-button link type="danger" @click="handleLogout">退出</el-button>
      </div>
      <div class="menu-title">管理功能</div>
      <router-link to="/admin/book" class="menu-item" active-class="active">图书管理</router-link>
      <router-link to="/admin/user" class="menu-item" active-class="active">用户管理</router-link>
      <router-link to="/admin/borrow" class="menu-item" active-class="active">借阅管理</router-link>
      <router-link to="/admin/ai" class="menu-item" active-class="active">AI助手</router-link>
    </aside>

    <main class="main-content">
      <div class="content-body">
        <router-view></router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

/**
 * 退出登录
 * @date 2026-03-28
 */
const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 250px 1fr;
}

.sidebar {
  background: rgba(255, 255, 255, 0.92);
  border-right: 1px solid var(--line);
  backdrop-filter: blur(8px);
  padding: 18px 14px;
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
}


.menu-title {
  margin: 16px 10px 8px;
  color: #94a3b8;
  font-size: 12px;
}

.menu-item {
  width: 100%;
  border: 0;
  background: transparent;
  text-align: left;
  color: #334155;
  padding: 11px 12px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: .2s ease;
  text-decoration: none;
  display: block;
}

.menu-item:hover {
  background: #f1f5f9;
  transform: translateX(2px);
}

.menu-item.active {
  background: var(--primary-soft);
  color: #1d4ed8;
  font-weight: 600;
}

.main-content {
  padding: 20px;
  display: grid;
  grid-template-rows: 1fr;
  height: 100vh;
  overflow: auto;
}

.admin-info {
  background: var(--panel);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  border: 1px solid #edf2f7;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 18px;
}

.username-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #22c55e;
}

.content-body {
  background: var(--panel);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  border: 1px solid #edf2f7;
  padding: 18px;
  overflow: auto;
}

@media (max-width: 1200px) {
  .app-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: relative;
    height: auto;
  }
}
</style>
