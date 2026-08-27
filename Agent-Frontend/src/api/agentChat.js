import request from './request'

export const agentChatApi = {
  /** 列出所有已加载的 Agent */
  listAgents: () => request.get('/agent/list'),

  /** 热加载指定 Agent */
  reload: (agentCode) => request.get(`/agent/${agentCode}/reload`),

  /** 同步对话（modelType 可选，为空时使用 Agent 默认模型） */
  chat: (agentCode, sessionId, msg, modelType) =>
    request.get(`/agent/${agentCode}/chat`, { params: { sessionId, msg, modelType } }),

  /** 获取已配置的大模型路由 name 列表（deepseek / kimi 等） */
  listModels: () => request.get('/model/list'),

  /** 从后端生成新会话 id */
  newSession: (agentCode) => request.get('/session/new', { params: { agentCode } }),

  /** 查询某 Agent 的历史会话 id 列表 */
  listSessions: (agentCode) => request.get('/session/list', { params: { agentCode } }),

  /** 查询某会话的历史消息 */
  historyMessages: (sessionId) => request.get(`/session/${sessionId}/messages`),

  /** 获取 SSE 流式对话的 URL（直接用于 EventSource） */
  chatStreamUrl: (agentCode, sessionId, msg) =>
    `/api/agent/${agentCode}/chat/stream?sessionId=${encodeURIComponent(sessionId)}&msg=${encodeURIComponent(msg)}`
}
