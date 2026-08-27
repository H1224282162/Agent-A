import request from './request'

export const callLogApi = {
  list: (params) => request.get('/agentCallLog/list', { params }),
  getById: (id) => request.get(`/agentCallLog/${id}`),
  delete: (id) => request.delete(`/agentCallLog/${id}`)
}
