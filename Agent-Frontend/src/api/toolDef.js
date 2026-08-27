import request from './request'

export const toolDefApi = {
  list: (params) => request.get('/toolDef/list', { params }),
  allEnabled: () => request.get('/toolDef/allEnabled'),
  getById: (id) => request.get(`/toolDef/${id}`),
  toggle: (id) => request.put(`/toolDef/${id}/toggle`)
}
