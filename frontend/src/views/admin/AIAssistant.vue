<template>
  <div class="ai-assistant">
    <h2>AI助手</h2>
    
    <div class="assistant-layout">
      <aside class="session-pane">
        <h3 class="pane-title">会话管理</h3>
        <el-button type="primary" class="new-session" @click="createNewSession">+ 新建会话</el-button>
        <div class="session-list">
          <div 
            class="session-item" 
            v-for="s in sessions" 
            :key="s.id" 
            :class="{ active: currentSession?.id === s.id }" 
            @click="selectSession(s)"
            @dblclick="startEdit(s)"
          >
            <div v-if="editingSessionId === s.id" class="edit-wrapper">
              <el-input 
                v-model="editTitle" 
                size="small" 
                @blur="finishEdit(s)" 
                @keyup.enter="finishEdit(s)" 
                ref="editInputRef"
              />
            </div>
            <div v-else class="title-wrapper">
              <span class="session-title" :title="s.title">{{ s.title }}</span>
              <el-icon class="delete-icon" @click.stop="deleteSession(s.id)"><Delete /></el-icon>
            </div>
          </div>
        </div>
      </aside>

      <section class="chat-pane">
        <h3 class="pane-title">AI对话区域</h3>
        <div class="chat-stream" ref="chatStreamRef">
          <div v-for="msg in messages" :key="msg.id" class="bubble" :class="[msg.role, { 'chart-bubble': msg.type === 'chart' }]">
            <template v-if="msg.type === 'chart' && msg.chartData">
              <div :ref="el => setChartRef(el, msg.id)" class="chart-container"></div>
            </template>
            <template v-else-if="msg.type === 'doc'">
              <div class="doc-content">{{ msg.content }}</div>
              <div v-if="msg.source_nodes && msg.source_nodes.length" class="source-nodes">
                <strong>来源：</strong>
                <ul>
                  <li v-for="(node, index) in msg.source_nodes" :key="index">{{ node }}</li>
                </ul>
              </div>
            </template>
            <template v-else>
              {{ msg.content }}
            </template>
          </div>
          <div v-if="aiLoading" class="bubble ai">思考中...</div>
        </div>
        <div class="chat-input">
          <el-upload
            class="upload-icon"
            action="/api/ai/upload"
            :headers="headers"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            name="file"
          >
            <el-button type="primary" plain circle title="上传RAG知识库文件">
              <el-icon><Upload /></el-icon>
            </el-button>
          </el-upload>
          <el-input v-model="inputText" placeholder="给AI助手发送消息" @keyup.enter="sendMessage"></el-input>
          <el-button type="primary" @click="sendMessage" :loading="aiLoading">发送</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Upload } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const headers = { Authorization: `Bearer ${userStore.token}` }

const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])

const inputText = ref('')
const aiLoading = ref(false)
const chatStreamRef = ref(null)

const chartRefs = ref({})

const editingSessionId = ref(null)
const editTitle = ref('')
const editInputRef = ref(null)

/**
 * 滚动到底部
 * @date 2026-03-29
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (chatStreamRef.value) {
      chatStreamRef.value.scrollTop = chatStreamRef.value.scrollHeight
    }
  })
}

/**
 * 设置图表ref
 * @date 2026-03-29
 */
const setChartRef = (el, id) => {
  if (el) {
    chartRefs.value[id] = el
  }
}

/**
 * 加载会话列表
 * @date 2026-03-29
 */
const loadSessions = async () => {
  try {
    const res = await request.get('/ai/session/list')
    sessions.value = res.data || []
    if (sessions.value.length > 0) {
      await selectSession(sessions.value[0])
    } else {
      await createNewSession()
    }
  } catch (error) {
    console.error('获取会话列表失败', error)
  }
}

/**
 * 新建会话
 * @date 2026-03-29
 */
const createNewSession = async () => {
  const title = `新会话 ${new Date().toLocaleString()}`
  try {
    const res = await request.post('/ai/session', { title })
    const newSession = res.data
    sessions.value.unshift(newSession)
    await selectSession(newSession)
  } catch (error) {
    console.error('创建会话失败', error)
    ElMessage.error('创建会话失败')
  }
}

/**
 * 选中会话并加载历史
 * @date 2026-03-29
 */
