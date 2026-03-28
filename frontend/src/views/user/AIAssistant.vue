<template>
  <div class="ai-assistant">
    <h2>AI助手</h2>
    
    <div class="assistant-layout">
      <aside class="session-pane">
        <h3 class="pane-title">会话管理</h3>
        <el-button type="primary" class="new-session" @click="createNewSession">+ 新建对话</el-button>
        <div class="session-list">
          <div class="session-item" v-for="s in sessions" :key="s.id" :class="{ active: currentSession?.id === s.id }" @click="selectSession(s)">
            {{ s.title }}
          </div>
        </div>
        <el-upload
          class="upload-box"
          drag
          action="/ai/upload"
          :headers="headers"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
        >
          <div class="el-upload__text">上传RAG知识库<br/>(PDF / DOCX，≤20MB)</div>
        </el-upload>
      </aside>

      <section class="chat-pane">
        <h3 class="pane-title">AI对话区域</h3>
        <div class="chat-stream" ref="chatStreamRef">
          <div v-for="msg in messages" :key="msg.id" class="bubble" :class="msg.role">
            {{ msg.content }}
          </div>
          <div v-if="aiLoading" class="bubble ai">思考中...</div>
        </div>
        <div class="chat-input">
          <el-input v-model="inputText" placeholder="请输入问题，支持文档问答与借阅咨询..." @keyup.enter="sendMessage"></el-input>
          <el-button type="primary" @click="sendMessage" :loading="aiLoading">发送</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const headers = { Authorization: `Bearer ${userStore.token}` }

const sessions = ref([
  { id: '1', title: '借阅推荐' },
  { id: '2', title: '文档规则问答' }
])
const currentSession = ref(sessions.value[0])
const messages = ref([
  { id: 1, role: 'user', content: '我上传了《借阅须知.pdf》，我最多可以借几本？' },
  { id: 2, role: 'ai', content: '根据文档检索结果，普通用户同时最多可借 5 本，超出需先归还后再借。' }
])

const inputText = ref('')
const aiLoading = ref(false)
const chatStreamRef = ref(null)

/**
 * 滚动到底部
 * @date 2026-03-28
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (chatStreamRef.value) {
      chatStreamRef.value.scrollTop = chatStreamRef.value.scrollHeight
    }
  })
}

const createNewSession = () => {
  const newSession = { id: Date.now().toString(), title: '新会话' }
  sessions.value.unshift(newSession)
  currentSession.value = newSession
  messages.value = []
}

const selectSession = (session) => {
  currentSession.value = session
  messages.value = []
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    ElMessage.success('知识库上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('上传失败')
}

/**
 * 发送消息
 * @date 2026-03-28
 */
const sendMessage = async () => {
  if (!inputText.value.trim()) return
  const query = inputText.value
  messages.value.push({ id: Date.now(), role: 'user', content: query })
  inputText.value = ''
  scrollToBottom()
  
  aiLoading.value = true
  try {
    const res = await request.post(`/ai/chat/${currentSession.value.id}`, { message: query })
    messages.value.push({ id: Date.now(), role: 'ai', content: res.data || '模拟回复：好的，我了解了。' })
  } catch (error) {
    // 模拟回复
    setTimeout(() => {
      messages.value.push({ id: Date.now(), role: 'ai', content: '这是一个模拟的AI回复。接口请求失败：' + error.message })
      scrollToBottom()
    }, 1000)
  } finally {
    aiLoading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  scrollToBottom()
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
  grid-template-columns: 280px 1fr;
  gap: 14px;
  height: calc(100vh - 120px);
}

.session-pane, .chat-pane {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 16px;
  background: #fcfdff;
  display: flex;
  flex-direction: column;
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

.upload-box {
  margin-top: 14px;
}

.upload-box :deep(.el-upload-dragger) {
  padding: 20px 10px;
  height: auto;
}

.chat-stream {
  flex: 1;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  margin-bottom: 14px;
}

.bubble {
  max-width: 85%;
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
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

.chat-input {
  display: flex;
  gap: 10px;
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
