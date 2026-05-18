import axios from 'axios'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => {
    const newToken = response.headers['x-refresh-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
    }
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      if (router.currentRoute.value.name !== 'Login') {
        router.push({ name: 'Login' })
      }
    }
    return Promise.reject(error)
  }
)

export default api