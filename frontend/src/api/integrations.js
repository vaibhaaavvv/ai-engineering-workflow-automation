import axios from 'axios'

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080' })
const authHeader = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` })

export const getIntegrationStatus = () =>
  api.get('/integrations/status', { headers: authHeader() })

export const getSlackConnectUrl = () =>
  api.get('/integrations/slack/connect', { headers: authHeader() })
