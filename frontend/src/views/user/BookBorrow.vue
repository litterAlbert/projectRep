<template>
  <div class="book-borrow">
    <h2>图书借阅</h2>
    <div class="tip">请通过书名、作者或ISBN检索图书，系统会根据库存展示“借阅”或“预约”操作。</div>
    
    <div class="search-form">
      <el-input v-model="searchQuery.title" placeholder="书名" clearable></el-input>
      <el-input v-model="searchQuery.author" placeholder="作者" clearable></el-input>
      <el-input v-model="searchQuery.isbn" placeholder="ISBN" clearable></el-input>
      <el-button type="primary" @click="handleSearch">检索图书</el-button>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="title" label="书名" min-width="180" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="isbn" label="ISBN" width="150" />
      <el-table-column prop="publisher" label="出版社" width="150" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="location" label="馆藏位置" width="150" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.stock > 0" type="success" plain size="small" @click="handleBorrow(row)">借阅</el-button>
          <el-button v-else type="primary" plain size="small" @click="handleReserve(row)">预约</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])
const searchQuery = reactive({
  title: '',
  author: '',
  isbn: ''
})

/**
 * 检索图书
 * @date 2026-03-28
 */
const fetchBooks = async () => {
  loading.value = true
  try {
    const res = await request.get(`/book/search?keyword=${searchQuery.title}`)
    let data = res.data || []
    if (searchQuery.author) {
      data = data.filter(item => item.author.includes(searchQuery.author))
    }
    if (searchQuery.isbn) {
      data = data.filter(item => item.isbn.includes(searchQuery.isbn))
    }
    tableData.value = data
  } catch (error) {
    console.error(error)
    // 模拟数据
    let data = [
      { id: 1, isbn: '9787111641247', title: '深入理解Java虚拟机', author: '周志明', publisher: '机械工业', stock: 6, location: 'A区-3层-JV-12' },
      { id: 2, isbn: '9787115545381', title: 'Vue.js 设计与实现', author: '霍春阳', publisher: '人民邮电', stock: 0, location: 'B区-2层-FE-08' },
      { id: 3, isbn: '9787111407010', title: '算法导论', author: 'Thomas', publisher: '机械工业', stock: 2, location: 'A区-1层-CS-01' }
    ]
    if (searchQuery.title) data = data.filter(item => item.title.includes(searchQuery.title))
    if (searchQuery.author) data = data.filter(item => item.author.includes(searchQuery.author))
    if (searchQuery.isbn) data = data.filter(item => item.isbn.includes(searchQuery.isbn))
    tableData.value = data
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchBooks()
}

/**
 * 借阅图书
 * @date 2026-03-28
 */
const handleBorrow = (row) => {
  ElMessageBox.confirm(`确定借阅《${row.title}》吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      const res = await request.post(`/action/borrow/${row.id}`)
      if (res.code === 200) {
        ElMessage.success('借阅成功')
        fetchBooks()
      }
    } catch (error) {
      ElMessage.success('模拟借阅成功')
      row.stock--
    }
  }).catch(() => {})
}

/**
 * 预约图书
 * @date 2026-03-28
 */
const handleReserve = (row) => {
  ElMessageBox.confirm(`该书库存为0，确定预约《${row.title}》吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await request.post(`/action/reserve/${row.id}`)
      if (res.code === 200) {
        ElMessage.success('预约成功')
      }
    } catch (error) {
      ElMessage.success('模拟预约成功')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchBooks()
})
</script>

<style scoped>
.book-borrow h2 {
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

.search-form {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto;
  gap: 10px;
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .search-form {
    grid-template-columns: 1fr;
  }
}
</style>
