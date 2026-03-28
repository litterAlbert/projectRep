<template>
  <div class="auth-container">
    <div class="auth-box">
      <div class="auth-header">
        <h2>智慧图书管理系统</h2>
        <p>欢迎回来，请登录</p>
      </div>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" size="large">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
        <div class="auth-links">
          <router-link to="/register">没有账号？去注册</router-link>
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
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #edf3ff 0%, #f6fbff 45%, #f7f8ff 100%);
}

.auth-box {
  width: 400px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  padding: 40px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  border: 1px solid var(--line);
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
}

.auth-header h2 {
  margin: 0;
  font-size: 24px;
  color: var(--text);
}

.auth-header p {
  margin: 10px 0 0;
  font-size: 14px;
  color: var(--muted);
}

.w-full {
  width: 100%;
}

.auth-links {
  text-align: right;
  font-size: 14px;
}

.auth-links a {
  color: var(--primary);
  text-decoration: none;
}

.auth-links a:hover {
  text-decoration: underline;
}
</style>
