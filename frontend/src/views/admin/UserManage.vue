<template>
  <div class="user-manage">
    <h2>用户管理</h2>
    
    <div class="layout-stack">
      <!-- 统计区域 -->
      <div class="card">
        <h3>用户统计</h3>
        <div class="kpi">
          <div class="kpi-item"><div class="label">用户总数</div><div class="value">{{ totalUsers }}</div></div>
          <div class="kpi-item"><div class="label">本月新增</div><div class="value">{{ newUsersThisMonth }}</div></div>
          <div class="kpi-item"><div class="label">管理员</div><div class="value">{{ adminUsers }}</div></div>
        </div>
      </div>
      
      <!-- 列表区域 -->
      <div class="card">
        <div class="card-header">
          <h3>用户列表</h3>
          <el-button type="primary" @click="handleAddUser">新增用户</el-button>
        </div>
        <el-table :data="tableData" style="width: 100%" height="400" v-loading="loading">
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="gender" label="性别" width="80" />
          <el-table-column prop="phone" label="电话" />
          <el-table-column prop="role" label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">{{ row.role === 'admin' ? '管理员' : '普通用户' }}</el-tag>
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

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog :title="isEdit ? '编辑用户' : '新增用户'" v-model="dialogVisible" width="400px">
      <el-form :model="form" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '不填表示不修改' : '请输入密码'"></el-input>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%;">
            <el-option label="普通用户" value="user"></el-option>
            <el-option label="管理员" value="admin"></el-option>
          </el-select>
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

// 统计数据计算
const totalUsers = computed(() => tableData.value.length)
const adminUsers = computed(() => tableData.value.filter(u => u.role === 'admin').length)
const newUsersThisMonth = computed(() => {
  const now = new Date()
  const currentMonth = now.getMonth()
  const currentYear = now.getFullYear()
  return tableData.value.filter(u => {
    if (!u.createdAt) return false
    const date = new Date(u.createdAt)
    return date.getMonth() === currentMonth && date.getFullYear() === currentYear
  }).length
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: null,
  username: '',
  password: '',
  phone: '',
  gender: '男',
  role: 'user'
})

/**
 * 获取用户列表
 * @date 2026-03-30
 */
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/list')
    if (res.code === 200) {
      tableData.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取列表失败')
      tableData.value = []
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取列表请求失败')
    tableData.value = [] // 移除假数据，使用真实数据
  } finally {
    loading.value = false
  }
}

/**
 * 点击新增用户
 * @date 2026-03-30
 */
const handleAddUser = () => {
  isEdit.value = false
  form.id = null
  form.username = ''
  form.password = ''
  form.phone = ''
  form.gender = '男'
  form.role = 'user'
  dialogVisible.value = true
}

/**
 * 点击编辑用户
 * @date 2026-03-30
 */
const handleEdit = (row) => {
  isEdit.value = true
  form.id = row.id
  form.username = row.username
  form.password = '' // 编辑时不显示原密码
  form.phone = row.phone
  form.gender = row.gender
  form.role = row.role
  dialogVisible.value = true
}

/**
 * 删除用户
 * @date 2026-03-30
 */
const handleDelete = async (row) => {
  try {
    const res = await request.delete(`/user/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchUsers()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除请求失败')
  }
}

/**
 * 提交表单（新增/编辑）
 * @date 2026-03-30
 */
const submitForm = async () => {
  try {
    if (isEdit.value) {
      // 编辑
      const updateData = {
        id: form.id,
        username: form.username,
        phone: form.phone,
        gender: form.gender,
        role: form.role
      }
      if (form.password) {
        updateData.password = form.password
      }
      const res = await request.put('/user/update', updateData)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        fetchUsers()
      } else {
        ElMessage.error(res.message || '更新失败')
      }
    } else {
      // 新增
      if (!form.username || !form.password) {
        ElMessage.warning('请填写用户名和密码')
        return
      }
      const res = await request.post('/user/register', {
        username: form.username,
        password: form.password,
        phone: form.phone,
        gender: form.gender,
        role: form.role
      })
      if (res.code === 200) {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        fetchUsers()
      } else {
        ElMessage.error(res.message || '新增失败')
      }
    }
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新请求失败' : '新增请求失败')
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

.layout-stack {
  display: flex;
  flex-direction: column;
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-header h3 {
  margin: 0;
}

.kpi {
  display: grid;
  grid-template-columns: repeat(3, minmax(100px, 1fr));
  gap: 10px;
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
</style>