const selectSession = async (session) => {
  currentSession.value = session
  messages.value = []
  try {
    const res = await request.get(`/ai/chat/${session.id}/history`)
    const history = res.data || []
    
    messages.value = history.map(msg => {
      let parsedContent = msg.content
      let type = 'text'
      let chartData = null
      let source_nodes = []
      
      if (msg.role === 'ai') {
        try {
          const parsed = JSON.parse(msg.content)
          if (parsed.type === 'chart') {
            type = 'chart'
            chartData = parsed
          } else if (parsed.type === 'doc') {
            type = 'doc'
            parsedContent = parsed.content
            source_nodes = parsed.source_nodes || []
          } else {
            type = 'text'
            parsedContent = parsed.content || parsed.reply || msg.content
          }
        } catch (e) {
          // not JSON, use as is
        }
      }
      
      return {
        id: msg.id,
        role: msg.role,
        content: parsedContent,
        type,
        chartData,
        source_nodes
      }
    })
    
    nextTick(() => {
      messages.value.forEach(msg => {
        if (msg.type === 'chart' && msg.chartData) {
          renderChart(msg.id, msg.chartData)
        }
      })
      scrollToBottom()
    })
  } catch (error) {
    console.error('获取聊天历史失败', error)
  }
}

/**
 * 删除会话
 * @date 2026-03-29
 */
const deleteSession = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该会话吗？', '提示', { type: 'warning' })
    await request.delete(`/ai/session/${id}`)
    ElMessage.success('删除成功')
    
    sessions.value = sessions.value.filter(s => s.id !== id)
    
    if (currentSession.value && currentSession.value.id === id) {
      currentSession.value = null
      messages.value = []
      if (sessions.value.length > 0) {
        await selectSession(sessions.value[0])
      } else {
        await createNewSession()
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除会话失败', error)
      ElMessage.error('删除会话失败')
    }
  }
}

/**
 * 开始编辑标题
 * @date 2026-03-29
 */
const startEdit = (session) => {
  editingSessionId.value = session.id
  editTitle.value = session.title
  nextTick(() => {
    if (editInputRef.value && editInputRef.value[0]) {
      editInputRef.value[0].focus()
    }
  })
}

/**
 * 结束编辑标题
 * @date 2026-03-29
 */
const finishEdit = async (session) => {
  if (!editingSessionId.value) return
  
  const newTitle = editTitle.value.trim()
  editingSessionId.value = null
  
  if (newTitle && newTitle !== session.title) {
    try {
      await request.put(`/ai/session/${session.id}`, { title: newTitle })
      session.title = newTitle
      ElMessage.success('重命名成功')
    } catch (error) {
      console.error('重命名失败', error)
      ElMessage.error('重命名失败')
    }
  }
}

/**
 * 发送消息
 * @date 2026-03-29
 */
const sendMessage = async () => {
  if (!inputText.value.trim() || !currentSession.value) return
  const query = inputText.value
  const userMsg = { id: Date.now(), role: 'user', content: query, type: 'text' }
  messages.value.push(userMsg)
  inputText.value = ''
  scrollToBottom()
  
  aiLoading.value = true
  try {
    const res = await request.post(`/ai/chat/${currentSession.value.id}`, { message: query })
    const resData = res.data || {}
    
    const aiMsg = {
      id: Date.now(),
      role: 'ai',
      content: resData.content,
      type: resData.type || 'text',
      chartData: resData.type === 'chart' ? resData : null,
      source_nodes: resData.source_nodes || []
    }
    
    messages.value.push(aiMsg)
    
    if (aiMsg.type === 'chart' && aiMsg.chartData) {
      renderChart(aiMsg.id, aiMsg.chartData)
    }
  } catch (error) {
    console.error('发送消息失败', error)
  } finally {
    aiLoading.value = false
    scrollToBottom()
  }
}

/**
 * 渲染图表
 * @date 2026-03-29
 */
