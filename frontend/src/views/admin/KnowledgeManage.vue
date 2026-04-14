<template>
  <div class="knowledge-manage">
    <div class="header">
      <h2>知识库管理</h2>
      <el-upload
        class="upload-demo"
        action="/api/ai/upload"
        :headers="headers"
        :show-file-list="false"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        name="file"
      >
        <el-button type="primary" :loading="uploading">
          <el-icon><Upload /></el-icon> 上传新文档
        </el-button>
      </el-upload>
    </div>

    <div class="table-container">
      <el-table :data="docList" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column prop="fileName" label="文档名称" min-width="200" />
        <el-table-column prop="fileUrl" label="文档链接" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <a :href="row.fileUrl" target="_blank" class="link-text">{{ row.fileUrl }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" align="center" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const headers = { Authorization: `Bearer ${userStore.token}` }

const docList = ref([])
const loading = ref(false)
const uploading = ref(false)

/**
 * 加载文档列表（从 localStorage）
 * @date 2026-04-14
 */
const loadDocs = () => {
  loading.value = true
  try {
    const stored = localStorage.getItem('knowledge_docs')
    if (stored) {
      docList.value = JSON.parse(stored)
    }
  } catch (e) {
    console.error('加载文档列表失败', e)
  } finally {
    loading.value = false
  }
}

/**
 * 保存到 localStorage
 * @date 2026-04-14
 */
const saveDocs = () => {
  localStorage.setItem('knowledge_docs', JSON.stringify(docList.value))
}

/**
 * 上传前校验
 * @date 2026-04-14
 */
const beforeUpload = (file) => {
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) {
    ElMessage.error('上传文件大小不能超过 20MB!')
    return false
  }
  uploading.value = true
  return true
}

/**
 * 上传成功处理
 * @date 2026-04-14
 */
const handleUploadSuccess = (res) => {
  uploading.value = false
  if (res.code === 200) {
    ElMessage.success('文件上传成功')
    const newDoc = {
      ...res.data,
      uploadTime: new Date().toLocaleString()
    }
    // 添加到列表并保存
    docList.value.unshift(newDoc)
    saveDocs()
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

/**
 * 上传失败处理
 * @date 2026-04-14
 */
const handleUploadError = () => {
  uploading.value = false
  ElMessage.error('上传失败')
}

/**
 * 删除文档
 * @date 2026-04-14
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除文档 ${row.fileName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 调用删除 API
    await request.delete('/ai/document', {
      params: {
        fileUrl: row.fileUrl,
        fileName: row.fileName
      }
    })
    
    ElMessage.success('删除成功')
    
    // 从列表中移除并保存
    docList.value = docList.value.filter(item => item.fileUrl !== row.fileUrl)
    saveDocs()
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文档失败', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadDocs()
})
</script>

<style scoped>
.knowledge-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
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
  font-weight: 600;
}

.table-container {
  flex: 1;
  background: #fff;
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.link-text {
  color: var(--primary);
  text-decoration: none;
}

.link-text:hover {
  text-decoration: underline;
}
</style>
