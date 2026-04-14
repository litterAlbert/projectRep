<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar glass-card">
      <div class="brand">
        <div class="brand-icon">
          <el-icon><Reading /></el-icon>
        </div>
        <span class="brand-text">管理员控制台</span>
      </div>
      
      <div class="admin-info">
        <div class="avatar-wrap">
          <span class="avatar">A</span>
          <span class="dot"></span>
        </div>
        <div class="info-text">
          <span class="username-text">{{ userStore.userInfo.username }}</span>
          <span class="role-text">超级管理员</span>
        </div>
        <el-button circle plain type="danger" @click="handleLogout" class="logout-btn" title="退出">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
      
      <div class="menu-title">管理功能</div>
      <div class="menu-list">
        <router-link to="/admin/book" class="menu-item" active-class="active">
          <el-icon><Notebook /></el-icon>图书管理
        </router-link>
        <router-link to="/admin/user" class="menu-item" active-class="active">
          <el-icon><User /></el-icon>用户管理
        </router-link>
        <router-link to="/admin/borrow" class="menu-item" active-class="active">
          <el-icon><Collection /></el-icon>借阅管理
        </router-link>
        <router-link to="/admin/ai" class="menu-item" active-class="active">
          <el-icon><ChatDotRound /></el-icon>AI助手
        </router-link>
        <router-link to="/admin/knowledge" class="menu-item" active-class="active">
          <el-icon><Files /></el-icon>知识库管理
        </router-link>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <div class="content-body glass-card">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { Reading, Notebook, User, Collection, ChatDotRound, SwitchButton, Files } from '@element-plus/icons-vue'

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
  display: flex;
  background: var(--bg-gradient);
  padding: 16px;
  gap: 16px;
  box-sizing: border-box;
}

.sidebar {
  width: 260px;
  height: calc(100vh - 32px);
  position: sticky;
  top: 16px;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--line);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px 24px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 24px;
}

.brand-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary), #818cf8);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  box-shadow: var(--shadow-glow);
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.5px;
}

.admin-info {
  background: var(--panel-solid);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  border: 1px solid var(--line-solid);
}

.avatar-wrap {
  position: relative;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
}

.dot {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #10b981;
  border: 2px solid #fff;
}

.info-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.username-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-text {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.logout-btn {
  border: none;
  background: #fee2e2;
  color: var(--danger);
}
.logout-btn:hover {
  background: var(--danger);
  color: white;
}

.menu-title {
  margin: 0 12px 12px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  color: var(--text-regular);
  font-size: 15px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.8);
  color: var(--primary);
  transform: translateX(4px);
}

.menu-item.active {
  background: var(--primary);
  color: white;
  box-shadow: var(--shadow-glow);
}

.main-content {
  flex: 1;
  height: calc(100vh - 32px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-body {
  flex: 1;
  background: var(--panel-solid);
  border-radius: var(--radius-xl);
  padding: 24px;
  overflow: auto;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--line);
}

/* 页面切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

@media (max-width: 1024px) {
  .app-layout {
    flex-direction: column;
    height: auto;
    overflow: auto;
  }

  .sidebar {
    width: 100%;
    height: auto;
    position: relative;
    top: 0;
  }

  .main-content {
    height: auto;
    min-height: 500px;
  }
}
</style>
