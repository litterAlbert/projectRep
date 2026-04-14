<template>
  <div class="book-return">
    <h2>归还图书</h2>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="title" label="书名" min-width="150" />
      <el-table-column prop="author" label="作者" min-width="120" align="center" />
      <el-table-column prop="publisher" label="出版社" min-width="150" align="center" />
      <el-table-column prop="borrowDate" label="借阅日期" width="180" align="center" :formatter="formatDate" />
      <el-table-column prop="status" label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ formatStatus(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right" align="center">
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
import dayjs from 'dayjs'

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
 * 获取状态标签类型
 * @param {String} status 
 * @returns {String}
 * @date 2026-03-29
 */
const getStatusType = (status) => {
  if (status === 'BORROWED') return 'primary'
  if (status === 'RETURNED') return 'success'
  return 'info'
}

/**
 * 格式化状态显示
 * @param {String} status 
 * @returns {String}
 * @date 2026-03-29
 */
const formatStatus = (status) => {
  if (status === 'BORROWED') return '借阅中'
  if (status === 'RETURNED') return '已归还'
  return status
}

/**
 * 获取借阅记录
 * @date 2026-03-28
 */
const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/action/borrow/unreturned/list')
    tableData.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

/**
 * 归还图书
 * @date 2026-03-28
 */
const handleReturn = (row) => {
  ElMessageBox.confirm(`确定归还《${row.title}》吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    //try {
      const res = await request.post(`/action/return/${row.id}`)
      if (res.code === 200) {
        ElMessage.success('归还成功')
        fetchRecords()
      }
    //} 
    /*catch (error) {
      ElMessage.success('模拟归还成功')
      tableData.value = tableData.value.filter(item => item.id !== row.id)
    }*/
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


</style>
