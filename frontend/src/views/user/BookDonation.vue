<template>
  <div class="book-donation">
    <div class="header">
      <h2>我的捐赠记录</h2>
      <el-button type="primary" @click="openDonateDialog">
        <el-icon><Plus /></el-icon>捐赠图书
      </el-button>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="isbn" label="ISBN" width="140" />
      <el-table-column prop="title" label="书名" min-width="150" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="publisher" label="出版社" width="150" />
      <el-table-column prop="publishDate" label="出版日期" width="120" />
      <el-table-column prop="count" label="数量" width="80" />
      <el-table-column prop="createdAt" label="捐赠日期" width="160">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" fixed="right">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ row.status === 'PENDING' ? '待处理' : row.status === 'PROCESSED' ? '已入库' : row.status === 'REJECTED' ? '已拒绝' : row.status }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- 捐赠弹窗 -->
    <el-dialog v-model="dialogVisible" title="捐赠图书" width="500px">
      <el-form :model="donateForm" :rules="rules" ref="donateFormRef" label-width="100px">
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="donateForm.isbn" placeholder="请输入ISBN" />
        </el-form-item>
        <el-form-item label="书名" prop="title">
          <el-input v-model="donateForm.title" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="donateForm.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="出版社" prop="publisher">
          <el-input v-model="donateForm.publisher" placeholder="请输入出版社" />
        </el-form-item>
        <el-form-item label="出版日期" prop="publishDate">
          <el-date-picker
            v-model="donateForm.publishDate"
            type="date"
            placeholder="选择出版日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="数量" prop="count">
          <el-input-number v-model="donateForm.count" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitDonate" :loading="submitLoading">提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])

/**
 * 格式化日期
 * @param {String} dateStr 
 * @returns {String} 格式化后的日期
 * @date 2026-04-16
 */
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { hour12: false })
}

/**
 * 获取状态标签类型
 * @param {String} status 
 * @returns {String} 标签类型
 * @date 2026-04-16
 */
const getStatusType = (status) => {
  if (status === 'PROCESSED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

/**
 * 获取个人捐赠记录
 * @date 2026-04-16
 */
const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/donation/my')
    if (res.code === 200) {
      tableData.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取记录失败')
    }
  } catch (error) {
    console.error('获取捐赠记录出错', error)
    ElMessage.error('获取记录出错')
  } finally {
    loading.value = false
  }
}

// 捐赠表单相关
const dialogVisible = ref(false)
const submitLoading = ref(false)
const donateFormRef = ref(null)

const donateForm = ref({
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  publishDate: '',
  count: 1
})

const rules = {
  isbn: [{ required: true, message: '请输入ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  publisher: [{ required: true, message: '请输入出版社', trigger: 'blur' }],
  publishDate: [{ required: true, message: '请选择出版日期', trigger: 'change' }],
  count: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

/**
 * 打开捐赠弹窗
 * @date 2026-04-16
 */
const openDonateDialog = () => {
  dialogVisible.value = true
  if (donateFormRef.value) {
    donateFormRef.value.resetFields()
  }
  donateForm.value = {
    isbn: '',
    title: '',
    author: '',
    publisher: '',
    publishDate: '',
    count: 1
  }
}

/**
 * 提交捐赠申请
 * @date 2026-04-16
 */
const submitDonate = async () => {
  if (!donateFormRef.value) return
  await donateFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const res = await request.post('/donation/submit', donateForm.value)
        if (res.code === 200) {
          ElMessage.success('捐赠申请提交成功')
          dialogVisible.value = false
          fetchRecords()
        } else {
          ElMessage.error(res.message || '提交失败')
        }
      } catch (error) {
        console.error('提交出错', error)
        ElMessage.error('提交出错')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
.book-donation {
  padding: 10px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text);
}
</style>