const renderChart = (msgId, chartData) => {
  nextTick(() => {
    const el = chartRefs.value[msgId]
    if (el) {
      const myChart = echarts.init(el)
      
      let seriesItem = {
        type: chartData.chartType === 'bar' ? 'bar' : (chartData.chartType === 'line' ? 'line' : 'pie'),
        data: chartData.data.map(item => ({
          name: item.name || item.title || item.username,
          value: item.value ?? item.borrow_count ?? item.borrowedCount ?? item.count ?? 0
        }))
      };

      if (chartData.chartType === 'bar') {
        seriesItem.barMaxWidth = 40;
        seriesItem.itemStyle = {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        };
      } else if (chartData.chartType === 'line') {
        seriesItem.smooth = true;
        seriesItem.areaStyle = {
          opacity: 0.2,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#188df0' },
            { offset: 1, color: '#fff' }
          ])
        };
        seriesItem.lineStyle = { width: 3, color: '#188df0' };
        seriesItem.itemStyle = { color: '#188df0' };
      } else if (chartData.chartType === 'pie') {
        seriesItem.radius = ['40%', '70%'];
        seriesItem.avoidLabelOverlap = false;
        seriesItem.itemStyle = {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        };
        seriesItem.label = {
          show: true,
          formatter: '{b}: {c}'
        };
      }

      const option = {
        tooltip: { 
          trigger: chartData.chartType === 'pie' ? 'item' : 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          borderColor: '#e2e8f0',
          textStyle: { color: '#334155' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '8%',
          containLabel: true
        },
        xAxis: chartData.chartType !== 'pie' ? {
          type: 'category',
          data: chartData.data.map(item => item.name || item.title || item.username),
          axisLine: { lineStyle: { color: '#cbd5e1' } },
          axisLabel: { color: '#475569', interval: 0, width: 80, overflow: 'truncate' }
        } : undefined,
        yAxis: chartData.chartType !== 'pie' ? { 
          type: 'value',
          splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
          axisLabel: { color: '#475569' }
        } : undefined,
        series: [seriesItem]
      }
      
      myChart.setOption(option)
      
      window.addEventListener('resize', () => {
        myChart.resize()
      })
    }
  })
}

/**
 * 上传前校验
 * @date 2026-03-29
 */
const beforeUpload = (file) => {
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) {
    ElMessage.error('上传文件大小不能超过 20MB!')
  }
  return isLt20M
}

/**
 * 上传成功
 * @date 2026-03-29
 */
const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    ElMessage.success('文件上传成功')
    
    // 同步保存到知识库列表 (localStorage)
    try {
      const stored = localStorage.getItem('knowledge_docs')
      const docList = stored ? JSON.parse(stored) : []
      const newDoc = {
        ...res.data,
        uploadTime: new Date().toLocaleString()
      }
      docList.unshift(newDoc)
      localStorage.setItem('knowledge_docs', JSON.stringify(docList))
    } catch (e) {
      console.error('保存文档到知识库失败', e)
    }
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

/**
 * 上传失败
 * @date 2026-03-29
 */
const handleUploadError = () => {
  ElMessage.error('上传失败')
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.ai-assistant h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
}

.assistant-layout {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 14px;
  height: calc(100vh - 140px);
  overflow: hidden;
}

.session-pane, .chat-pane {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 16px;
  background: #fcfdff;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.pane-title {
  margin: 0 0 14px;
  font-size: 15px;
}

.new-session {
  width: 100%;
  margin-bottom: 14px;
}

.session-list {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  padding: 10px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  transition: all .2s;
  display: flex;
  align-items: center;
  min-height: 42px;
}

.session-item:hover {
  background: #f8fafc;
}

.session-item.active {
  border-color: #dbeafe;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}

.title-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-icon {
  color: #ef4444;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
  font-size: 14px;
  margin-left: 8px;
}

.session-item:hover .delete-icon {
  opacity: 1;
}

.delete-icon:hover {
  color: #dc2626;
}

.edit-wrapper {
  width: 100%;
}

.chat-stream {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  margin-bottom: 14px;
  height: 0; /* Important for flex child to scroll */
}

.bubble {
  max-width: 85%;
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.chart-bubble {
  max-width: 100% !important;
  width: 600px;
  background: #fff !important;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.chart-container {
  width: 100%;
  height: 350px;
}

.bubble.user {
  align-self: flex-end;
  background: #dbeafe;
  color: #1e3a8a;
  border-bottom-right-radius: 4px;
}

.bubble.ai {
  align-self: flex-start;
  background: #f1f5f9;
  color: #334155;
  border-bottom-left-radius: 4px;
}

.doc-content {
  margin-bottom: 10px;
}

.source-nodes {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e2e8f0;
  font-size: 12px;
  color: #64748b;
}

.source-nodes ul {
  margin: 5px 0 0 0;
  padding-left: 20px;
}

.chat-input {
  display: flex;
  gap: 10px;
  align-items: center;
}

.upload-icon {
  display: flex;
  align-items: center;
}

@media (max-width: 992px) {
  .assistant-layout {
    grid-template-columns: 1fr;
    height: auto;
  }
  .chat-stream {
    height: 500px;
  }
}
</style>
