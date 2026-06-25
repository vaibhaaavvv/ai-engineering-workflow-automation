import { useEffect, useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { X, GitBranch, Calendar, Clock, Copy, Check } from 'lucide-react'
import AppNavbar from '../components/AppNavbar'
import { LogoMark } from '../components/Logo'
import { getTickets, updateTicketStatus } from '../api/tickets'
import { getIntegrationStatus } from '../api/integrations'

const COLUMNS = [
  { id: 'TO_DO',                label: 'To Do',                color: '#6e6f8c',  accent: '#6e6f8c' },
  { id: 'IN_PROGRESS',          label: 'In Progress',          color: '#818cf8',  accent: '#818cf8' },
  { id: 'PR_RAISED',            label: 'PR Raised',            color: '#f59e0b',  accent: '#f59e0b' },
  { id: 'READY_FOR_DEPLOYMENT', label: 'Ready for Deployment', color: '#10b981',  accent: '#10b981' },
  { id: 'COMPLETED',            label: 'Completed',            color: '#6366f1',  accent: '#6366f1' },
]

const PRIORITY_STYLES = {
  LOW:      'bg-blue-500/10 text-blue-400 border-blue-500/20',
  MEDIUM:   'bg-yellow-500/10 text-yellow-400 border-yellow-500/20',
  HIGH:     'bg-orange-500/10 text-orange-400 border-orange-500/20',
  CRITICAL: 'bg-red-500/10 text-red-400 border-red-500/20',
}

const TYPE_STYLES = {
  BUG:     'bg-red-500/10 text-red-400 border-red-500/20',
  FEATURE: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
  TASK:    'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  CHORE:   'bg-[#252641] text-[#6e6f8c] border-[#3a3b55]',
}

const AVATAR_GRADIENTS = [
  'from-indigo-400 to-violet-500',
  'from-orange-400 to-rose-500',
  'from-emerald-400 to-teal-500',
  'from-blue-400 to-indigo-500',
  'from-amber-400 to-orange-500',
  'from-pink-400 to-rose-500',
]

function getInitials(name) {
  if (!name) return '?'
  return name.trim().split(/\s+/).map(n => n[0]).join('').toUpperCase().slice(0, 2)
}

function getAvatarGradient(name) {
  if (!name) return AVATAR_GRADIENTS[0]
  const hash = name.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0)
  return AVATAR_GRADIENTS[hash % AVATAR_GRADIENTS.length]
}

function AssigneeAvatar({ name, size = 22 }) {
  const gradient = getAvatarGradient(name)
  return (
    <div
      className={`rounded-full bg-gradient-to-br ${gradient} flex items-center justify-center text-white font-bold shrink-0`}
      style={{ width: size, height: size, fontSize: size * 0.38 }}
      title={name}
    >
      {getInitials(name)}
    </div>
  )
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function TicketModal({ ticket, onClose }) {
  const [copied, setCopied] = useState(false)
  const [isClosing, setIsClosing] = useState(false)

  if (!ticket) return null

  const handleClose = () => {
    setIsClosing(true)
    setTimeout(onClose, 220)
  }

  const statusMeta = COLUMNS.find(c => c.id === ticket.status)
  const statusLabel = statusMeta?.label ?? ticket.status
  const statusColor = statusMeta?.color ?? '#6e6f8c'

  const copyBranch = () => {
    if (!ticket.branchName) return
    navigator.clipboard.writeText(ticket.branchName)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div
      className={`fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 ${isClosing ? 'backdrop-exit' : 'backdrop-enter'}`}
      onClick={handleClose}
    >
      <div
        className={`bg-[#181929] border border-[#252641] rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl ${isClosing ? 'modal-exit' : 'modal-enter'}`}
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between px-8 py-6 border-b border-[#252641] gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2.5 mb-3">
              <span className="text-[#6e6f8c] text-sm font-mono font-semibold">{ticket.ticketNumber}</span>
              <span className={`text-xs px-2 py-0.5 rounded-md border font-medium ${TYPE_STYLES[ticket.type] || TYPE_STYLES.CHORE}`}>
                {ticket.type}
              </span>
              <span className={`text-xs px-2 py-0.5 rounded-md border font-medium ${PRIORITY_STYLES[ticket.priority] || PRIORITY_STYLES.MEDIUM}`}>
                {ticket.priority}
              </span>
            </div>
            <h2 className="text-[#e9eaf5] text-2xl font-bold leading-snug">{ticket.title}</h2>
          </div>
          <button
            onClick={handleClose}
            className="text-[#4a4b6a] hover:text-[#e9eaf5] transition-colors p-1.5 rounded-lg hover:bg-[#252641] shrink-0 mt-0.5"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="px-8 py-6 space-y-6 max-h-[70vh] overflow-y-auto">

          {/* Status pill */}
          <div className="flex items-center gap-2">
            <span className="text-[#6e6f8c] text-sm">Status:</span>
            <span
              className="text-sm px-3 py-1 rounded-full font-semibold"
              style={{ backgroundColor: `${statusColor}20`, color: statusColor, border: `1px solid ${statusColor}40` }}
            >
              {statusLabel}
            </span>
          </div>

          {/* Description */}
          {ticket.description ? (
            <div className="bg-[#111224] border border-[#252641] rounded-xl p-4">
              <p className="text-[#6e6f8c] text-sm font-semibold mb-2">Description</p>
              <p className="text-[#b0b2cc] text-base leading-relaxed">{ticket.description}</p>
            </div>
          ) : (
            <div className="bg-[#111224] border border-dashed border-[#252641] rounded-xl p-4">
              <p className="text-[#3a3b55] text-sm">No description added.</p>
            </div>
          )}

          {/* Details grid */}
          <div className="grid grid-cols-2 gap-x-6 gap-y-5">
            <div>
              <p className="text-[#6e6f8c] text-sm font-semibold mb-2">Assignee</p>
              <div className="flex items-center gap-2">
                <AssigneeAvatar name={ticket.assignee} size={26} />
                <span className="text-[#e9eaf5] text-base font-medium">{ticket.assignee}</span>
              </div>
            </div>

            {ticket.branchName && (
              <div>
                <p className="text-[#6e6f8c] text-sm font-semibold mb-2">Branch</p>
                <button
                  onClick={copyBranch}
                  className="flex items-center gap-1.5 group max-w-full"
                  title="Click to copy"
                >
                  <GitBranch size={14} className="text-[#4a4b6a] shrink-0" />
                  <span className="text-[#e9eaf5] text-sm font-mono truncate group-hover:text-indigo-400 transition-colors">
                    {ticket.branchName}
                  </span>
                  {copied
                    ? <Check size={13} className="text-emerald-400 shrink-0" />
                    : <Copy size={13} className="text-[#3a3b55] group-hover:text-indigo-400 shrink-0 transition-colors" />
                  }
                </button>
              </div>
            )}

            <div>
              <p className="text-[#6e6f8c] text-sm font-semibold mb-2">Created</p>
              <div className="flex items-center gap-1.5">
                <Calendar size={14} className="text-[#4a4b6a]" />
                <span className="text-[#e9eaf5] text-base">{formatDate(ticket.createdAt)}</span>
              </div>
            </div>

            <div>
              <p className="text-[#6e6f8c] text-sm font-semibold mb-2">Last updated</p>
              <div className="flex items-center gap-1.5">
                <Clock size={14} className="text-[#4a4b6a]" />
                <span className="text-[#e9eaf5] text-base">{formatDate(ticket.updatedAt)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function TicketCard({ ticket, onDragStart, onClick }) {
  const dragging = useRef(false)
  const preview = ticket.description
    ? ticket.description.length > 70 ? ticket.description.slice(0, 70) + '…' : ticket.description
    : null

  return (
    <div
      draggable
      onDragStart={() => { dragging.current = true; onDragStart(ticket.id) }}
      onDragEnd={() => { setTimeout(() => { dragging.current = false }, 0) }}
      onClick={() => { if (!dragging.current) onClick(ticket) }}
      className="bg-[#111224] border border-[#252641] rounded-xl p-4 cursor-pointer active:cursor-grabbing hover:border-[#3a3b55] hover:bg-[#13142a] transition-all group"
    >
      {/* Top row: ticket number + type */}
      <div className="flex items-center justify-between mb-3">
        <span className="text-[#4a4b6a] text-xs font-mono font-semibold">{ticket.ticketNumber}</span>
        <span className={`text-xs px-2 py-0.5 rounded-md border font-medium ${TYPE_STYLES[ticket.type] || TYPE_STYLES.CHORE}`}>
          {ticket.type}
        </span>
      </div>

      {/* Title */}
      <p className="text-[#e9eaf5] text-base font-semibold leading-snug mb-2">{ticket.title}</p>

      {/* Description preview */}
      {preview && (
        <p className="text-[#6e6f8c] text-sm leading-relaxed mb-3 line-clamp-2">{preview}</p>
      )}

      {/* Bottom row: priority + assignee */}
      <div className="flex items-center justify-between mt-2 pt-2 border-t border-[#1e1f36]">
        <span className={`text-xs px-2 py-0.5 rounded-md border font-medium ${PRIORITY_STYLES[ticket.priority] || PRIORITY_STYLES.MEDIUM}`}>
          {ticket.priority}
        </span>
        <div className="flex items-center gap-1.5">
          <AssigneeAvatar name={ticket.assignee} size={20} />
          <span className="text-[#6e6f8c] text-xs">{ticket.assignee}</span>
        </div>
      </div>
    </div>
  )
}

function KanbanColumn({ column, tickets, onDragStart, onDrop, onCardClick }) {
  const [isOver, setIsOver] = useState(false)

  return (
    <div
      className={`flex-1 min-w-[220px] rounded-xl flex flex-col border transition-all ${
        isOver ? 'border-indigo-500/40 bg-indigo-500/5 shadow-lg shadow-indigo-500/10' : 'border-[#252641] bg-[#181929]'
      }`}
      style={{ borderTop: `2px solid ${column.accent}40` }}
      onDragOver={(e) => { e.preventDefault(); setIsOver(true) }}
      onDragLeave={() => setIsOver(false)}
      onDrop={() => { setIsOver(false); onDrop(column.id) }}
    >
      {/* Header */}
      <div className="px-4 py-3.5 border-b border-[#252641] flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2.5">
          <div className="w-2 h-2 rounded-full" style={{ backgroundColor: column.color }} />
          <span className="text-[#e9eaf5] font-semibold text-sm">{column.label}</span>
        </div>
        <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-[#252641] text-[#6e6f8c]">
          {tickets.length}
        </span>
      </div>

      {/* Cards */}
      <div className="p-3 flex-1 space-y-2.5 min-h-[140px]">
        {tickets.length === 0 ? (
          <div className={`flex items-center justify-center h-20 rounded-lg border border-dashed transition-colors ${
            isOver ? 'border-indigo-500/40 bg-indigo-500/5' : 'border-[#252641]'
          }`}>
            <span className="text-[#2e2f4a] text-xs">{isOver ? 'Drop here' : 'No tickets'}</span>
          </div>
        ) : (
          tickets.map(ticket => (
            <TicketCard
              key={ticket.id}
              ticket={ticket}
              onDragStart={onDragStart}
              onClick={onCardClick}
            />
          ))
        )}
      </div>
    </div>
  )
}

function EmptyBoard() {
  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <LogoMark size={56} className="mb-6 opacity-60" />
      <h2 className="text-[#e9eaf5] text-2xl font-bold mb-3">Your board is empty</h2>
      <p className="text-[#6e6f8c] text-base max-w-md leading-relaxed mb-8">
        Relay is connected and listening. Mention it in any Slack channel to create your first ticket — or run a test to see it in action.
      </p>
      <div className="flex flex-col gap-3 items-center">
        {[
          { label: 'Create a ticket', cmd: '@Relay fix login bug, assign to Vaibhav' },
          { label: 'Run a demo',      cmd: '@Relay test-run' },
        ].map(({ label, cmd }) => (
          <div key={label} className="flex items-center gap-3">
            <span className="text-[#3a3b55] text-sm w-28 text-right shrink-0">{label}</span>
            <code className="text-sm text-[#9293af] bg-[#111224] border border-[#252641] rounded-lg px-3 py-2 font-mono">
              {cmd}
            </code>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)
  const [selectedTicket, setSelectedTicket] = useState(null)
  const dragTicketId = useRef(null)

  useEffect(() => {
    getIntegrationStatus()
      .then(res => {
        if (!res.data.slack) {
          navigate('/connect', { replace: true })
          return Promise.reject('not_connected')
        }
        return getTickets()
      })
      .then(res => setTickets(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [navigate])

  useEffect(() => {
    window.history.pushState(null, '', window.location.href)
    const block = () => window.history.pushState(null, '', window.location.href)
    window.addEventListener('popstate', block)
    return () => window.removeEventListener('popstate', block)
  }, [])

  const handleDragStart = (ticketId) => { dragTicketId.current = ticketId }

  const handleDrop = async (newStatus) => {
    const id = dragTicketId.current
    if (!id) return
    const ticket = tickets.find(t => t.id === id)
    if (!ticket || ticket.status === newStatus) return

    const previousStatus = ticket.status
    setTickets(prev => prev.map(t => t.id === id ? { ...t, status: newStatus } : t))
    try {
      await updateTicketStatus(id, newStatus)
    } catch {
      setTickets(prev => prev.map(t => t.id === id ? { ...t, status: previousStatus } : t))
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0e0f1c] flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  const openCount     = tickets.filter(t => t.status !== 'COMPLETED').length
  const inProgress    = tickets.filter(t => t.status === 'IN_PROGRESS').length
  const criticalCount = tickets.filter(t => t.priority === 'CRITICAL' && t.status !== 'COMPLETED').length
  const doneCount     = tickets.filter(t => t.status === 'COMPLETED').length

  return (
    <div className="min-h-screen bg-[#0e0f1c]">
      <AppNavbar />

      <div className="pt-24 pb-8 px-6">

        {/* Page header */}
        <div className="flex items-start justify-between mb-6 flex-wrap gap-4">
          <div>
            <h1 className="text-3xl font-bold text-[#e9eaf5]">Relay Board</h1>
            <p className="text-[#9293af] text-base mt-1">
              Drag cards between columns to update their status
            </p>
          </div>

          {/* Stats row */}
          {tickets.length > 0 && (
            <div className="flex items-center gap-3 flex-wrap">
              <StatPill label="Open" value={openCount} color="#818cf8" />
              <StatPill label="In Progress" value={inProgress} color="#f59e0b" />
              {criticalCount > 0 && (
                <StatPill label="Critical" value={criticalCount} color="#f87171" urgent />
              )}
              <StatPill label="Done" value={doneCount} color="#10b981" />
            </div>
          )}
        </div>

        {/* Board or empty state */}
        {tickets.length === 0 ? (
          <EmptyBoard />
        ) : (
          <div className="flex gap-4 pb-4 overflow-x-auto">
            {COLUMNS.map(column => (
              <KanbanColumn
                key={column.id}
                column={column}
                tickets={tickets.filter(t => t.status === column.id)}
                onDragStart={handleDragStart}
                onDrop={handleDrop}
                onCardClick={setSelectedTicket}
              />
            ))}
          </div>
        )}
      </div>

      <TicketModal key={selectedTicket?.id} ticket={selectedTicket} onClose={() => setSelectedTicket(null)} />
    </div>
  )
}

function StatPill({ label, value, color, urgent }) {
  return (
    <div
      className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-sm font-medium ${
        urgent ? 'animate-pulse' : ''
      }`}
      style={{
        backgroundColor: `${color}12`,
        borderColor: `${color}30`,
        color,
      }}
    >
      <span className="font-bold text-base">{value}</span>
      <span className="opacity-80">{label}</span>
    </div>
  )
}
