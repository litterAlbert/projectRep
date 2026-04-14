<template>
  <div class="book-manage">
    <h2>图书管理</h2>
    
    <div class="kpi">
      <div class="kpi-item"><div class="label">馆藏总量</div><div class="value">{{ totalCollection }}</div></div>
      <div class="kpi-item"><div class="label">在馆可借</div><div class="value">{{ availableStock }}</div></div>
      <div class="kpi-item"><div class="label">预约排队</div><div class="value">{{ totalReserved }}</div></div>
    </div>

    <div class="actions">
      <el-button type="primary" @click="handleAdd">新增图书</el-button>
      <div style="flex: 1"></div>
      <el-input v-model="searchKeyword" placeholder="输入书名/作者/ISBN" style="width: 250px" @keyup.enter="handleSearch">
        <template #append>
          <el-button icon="Search" @click="handleSearch" />
        </template>
      </el-input>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="isbn" label="ISBN" width="150" />
      <el-table-column prop="title" label="书名" min-width="180" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="publisher" label="出版社" width="150" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="borrowedCount" label="借出数量" width="90" />
      <el-table-column prop="reservedCount" label="预约数量" width="90" />
      <el-table-column prop="location" label="馆藏位置" width="150" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 表单弹窗 -->
    <el-dialog :title="dialogType === 'add' ? '新增图书' : '编辑图书'" v-model="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" :disabled="dialogType === 'edit'"></el-input>
        </el-form-item>
        <el-form-item label="书名" prop="title">
          <el-input v-model="form.title"></el-input>
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author"></el-input>
        </el-form-item>
        <el-form-item label="出版社" prop="publisher">
          <el-input v-model="form.publisher"></el-input>
        </el-form-item>
        <el-form-item label="出版日期" prop="publishDate">
          <el-date-picker v-model="form.publishDate" type="date" placeholder="选择日期" style="width: 100%"></el-date-picker>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0"></el-input-number>
        </el-form-item>
        <el-form-item label="馆藏位置" prop="location">
          <el-input v-model="form.location"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])
const searchKeyword = ref('')

/**
 * 计算馆藏总量（所有书的库存+借出数量总和）
 * @date 2026-03-30
 */
const totalCollection = computed(() => {
  return tableData.value.reduce((sum, book) => sum + (book.stock || 0) + (book.borrowedCount || 0), 0)
})

/**
 * 计算在馆可借数量（所有书的库存总和）
 * @date 2026-03-30
 */
const availableStock = computed(() => {
  return tableData.value.reduce((sum, book) => sum + (book.stock || 0), 0)
})

/**
 * 计算预约排队数量（所有书的预约数量总和）
 * @date 2026-03-30
 */
const totalReserved = computed(() => {
  return tableData.value.reduce((sum, book) => sum + (book.reservedCount || 0), 0)
})

const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)

const form = reactive({
  id: null,
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  publishDate: '',
  stock: 0,
  location: ''
})

const rules = {
  isbn: [{ required: true, message: '请输入ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

/**
 * 获取图书列表
 * @date 2026-03-28
 */
const fetchBooks = async () => {
  loading.value = true
  try {
    const res = await request.get(`/book/search?keyword=${searchKeyword.value}`)
    tableData.value = res.data || []
  } catch (error) {
    console.error(error)
    // 模拟数据
    tableData.value = [
      { id: 1, isbn: '9787111641247', title: '深入理解Java虚拟机', author: '周志明', publisher: '机械工业', stock: 6, borrowedCount: 37, reservedCount: 2, location: 'A区-3层' },
      { id: 2, isbn: '9787115545381', title: 'Vue.js 设计与实现', author: '霍春阳', publisher: '人民邮电', stock: 0, borrowedCount: 23, reservedCount: 11, location: 'B区-2层' }
    ]
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchBooks()
}

const handleAdd = () => {
  dialogType.value = 'add'
  form.id = null
  form.isbn = ''
  form.title = ''
  form.author = ''
  form.publisher = ''
  form.publishDate = ''
  form.stock = 0
  form.location = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  //try {
    const res = await request.delete(`/book/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchBooks()
    }
  //} 
  /*catch (error) {
    ElMessage.success('')
  }*/
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (dialogType.value === 'add') {
          const res = await request.post('/book/add', form)
          if (res.code === 200) {
            ElMessage.success('添加成功')
            dialogVisible.value = false
            fetchBooks()
          }
        } else {
          const res = await request.put('/book/update', form)
          if (res.code === 200) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            fetchBooks()
          }
        }
      } catch (error) {
        ElMessage.success(`模拟${dialogType.value === 'add' ? '添加' : '更新'}成功`)
        dialogVisible.value = false
      }
    }
  })
}

onMounted(() => {
  fetchBooks()
})
</script>

<style scoped>
.book-manage h2 {
  margin-top: 0;
  margin-bottom: 24px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.5px;
}

.kpi {
  display: grid;
  grid-template-columns: repeat(3, minmax(140px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.kpi-item {
  border: 1px solid var(--line-solid);
  border-radius: var(--radius-lg);
  padding: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.kpi-item::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, var(--primary-soft) 0%, transparent 70%);
  opacity: 0.5;
  border-radius: 50%;
  transform: translate(30%, -30%);
}

.kpi-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  border-color: #cbd5e1;
}

.kpi-item .label {
  font-size: 14px;
  font-weight: 500;
  color: var(--muted);
}

.kpi-item .value {
  margin-top: 12px;
  font-size: 32px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: -1px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
  background: #f8fafc;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line-solid);
}
</style>
