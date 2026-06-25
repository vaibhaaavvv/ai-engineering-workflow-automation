import axios from 'axios'

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080' })
const authHeader = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` })

export const getTickets = () =>
  api.get('/tickets', { headers: authHeader() })

export const updateTicketStatus = (id, status) =>
  api.patch(`/tickets/${id}/status`, { status }, { headers: authHeader() })
