import api from './api'

export const authApi = {
  login(data) { return api.post('/auth/login', data) },
  register(data) { return api.post('/auth/register', data) }
}

export const projectApi = {
  list(page = 0, size = 20) { return api.get('/projects', { params: { page, size } }) },
  get(id) { return api.get(`/projects/${id}`) },
  create(data) { return api.post('/projects', data) },
  update(id, data) { return api.put(`/projects/${id}`, data) },
  invite(id, data) { return api.post(`/projects/${id}/invitations`, data) },
  members(id) { return api.get(`/projects/${id}/members`) },
  changeRole(projectId, memberId, role) { return api.put(`/projects/${projectId}/members/${memberId}/role`, { role }) },
  removeMember(projectId, memberId) { return api.delete(`/projects/${projectId}/members/${memberId}`) }
}

export const boardApi = {
  columns(projectId) { return api.get(`/boards/${projectId}/columns`) },
  createColumn(projectId, data) { return api.post(`/boards/${projectId}/columns`, data) },
  renameColumn(projectId, columnId, data) { return api.put(`/boards/${projectId}/columns/${columnId}`, data) },
  deleteColumn(projectId, columnId) { return api.delete(`/boards/${projectId}/columns/${columnId}`) },
  reorderColumns(projectId, data) { return api.put(`/boards/${projectId}/columns/reorder`, data) }
}

export const taskApi = {
  list(columnId) { return api.get('/tasks', { params: { columnId } }) },
  create(columnId) { return api.post('/tasks', null, { params: { columnId } }) },
  update(id, data) { return api.put(`/tasks/${id}`, data) },
  delete(id) { return api.delete(`/tasks/${id}`) },
  move(id, data) { return api.put(`/tasks/${id}/move`, data) },
  attachments(id) { return api.get(`/tasks/${id}/attachments`) },
  uploadAttachment(id, file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/tasks/${id}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteAttachment(taskId, attachmentId) {
    return api.delete(`/tasks/${taskId}/attachments/${attachmentId}`)
  }
}

export const notificationApi = {
  list() { return api.get('/notifications') },
  unreadCount() { return api.get('/notifications/unread-count') },
  markRead(id) { return api.put(`/notifications/${id}/read`) }
}

export const invitationApi = {
  accept(token) { return api.get(`/invitations/${token}`) }
}

export const aiApi = {
  suggest(description) { return api.post('/ai/suggest', { description }) },
  executeCommand(command, projectId) { return api.post('/ai/command', { command, projectId }) }
}