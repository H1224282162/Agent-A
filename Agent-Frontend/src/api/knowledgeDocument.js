import request from './request'

export const knowledgeDocumentApi = {
  list: (params) => request.get('/knowledgeDocument/list', { params }),
  upload: (kbId, file) => {
    const formData = new FormData()
    formData.append('kbId', kbId)
    formData.append('file', file)
    return request.post('/knowledgeDocument/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  delete: (id) => request.delete(`/knowledgeDocument/${id}`),
  reparse: (id) => request.post(`/knowledgeDocument/${id}/reparse`)
}
