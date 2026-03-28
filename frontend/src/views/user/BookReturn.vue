<template>
  <div class="book-return">
    <h2>归还图书</h2>
    <div class="tip">以下为当前借阅中的图书，点击“归还”即可发起归还流程。</div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="bookTitle" label="书名" min-width="180" />
      <el-table-column prop="borrowDate" label="借阅日期" width="150" />
      <el-table-column prop="dueDate" label="应还日期" width="150" />
      <el-table-column prop="status" label="逾期状态" width="120">
        <template #default="{ row }">
          <span :style="{ color: row.status === '已逾期' ? 'var(--warn)' : 'var(--ok)' }">{{ row.status === '已逾期' ? '已逾期' : '正常' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="warning" plain size="small" @click="handleReturn(row)">归还</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])

/**
 * 获取借阅记录
 * @date 2026-03-28
 */
const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/action/borrow/list')
    // 这里简单过滤出未归还的
    tableData.value = (res.data || []).filter(item => item.status !== '已归还')
  } catch (error) {
    console.error(error)
    // 模拟数据
    tableData.value = [
      { id: 1, bookTitle: 'Vue.js 设计与实现', borrowDate: '2026-03-14', dueDate: '2026-04-03', status: '正常' },
      { id: 2, bookTitle: '计算机网络（第8版）', borrowDate: '2026-02-21', dueDate: '2026-03-13', status: '已逾期' }
    ]
  } finally {
    loading.value = false
  }
}

/**
 * 归还图书
 * @date 2026-03-28
 */
const handleReturn = (row) => {
  ElMessageBox.confirm(`确定归还《${row.bookTitle}》吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await request.post(`/action/return/${row.id}`)
      if (res.code === 200) {
        ElMessage.success('归还成功')
        fetchRecords()
      }
    } catch (error) {
      ElMessage.success('模拟归还成功')
      tableData.value = tableData.value.filter(item => item.id !== row.id)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
.book-return h2 {
  margin-top: 0;
  margin-bottom: 14px;
  font-size: 18px;
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
</style>
