import request from './request'

export const agentKnowledgeApi = {
  list: (agentId) => request.get('/agentKnowledge/list', { params: { agentId } }),
  bind: (data) => request.post('/agentKnowledge/bind', data),
  unbind: (agentId, kbId) => request.delete('/agentKnowledge/unbind', { params: { agentId, kbId } }),
  toggle: (agentId, kbId) => request.put('/agentKnowledge/toggle', null, { params: { agentId, kbId } })
}
