import request from './request'

export const knowledgeBaseApi = {
  list: (params) => request.get('/knowledgeBase/list', { params }),
  getById: (id) => request.get(`/knowledgeBase/${id}`),
  save: (data) => request.post('/knowledgeBase', data),
  update: (id, data) => request.put(`/knowledgeBase/${id}`, data),
  delete: (id) => request.delete(`/knowledgeBase/${id}`),
  allEnabled: () => request.get('/knowledgeBase/allEnabled')
}
