import request from './request'

export const knowledgeChunkApi = {
  list: (params) => request.get('/knowledgeChunk/list', { params }),
  getById: (id) => request.get(`/knowledgeChunk/${id}`)
}
