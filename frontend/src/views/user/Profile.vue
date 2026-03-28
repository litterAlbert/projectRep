<template>
  <div class="profile">
    <h2>个人信息</h2>
    <div class="tip">以下为当前登录用户的个人借阅记录。</div>

    <div class="user-card">
      <div class="info-item"><span>用户名：</span> {{ userStore.userInfo.username }}</div>
      <div class="info-item"><span>手机号：</span> {{ userStore.userInfo.phone || '暂无' }}</div>
      <div class="info-item"><span>性别：</span> {{ userStore.userInfo.gender || '暂无' }}</div>
      <div class="info-item"><span>角色：</span> {{ userStore.userInfo.role === 'user' ? '普通用户' : '管理员' }}</div>
    </div>

    <h3>历史借阅记录</h3>
    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="bookTitle" label="书名" min-width="180" />
      <el-table-column prop="borrowDate" label="借阅日期" width="150" />
      <el-table-column prop="dueDate" label="应还日期" width="150" />
      <el-table-column prop="returnDate" label="归还日期" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])

/**
 * 获取状态标签类型
 * @date 2026-03-28
 */
const getStatusType = (status) => {
  if (status === '已归还') return 'success'
  if (status === '借阅中') return 'primary'
  if (status === '已逾期') return 'danger'
  return ''
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
    tableData.value = [
      { id: 1, bookTitle: 'Vue.js 设计与实现', borrowDate: '2026-03-14', dueDate: '2026-04-03', returnDate: '-', status: '借阅中' },
      { id: 2, bookTitle: '深入理解Java虚拟机', borrowDate: '2026-03-01', dueDate: '2026-03-21', returnDate: '2026-03-20', status: '已归还' },
      { id: 3, bookTitle: '计算机网络（第8版）', borrowDate: '2026-02-21', dueDate: '2026-03-13', returnDate: '-', status: '已逾期' }
    ]
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

.tip {
  margin-bottom: 14px;
  border-radius: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  border: 1px dashed #cbd5e1;
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
