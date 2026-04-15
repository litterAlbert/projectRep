<template>
  <div class="profile">
    <h2>个人信息</h2>

    <div class="user-card">
      <div class="info-item"><span>用户名：</span> {{ userStore.userInfo.username }}</div>
      <div class="info-item"><span>手机号：</span> {{ userStore.userInfo.phone || '暂无' }}</div>
      <div class="info-item"><span>性别：</span> {{ userStore.userInfo.gender || '暂无' }}</div>
      <div class="info-item"><span>角色：</span> {{ userStore.userInfo.role === 'user' ? '普通用户' : '管理员' }}</div>
    </div>

    <h3>预约记录</h3>
    <el-table :data="reserveData" style="width: 100%; margin-bottom: 24px;" v-loading="reserveLoading">
      <el-table-column prop="title" label="书名" min-width="100" />
      <el-table-column prop="author" label="作者" min-width="100" align="center" />
      <el-table-column prop="publisher" label="出版社" min-width="150" align="center" />
      <el-table-column prop="reserveDate" label="预约日期" width="180" align="center" :formatter="formatDate" />
    </el-table>

    <h3>历史借阅记录</h3>
    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="title" label="书名" min-width="100" />
      <el-table-column prop="author" label="作者" min-width="100" align="center" />
      <el-table-column prop="publisher" label="出版社" min-width="150" align="center" />
      <el-table-column prop="borrowDate" label="借阅日期" width="180" align="center" :formatter="formatDate" />
      <el-table-column prop="returnDate" label="归还日期" width="180" align="center" :formatter="formatDate" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const reserveLoading = ref(false)
const reserveData = ref([])

/**
 * 格式化日期
 * @param {Object} row 
 * @param {Object} column 
 * @param {String} cellValue 
 * @returns {String}
 * @date 2026-03-29
 */
const formatDate = (row, column, cellValue) => {
  if (!cellValue) return '-'
  return dayjs(cellValue).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 获取借阅记录
 * @date 2026-03-28
 */
const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/action/borrow/list')
    tableData.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

/**
 * 获取预约记录
 * @date 2026-04-15
 */
const fetchReserves = async () => {
  reserveLoading.value = true
  try {
    const res = await request.get('/action/reserve/list')
    reserveData.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    reserveLoading.value = false
  }
}

onMounted(() => {
  fetchRecords()
  fetchReserves()
})
</script>

<style scoped>
.profile h2, .profile h3 {
  margin-top: 0;
  margin-bottom: 24px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.5px;
}

.profile h3 {
  margin-top: 32px;
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile h3::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 18px;
  background: var(--primary);
  border-radius: 2px;
}

.user-card {
  border: 1px solid var(--line-solid);
  border-radius: var(--radius-xl);
  padding: 24px 32px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.user-card:hover {
  box-shadow: var(--shadow);
  transform: translateY(-2px);
  border-color: #cbd5e1;
}

.user-card::before {
  content: '';
  position: absolute;
  top: -50px;
  right: -50px;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, var(--primary-soft) 0%, transparent 70%);
  opacity: 0.4;
  border-radius: 50%;
}

.info-item {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 1;
}

.info-item span {
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
</style>
