import request from './request'

export const agentDefApi = {
  list: (params) => request.get('/agentDef/list', { params }),
  getById: (id) => request.get(`/agentDef/${id}`),
  save: (data) => request.post('/agentDef', data),
  update: (data) => request.put('/agentDef', data),
  delete: (id) => request.delete(`/agentDef/${id}`),
  clone: (id) => request.post(`/agentDef/${id}/clone`)
}
