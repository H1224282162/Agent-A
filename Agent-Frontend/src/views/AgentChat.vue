<template>
  <div style="height:calc(100vh - 140px);display:flex;flex-direction:column;">
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px;">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h3>{{ agentName || 'Agent 对话测试' }}</h3>
      <el-tag v-if="agentCode" type="success">{{ agentCode }}</el-tag>
      <el-button size="small" @click="handleReload" :loading="reloading">🔄 热加载</el-button>
      <el-button size="small" type="primary" plain @click="handleNewSession">＋ 新建会话</el-button>
    </div>

    <div style="flex:1;display:flex;gap:12px;min-height:0;">
      <!-- 左侧：历史会话列表 -->
      <div style="width:200px;display:flex;flex-direction:column;gap:8px;min-height:0;">
        <el-card shadow="never" style="flex:1;overflow-y:auto;margin-bottom:0;">
          <template #header>
            <span style="font-size:13px;font-weight:600;">历史会话</span>
          </template>
          <div v-if="sessionList.length === 0" style="text-align:center;color:#909399;font-size:12px;padding-top:20px;">
            暂无历史会话
          </div>
          <div
            v-for="s in sessionList"
            :key="s"
            @click="handleSelectSession(s)"
            :style="{
              padding: '8px 10px',
              borderRadius: '6px',
              marginBottom: '4px',
              cursor: 'pointer',
              fontSize: '12px',
              background: s === sessionId ? '#ecf5ff' : 'transparent',
              color: s === sessionId ? '#409EFF' : '#606266'
            }"
          >
            <div style="word-break:break-all;">{{ formatSessionTime(s) }}</div>
            <div style="color:#909399;font-size:11px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ s }}</div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：对话区 + 输入区 -->
      <div style="flex:1;display:flex;flex-direction:column;gap:12px;min-width:0;">
        <!-- 对话区域 -->
        <el-card shadow="never" style="flex:1;overflow-y:auto;" ref="chatBox">
          <div v-if="messages.length === 0" style="text-align:center;color:#909399;padding-top:80px;">
            <div style="font-size:48px;">💬</div>
            <div style="margin-top:12px;">在下方输入消息，开始与 Agent 对话</div>
            <div style="font-size:12px;margin-top:4px;">同一个 sessionId 下的多轮对话会保持上下文</div>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" style="margin-bottom:20px;">
            <!-- 用户消息 -->
            <div v-if="msg.role === 'user'" style="display:flex;justify-content:flex-end;">
              <div style="background:#409EFF;color:#fff;padding:10px 16px;border-radius:12px;max-width:70%;word-break:break-all;">
                {{ msg.content }}
              </div>
            </div>

            <!-- Agent 回复 -->
            <div v-else style="display:flex;gap:8px;">
              <div style="width:32px;height:32px;background:#67C23A;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0;">🤖</div>
              <div style="background:#f0f2f5;padding:10px 16px;border-radius:12px;max-width:70%;word-break:break-all;white-space:pre-wrap;">
                {{ msg.content }}
              </div>
            </div>
          </div>

          <!-- 正在输入提示 -->
          <div v-if="loading" style="display:flex;gap:8px;">
            <div style="width:32px;height:32px;background:#67C23A;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0;">🤖</div>
            <div style="background:#f0f2f5;padding:10px 16px;border-radius:12px;color:#909399;">正在思考...</div>
          </div>
        </el-card>

        <!-- 输入区域 -->
        <div style="display:flex;gap:8px;align-items:flex-start;">
          <div style="display:flex;flex-direction:column;gap:4px;width:140px;">
            <el-input v-model="sessionId" size="small" placeholder="sessionId" readonly />
            <el-select v-model="modelType" placeholder="请选择具体大模型" size="small" clearable>
              <el-option
                v-for="item in modelTypeList"
                :key = "item.id"
                :label = "item.name"
                :value = "item.id"
              />
            </el-select>
            <span style="font-size:10px;color:#909399;">同 ID = 同会话</span>
          </div>
          <el-input
            v-model="inputMsg"
            placeholder="输入消息，如：我想喝蜜雪冰城"
            :rows="2"
            type="textarea"
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button type="primary" @click="handleSend" :loading="loading" style="height:60px;width:80px;">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { agentDefApi } from '../api/agentDef'
import { agentChatApi } from '../api/agentChat'

const route = useRoute()
const agentId = route.params.id

const agentCode = ref('')
const agentName = ref('')
const sessionId = ref('')
const sessionList = ref([])
const modelType = ref('')
const modelTypeList = ref([])
const inputMsg = ref('')
const messages = ref([])
const loading = ref(false)
const reloading = ref(false)
const chatBox = ref(null)

onMounted(async () => {
  if (agentId) {
    const agent = await agentDefApi.getById(agentId)
    agentCode.value = agent.agentCode
    agentName.value = agent.agentName
  }

  // 加载可选的大模型列表（后端返回 name 数组，映射为 { id, name } 供下拉框使用）
  try {
    const names = await agentChatApi.listModels()
    modelTypeList.value = (names || []).map(name => ({ id: name, name }))
  } catch (e) {
    // 模型列表加载失败不阻塞对话，仍可走 Agent 默认模型
    console.warn('加载模型列表失败', e)
  }

  // 会话 id 从后端生成
  await handleNewSession()
})

/** 新建会话：从后端获取新会话 id，并刷新历史会话列表 */
const handleNewSession = async () => {
  if (!agentCode.value) return
  try {
    sessionId.value = await agentChatApi.newSession(agentCode.value)
    messages.value = []
    await loadSessions()
  } catch (e) {
    ElMessage.error('生成会话失败')
  }
}

/** 加载历史会话 id 列表 */
const loadSessions = async () => {
  if (!agentCode.value) return
  try {
    sessionList.value = await agentChatApi.listSessions(agentCode.value)
  } catch (e) {
    console.warn('加载历史会话失败', e)
  }
}

/** 点击历史会话：切换 sessionId 并加载历史消息 */
const handleSelectSession = async (sid) => {
  if (sid === sessionId.value) return
  sessionId.value = sid
  loading.value = false
  try {
    const list = await agentChatApi.historyMessages(sid)
    messages.value = list || []
  } catch (e) {
    messages.value = []
    ElMessage.error('加载历史消息失败')
  }
  await nextTick()
  scrollToBottom()
}

/** 从会话 id 中解析时间戳并格式化显示（格式：agentCode_时间戳_随机串） */
const formatSessionTime = (sid) => {
  const parts = (sid || '').split('_')
  const ts = Number(parts[parts.length - 2])
  if (!ts) return sid
  const d = new Date(ts)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const handleSend = async () => {
  const msg = inputMsg.value.trim()
  if (!msg || !agentCode.value || !sessionId.value) return

  messages.value.push({ role: 'user', content: msg })
  inputMsg.value = ''
  loading.value = true

  try {
    // modelType 为空时后端回退到 Agent 默认模型
    const reply = await agentChatApi.chat(agentCode.value, sessionId.value, msg, modelType.value)
    messages.value.push({ role: 'assistant', content: reply })
    // 首次发送后，该会话会进入历史列表，刷新一下
    loadSessions()
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '❌ 请求失败：' + (e.message || e) })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  const el = chatBox.value?.$el
  if (el) el.scrollTop = el.scrollHeight
}

const handleReload = async () => {
  reloading.value = true
  try {
    await agentChatApi.reload(agentCode.value)
    ElMessage.success('热加载成功，最新 Prompt 和工具绑定已生效')
  } catch (e) {
    ElMessage.error('热加载失败')
  } finally {
    reloading.value = false
  }
}
</script>
