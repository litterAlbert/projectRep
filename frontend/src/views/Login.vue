<template>
  <div class="auth-container">
    <!-- 背景装饰元素 -->
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>
    
    <div class="auth-box glass-card">
      <div class="auth-header">
        <div class="logo-circle">
          <el-icon><Reading /></el-icon>
        </div>
        <h2>智慧图书管理系统</h2>
        <p>欢迎回来，请登录</p>
      </div>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" size="large" class="auth-form">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="w-full btn-login" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
        <div class="auth-links">
          <router-link to="/register">没有账号？去注册 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * 处理登录
 * @date 2026-03-28
 */
const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await request.post('/user/login', loginForm)
        if (res.code === 200) {
          ElMessage.success('登录成功')
          userStore.setToken(res.data.token)
          userStore.setUserInfo(res.data.user)
          if (res.data.user.role === 'admin') {
            router.push('/admin')
          } else {
            router.push('/user')
          }
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-gradient);
  position: relative;
  overflow: hidden;
}

/* 高级装饰背景 */
.bg-shape {
  position: absolute;
  filter: blur(80px);
  z-index: 0;
  border-radius: 50%;
  animation: float 10s infinite ease-in-out alternate;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: rgba(99, 102, 241, 0.4);
  top: -10%;
  left: -10%;
}

.shape-2 {
  width: 500px;
  height: 500px;
  background: rgba(168, 85, 247, 0.3);
  bottom: -20%;
  right: -10%;
  animation-delay: -5s;
}

@keyframes float {
  0% { transform: translateY(0) scale(1); }
  100% { transform: translateY(30px) scale(1.1); }
}

.auth-box {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 48px;
  animation: fadeIn 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-circle {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, var(--primary), #818cf8);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: white;
  font-size: 32px;
  box-shadow: var(--shadow-glow);
}

.auth-header h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.5px;
}

.auth-header p {
  margin: 8px 0 0;
  font-size: 15px;
  color: var(--muted);
}

.auth-form :deep(.el-input__wrapper) {
  padding: 8px 15px;
  background: rgba(255, 255, 255, 0.6) !important;
}

.btn-login {
  height: 48px;
  font-size: 16px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--primary), #6366f1);
  border: none;
  margin-top: 10px;
}

.w-full {
  width: 100%;
}

.auth-links {
  text-align: center;
  font-size: 14px;
  margin-top: 24px;
}

.auth-links a {
  color: var(--primary);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
  transition: all 0.3s;
}

.auth-links a:hover {
  color: var(--primary-hover);
  transform: translateX(4px);
}
</style>
