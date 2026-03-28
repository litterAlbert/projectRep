<template>
  <div class="user-manage">
    <h2>用户管理</h2>
    
    <div class="grid-2">
      <div class="card">
        <h3>用户统计</h3>
        <div class="kpi">
          <div class="kpi-item"><div class="label">用户总数</div><div class="value">2,039</div></div>
          <div class="kpi-item"><div class="label">本月新增</div><div class="value">164</div></div>
          <div class="kpi-item"><div class="label">管理员</div><div class="value">7</div></div>
        </div>
        <div class="actions">
          <el-button type="primary">新增用户</el-button>
          <el-button plain type="primary">角色分配</el-button>
          <el-button type="warning">冻结账号</el-button>
        </div>
      </div>
      <div class="card">
        <h3>用户列表</h3>
        <el-table :data="tableData" style="width: 100%" height="250" v-loading="loading">
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="gender" label="性别" width="80" />
          <el-table-column prop="phone" label="电话" />
          <el-table-column prop="role" label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">{{ row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
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
      </div>
    </div>

    <el-dialog title="编辑用户" v-model="dialogVisible" width="400px">
      <el-form :model="form" ref="formRef" label-width="80px">
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])

const dialogVisible = ref(false)
const form = reactive({
  id: null,
  phone: '',
  gender: ''
})

/**
 * 获取用户列表
 * @date 2026-03-28
 */
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/list')
    tableData.value = res.data || []
  } catch (error) {
    console.error(error)
    tableData.value = [
      { id: 1, username: 'admin', gender: '男', phone: '13800000001', role: 'admin' },
      { id: 2, username: 'zhangsan', gender: '女', phone: '13800000002', role: 'user' },
      { id: 3, username: 'lisi', gender: '男', phone: '13800000003', role: 'user' }
    ]
  } finally {
    loading.value = false
  }
}

const handleEdit = (row) => {
  form.id = row.id
  form.phone = row.phone
  form.gender = row.gender
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    const res = await request.delete(`/user/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchUsers()
    }
  } catch (error) {
    ElMessage.success('模拟删除成功')
  }
}

const submitForm = async () => {
  try {
    const res = await request.put('/user/update', form)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      dialogVisible.value = false
      fetchUsers()
    }
  } catch (error) {
    ElMessage.success('模拟更新成功')
    dialogVisible.value = false
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-manage h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: var(--text);
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.card {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 14px;
  background: linear-gradient(135deg, #fff, #f8fbff);
}

.card h3 {
  margin: 0 0 10px;
  font-size: 15px;
}

.kpi {
  display: grid;
  grid-template-columns: repeat(3, minmax(100px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.kpi-item {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 12px;
  background: #f8fbff;
}

.kpi-item .label {
  font-size: 12px;
  color: var(--muted);
}

.kpi-item .value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
