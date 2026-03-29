<template>
  <div class="profile">
    <h2>个人信息</h2>

    <div class="user-card">
      <div class="info-item"><span>用户名：</span> {{ userStore.userInfo.username }}</div>
      <div class="info-item"><span>手机号：</span> {{ userStore.userInfo.phone || '暂无' }}</div>
      <div class="info-item"><span>性别：</span> {{ userStore.userInfo.gender || '暂无' }}</div>
      <div class="info-item"><span>角色：</span> {{ userStore.userInfo.role === 'user' ? '普通用户' : '管理员' }}</div>
    </div>

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

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
.profile h2, .profile h3 {
  margin-top: 0;
  margin-bottom: 14px;
  font-size: 18px;
}

.profile h3 {
  margin-top: 20px;
  font-size: 16px;
}



.user-card {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 16px;
  background: linear-gradient(135deg, #fff, #f8fbff);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  font-size: 14px;
}

.info-item span {
  color: var(--muted);
}
</style>
