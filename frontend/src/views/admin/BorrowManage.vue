<template>
  <div class="borrow-manage">
    <h2>借阅管理</h2>
    
    <div class="actions">
      <el-button type="primary">新建借阅记录</el-button>
      <el-button plain type="primary">批量归还登记</el-button>
      <el-button type="warning">超期提醒</el-button>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="reader" label="读者" width="120" />
      <el-table-column prop="bookTitle" label="书名" min-width="180" />
      <el-table-column prop="borrowDate" label="借阅日期" width="150" />
      <el-table-column prop="dueDate" label="应还日期" width="150" />
      <el-table-column prop="returnDate" label="归还日期" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handler" label="处理人" width="100" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === '借阅中' || row.status === '已逾期'" link type="primary" size="small" @click="handleReturn(row)">归还登记</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])

/**
 * 获取状态标签类型
 * @date 2026-03-28
 */
const getStatusType = (status) => {
  if (status === '已归还') return 'success'
  if (status === '借阅中') return 'primary'
  if (status === '预约中') return 'info'
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
    // 模拟管理员获取所有借阅记录，实际需调对应接口
    const res = await request.get('/action/borrow/list')
    tableData.value = res.data || []
  } catch (error) {
    console.error(error)
    tableData.value = [
      { id: 1, reader: '张明', bookTitle: '深入理解Java虚拟机', borrowDate: '2026-03-01', dueDate: '2026-03-21', returnDate: '2026-03-20', status: '已归还', handler: 'admin' },
      { id: 2, reader: '李华', bookTitle: 'Vue.js 设计与实现', borrowDate: '2026-03-14', dueDate: '2026-04-03', returnDate: '-', status: '借阅中', handler: 'admin' },
      { id: 3, reader: '王雨', bookTitle: '算法导论', borrowDate: '-', dueDate: '-', returnDate: '-', status: '预约中', handler: 'admin' }
    ]
  } finally {
    loading.value = false
  }
}

const handleReturn = async (row) => {
  try {
    const res = await request.post(`/action/return/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('归还成功')
      fetchRecords()
    }
  } catch (error) {
    ElMessage.success('模拟归还成功')
    row.status = '已归还'
    row.returnDate = '2026-03-28'
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
.borrow-manage h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: var(--text);
}

.actions {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
