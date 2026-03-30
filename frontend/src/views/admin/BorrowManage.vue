<template>
  <div class="borrow-manage">
    <h2>借阅管理</h2>
    
    <div class="actions" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
      <el-button type="primary" @click="openCreateDialog">新建借阅记录</el-button>
      <el-input v-model="searchKeyword" placeholder="搜索用户/书名" style="width: 250px" clearable>
        <template #append>
          <el-button>搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="filteredTableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="title" label="书名" min-width="150" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="publisher" label="出版社" width="150" />
      <el-table-column prop="borrowDate" label="借阅日期" width="160">
        <template #default="{ row }">
          {{ formatDate(row.borrowDate) }}
        </template>
      </el-table-column>
      <el-table-column prop="returnDate" label="归还日期" width="160">
        <template #default="{ row }">
          {{ formatDate(row.returnDate) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status === 'BORROWED' ? '借阅中' : row.status === 'RETURNED' ? '已归还' : row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'BORROWED'" link type="primary" size="small" @click="handleReturn(row)">归还</el-button>
          <span v-else style="color: #999; font-size: 13px;">无</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建借阅记录弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建借阅记录" width="800px">
      <div class="create-header" style="display: flex; align-items: center; gap: 15px; margin-bottom: 20px;">
        <el-input v-model="newUsername" placeholder="填写用户名" style="width: 200px" />
        <el-input v-model="bookSearchKeyword" placeholder="搜索图书名/作者/ISBN" style="width: 250px" clearable @keyup.enter="fetchBooks" @clear="fetchBooks">
          <template #append>
            <el-button @click="fetchBooks">搜索</el-button>
          </template>
        </el-input>
        <div style="flex-grow: 1;"></div>
        <el-button :type="isAllReserve ? 'warning' : 'primary'" :loading="submitLoading" @click="submitCreate">{{ submitButtonText }}</el-button>
      </div>
      <el-table :data="booksData" v-loading="booksLoading" style="width: 100%" max-height="400" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="title" label="书名" min-width="150" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="publisher" label="出版社" width="150" />
        <el-table-column prop="stock" label="库存" width="80" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

/**
 * 格式化日期
 * @param {String} dateStr 
 * @returns {String} 格式化后的日期
 * @date 2026-03-30
 */
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const loading = ref(false)
const tableData = ref([])
const searchKeyword = ref('')

/**
 * 过滤后的借阅记录
 * @date 2026-03-30
 */
const filteredTableData = computed(() => {
  if (!searchKeyword.value) return tableData.value
  const keyword = searchKeyword.value.toLowerCase()
  return tableData.value.filter(item => 
    (item.username && item.username.toLowerCase().includes(keyword)) || 
    (item.title && item.title.toLowerCase().includes(keyword))
  )
})

/**
 * 获取状态标签类型
 * @param {String} status 
 * @returns {String} 标签类型
 * @date 2026-03-30
 */
const getStatusType = (status) => {
  if (status === 'RETURNED') return 'success'
  if (status === 'BORROWED') return 'primary'
  if (status === 'RESERVED') return 'info'
  return ''
}

/**
 * 获取所有借阅记录
 * @date 2026-03-30
 */
const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/action/borrow/all')
    if (res.code === 200) {
      tableData.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取借阅记录失败')
    }
  } catch (error) {
    console.error('获取借阅记录出错', error)
    ElMessage.error('获取借阅记录出错')
  } finally {
    loading.value = false
  }
}

/**
 * 归还登记
 * @param {Object} row 
 * @date 2026-03-30
 */
const handleReturn = async (row) => {
  try {
    const res = await request.post(`/action/return/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('归还成功')
      fetchRecords()
    } else {
      ElMessage.error(res.message || '归还失败')
    }
  } catch (error) {
    console.error('归还出错', error)
    ElMessage.error('归还出错')
  }
}

// ================= 新建借阅记录 =================
const createDialogVisible = ref(false)
const newUsername = ref('')
const bookSearchKeyword = ref('')
const booksData = ref([])
const booksLoading = ref(false)
const selectedBooks = ref([])
const submitLoading = ref(false)

/**
 * 是否全部为预约
 * @date 2026-03-30
 */
const isAllReserve = computed(() => {
  return selectedBooks.value.length > 0 && selectedBooks.value.every(book => book.stock === 0)
})

/**
 * 包含预约
 * @date 2026-03-30
 */
const hasReserve = computed(() => {
  return selectedBooks.value.some(book => book.stock === 0)
})

/**
 * 提交按钮文本
 * @date 2026-03-30
 */
const submitButtonText = computed(() => {
  if (selectedBooks.value.length === 0) return '新建'
  if (isAllReserve.value) return '预约'
  if (hasReserve.value) return '新建/预约'
  return '新建'
})

/**
 * 打开新建弹窗
 * @date 2026-03-30
 */
const openCreateDialog = () => {
  createDialogVisible.value = true
  newUsername.value = ''
  bookSearchKeyword.value = ''
  selectedBooks.value = []
  fetchBooks()
}

/**
 * 获取所有图书列表供选择
 * @date 2026-03-30
 */
const fetchBooks = async () => {
  booksLoading.value = true
  try {
    const keyword = encodeURIComponent(bookSearchKeyword.value.trim())
    const res = await request.get(`/book/search?keyword=${keyword}`)
    if (res.code === 200) {
      booksData.value = res.data || []
    }
  } catch (error) {
    console.error('获取图书失败', error)
  } finally {
    booksLoading.value = false
  }
}

/**
 * 图书选择变化
 * @param {Array} selection 
 * @date 2026-03-30
 */
const handleSelectionChange = (selection) => {
  selectedBooks.value = selection
}

/**
 * 提交新建借阅
 * @date 2026-03-30
 */
const submitCreate = async () => {
  if (!newUsername.value.trim()) {
    return ElMessage.warning('请填写用户名')
  }
  if (selectedBooks.value.length === 0) {
    return ElMessage.warning('请至少选择一本图书')
  }

  submitLoading.value = true
  try {
    let successCount = 0
    let failCount = 0
    
    // 遍历选中的书，根据库存逐一发起借阅或预约创建
    for (const book of selectedBooks.value) {
      const isReserve = book.stock === 0
      const payload = {
        username: newUsername.value.trim(),
        title: book.title,
        status: isReserve ? 'RESERVED' : 'BORROWED'
      }
      const url = isReserve ? '/action/reserve/create' : '/action/borrow/create'
      try {
        const res = await request.post(url, payload)
        if (res.code === 200) {
          successCount++
        } else {
          failCount++
        }
      } catch (err) {
        failCount++
        console.error(`处理 ${book.title} 失败:`, err)
      }
    }
    
    if (failCount === 0) {
      ElMessage.success(`成功操作 ${successCount} 条记录`)
      createDialogVisible.value = false
      fetchRecords()
    } else if (successCount > 0) {
      // 只有在部分成功时才提示总结信息，全部失败时依靠全局拦截器的报错提示即可
      ElMessage.warning(`成功 ${successCount} 条，失败 ${failCount} 条`)
      fetchRecords()
    }
  } catch (error) {
    console.error('新建借阅记录出错', error)
    ElMessage.error('新建借阅记录出错')
  } finally {
    submitLoading.value = false
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
