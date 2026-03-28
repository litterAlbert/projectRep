<template>
  <div class="auth-container">
    <div class="auth-box">
      <div class="auth-header">
        <h2>注册账号</h2>
        <p>智慧图书管理系统</p>
      </div>
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef" size="large" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="registerForm.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- 默认注册为普通用户，如果需要注册管理员可以加个选项，这里按需求文档默认支持角色注册，为了方便测试开放角色选择 -->
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="registerForm.role">
            <el-radio label="user">普通用户</el-radio>
            <el-radio label="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="w-full" :loading="loading" @click="handleRegister">注 册</el-button>
        </el-form-item>
        <div class="auth-links">
          <router-link to="/login">已有账号？去登录</router-link>
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
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #edf3ff 0%, #f6fbff 45%, #f7f8ff 100%);
}

.auth-box {
  width: 500px;
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
