import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Mail, Lock, ArrowRight, AlertCircle, Check, ChevronLeft } from 'lucide-react'
import { Logo, LogoMark } from '../components/Logo'
import { login } from '../api/auth'

const benefits = [
  'AI reads every message in your Slack channels',
  'Ticket proposals appear in-thread — one click to create',
  'Your Kanban board stays current, automatically',
]

const flowSteps = ['Slack message', 'AI triage', 'Kanban ticket']

export default function Login() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await login(form.email, form.password)
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userEmail', res.data.email)
      navigate('/connect', { replace: true })
    } catch (err) {
      setError(err.response?.data?.error || 'Invalid email or password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex page-enter">

      {/* ── Left panel ── */}
      <div className="hidden lg:flex lg:w-[55%] flex-col justify-between bg-[#0c0d1a] p-14 border-r border-[#252641]">
        <Link to="/"><Logo size={36} textClass="text-2xl" /></Link>

        <div>
          <h2 className="text-5xl font-bold text-[#e9eaf5] leading-tight">
            Stop losing bugs and tasks
            <span className="block mt-2 text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-violet-400">
              buried in Slack threads.
            </span>
          </h2>
          <p className="mt-6 text-[#6e6f8c] text-xl leading-relaxed max-w-md">
            Relay watches your team's conversations and automatically turns engineering chatter into tracked tickets — without anyone lifting a finger.
          </p>

          <div className="mt-10 space-y-4">
            {benefits.map((item) => (
              <div key={item} className="flex items-start gap-4">
                <div className="w-5 h-5 rounded-full bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center shrink-0 mt-0.5">
                  <Check size={11} className="text-indigo-400" strokeWidth={3} />
                </div>
                <span className="text-[#9293af] text-lg leading-snug">{item}</span>
              </div>
            ))}
          </div>

          {/* Mini flow */}
          <div className="mt-10 flex items-center gap-2 flex-wrap">
            {flowSteps.map((step, i) => (
              <div key={step} className="flex items-center gap-2">
                <span className="px-3 py-1.5 bg-[#181929] border border-[#252641] rounded-lg text-[#6e6f8c] text-sm font-medium">
                  {step}
                </span>
                {i < flowSteps.length - 1 && (
                  <ArrowRight size={13} className="text-[#3a3b55]" />
                )}
              </div>
            ))}
          </div>
        </div>

        <p className="text-[#3c3d55] text-base">© {new Date().getFullYear()} Relay · Built for engineering teams</p>
      </div>

      {/* ── Right panel — form ── */}
      <div className="flex-1 flex items-center justify-center bg-[#0e0f1c] px-8 py-12 relative">
        <div className="w-full max-w-md">

          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-[#6e6f8c] hover:text-[#e9eaf5] text-sm font-medium transition-colors mb-8"
          >
            <ChevronLeft size={16} />
            Back to home
          </Link>

          {/* Mobile logo */}
          <Link to="/" className="flex items-center gap-3 mb-10 lg:hidden">
            <LogoMark size={36} />
            <span className="text-[#e9eaf5] font-bold text-xl">Relay</span>
          </Link>

          <div className="bg-[#181929] border border-[#252641] rounded-2xl p-9 shadow-2xl">
            <div className="mb-8">
              <h1 className="text-4xl font-bold text-[#e9eaf5] mb-2">Welcome back</h1>
              <p className="text-[#6e6f8c] text-lg">Sign in to your workspace</p>
            </div>

            {error && (
              <div className="mb-6 flex items-center gap-3 bg-red-500/10 border border-red-500/20 text-red-400 text-base px-4 py-3.5 rounded-xl">
                <AlertCircle size={18} className="shrink-0" />
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="block text-base font-semibold text-[#9293af] mb-2">Email address</label>
                <div className="relative">
                  <Mail size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-[#4a4b6a]" />
                  <input
                    type="email" name="email" value={form.email} onChange={handleChange}
                    placeholder="you@company.com" required className="input-field pl-11"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-base font-semibold text-[#9293af]">Password</label>
                </div>
                <div className="relative">
                  <Lock size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-[#4a4b6a]" />
                  <input
                    type="password" name="password" value={form.password} onChange={handleChange}
                    placeholder="••••••••" required className="input-field pl-11"
                  />
                </div>
              </div>

              <div className="pt-1">
                <button type="submit" disabled={loading} className="btn-primary flex items-center justify-center gap-2">
                  {loading
                    ? <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    : <><span>Sign in</span><ArrowRight size={18} /></>}
                </button>
              </div>
            </form>

            <div className="mt-7 pt-7 border-t border-[#252641] text-center">
              <p className="text-lg text-[#6e6f8c]">
                Don't have an account?{' '}
                <Link to="/signup" className="text-indigo-400 hover:text-indigo-300 font-semibold transition-colors">
                  Create one free
                </Link>
              </p>
            </div>
          </div>

          <p className="text-center text-[#3a3b55] text-sm mt-6">
            By signing in you agree to our terms of service.
          </p>
        </div>
      </div>
    </div>
  )
}
