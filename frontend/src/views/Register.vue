<template>
  <div class="auth-container">
    <!-- 背景装饰元素 -->
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>
    
    <div class="auth-box glass-card">
      <div class="auth-header">
        <div class="logo-circle">
          <el-icon><UserFilled /></el-icon>
        </div>
        <h2>注册账号</h2>
        <p>智慧图书管理系统</p>
      </div>
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef" size="large" label-width="80px" class="auth-form" label-position="top">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" prefix-icon="User"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号" prefix-icon="Phone"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" prefix-icon="Lock" show-password></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="registerForm.gender">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-radio-group v-model="registerForm.role">
                <el-radio label="user">普通用户</el-radio>
                <el-radio label="admin">管理员</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" class="w-full btn-login" :loading="loading" @click="handleRegister">注 册</el-button>
        </el-form-item>
        <div class="auth-links">
          <router-link to="/login"><el-icon><ArrowLeft /></el-icon> 已有账号？去登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  gender: '男',
  role: 'user'
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

/**
 * 处理注册
 * @date 2026-03-28
 */
const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const payload = {
          username: registerForm.username,
          password: registerForm.password,
          phone: registerForm.phone,
          gender: registerForm.gender,
          role: registerForm.role
        }
        const res = await request.post('/user/register', payload)
        if (res.code === 200) {
          ElMessage.success('注册成功，请登录')
          router.push('/login')
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
  width: 600px;
  padding: 40px 48px;
  animation: fadeIn 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-circle {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, var(--primary), #818cf8);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: white;
  font-size: 28px;
  box-shadow: var(--shadow-glow);
}

.auth-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.5px;
}

.auth-header p {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--muted);
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 4px !important;
  font-weight: 500;
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
  margin-top: 20px;
}

.w-full {
  width: 100%;
}

.auth-links {
  text-align: center;
  font-size: 14px;
  margin-top: 16px;
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
  transform: translateX(-4px);
}
</style>
