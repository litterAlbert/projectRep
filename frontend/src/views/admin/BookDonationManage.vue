<template>
  <div class="donation-manage">
    <h2>旧书入库</h2>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="username" label="捐赠者" width="120" />
      <el-table-column prop="isbn" label="ISBN" width="140" />
      <el-table-column prop="title" label="书名" min-width="150" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="publisher" label="出版社" width="150" />
      <el-table-column prop="count" label="数量" width="80" />
      <el-table-column prop="createdAt" label="捐赠日期" width="160">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ row.status === 'PENDING' ? '待处理' : row.status === 'PROCESSED' ? '已入库' : row.status === 'REJECTED' ? '已拒绝' : row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button link type="primary" size="small" @click="openProcessDialog(row)">入库</el-button>
            <el-button link type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
          </template>
          <span v-else style="color: #999; font-size: 13px;">无</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 入库弹窗 -->
    <el-dialog v-model="processDialogVisible" title="旧书入库" width="400px">
      <el-form :model="processForm" ref="processFormRef" label-width="100px">
        <el-form-item label="馆藏位置" prop="location" :rules="[{ required: true, message: '请输入馆藏位置', trigger: 'blur' }]">
          <el-input v-model="processForm.location" placeholder="例如：A区1架1层" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="processDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitProcess" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

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

const loading = ref(false)
const tableData = ref([])

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
 * 获取所有捐赠记录
 * @date 2026-04-16
 */
const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/donation/list')
    if (res.code === 200) {
      tableData.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取捐赠记录失败')
    }
  } catch (error) {
    console.error('获取捐赠记录出错', error)
    ElMessage.error('获取捐赠记录出错')
  } finally {
    loading.value = false
  }
}

// ================= 处理入库 =================
const processDialogVisible = ref(false)
const submitLoading = ref(false)
const currentRowId = ref(null)
const processFormRef = ref(null)
const processForm = ref({
  location: ''
})

/**
 * 打开入库弹窗
 * @param {Object} row 
 * @date 2026-04-16
 */
const openProcessDialog = (row) => {
  currentRowId.value = row.id
  processForm.value.location = ''
  processDialogVisible.value = true
  if (processFormRef.value) {
    processFormRef.value.clearValidate()
  }
}

/**
 * 提交入库
 * @date 2026-04-16
 */
const submitProcess = async () => {
  if (!processFormRef.value) return
  await processFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const res = await request.post(`/donation/process/${currentRowId.value}?location=${encodeURIComponent(processForm.value.location)}`)
        if (res.code === 200) {
          ElMessage.success('入库成功')
          processDialogVisible.value = false
          fetchRecords()
        } else {
          ElMessage.error(res.message || '入库失败')
        }
      } catch (error) {
        console.error('入库出错', error)
        ElMessage.error('入库出错')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

/**
 * 拒绝捐赠
 * @param {Object} row 
 * @date 2026-04-16
 */
const handleReject = (row) => {
  ElMessageBox.confirm('确定要拒绝该捐赠申请吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await request.post(`/donation/reject/${row.id}`)
      if (res.code === 200) {
        ElMessage.success('已拒绝捐赠')
        fetchRecords()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      console.error('操作出错', error)
      ElMessage.error('操作出错')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
.donation-manage h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: var(--text);
}
</style>
