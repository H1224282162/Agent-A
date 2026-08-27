import request from './request'

export const agentToolApi = {
  list: (agentId) => request.get('/agentTool/list', { params: { agentId } }),
  bind: (data) => request.post('/agentTool', data),
  unbind: (agentId, toolId) => request.delete('/agentTool', { params: { agentId, toolId } }),
  toggle: (agentId, toolId) => request.put('/agentTool/toggle', null, { params: { agentId, toolId } })
}
