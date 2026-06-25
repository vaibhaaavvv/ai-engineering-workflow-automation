import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Mail, Lock, ArrowRight, AlertCircle, CheckCircle2, ChevronLeft } from 'lucide-react'
import { Logo, LogoMark } from '../components/Logo'
import { signup } from '../api/auth'

const setupSteps = [
  { n: '01', title: 'Create your account', desc: 'Takes 30 seconds. Just an email and password.' },
  { n: '02', title: 'Connect your Slack workspace', desc: 'Authorize Relay with one click. The bot joins your channels quietly.' },
  { n: '03', title: 'Watch the board fill up', desc: 'Relay reads conversations and proposes tickets automatically — no setup needed.' },
]

export default function Signup() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', confirm: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    setError('')
  }

  const passwordRules = [
    { label: 'At least 8 characters', met: form.password.length >= 8 },
    { label: 'Passwords match', met: form.password.length > 0 && form.password === form.confirm },
  ]

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.password !== form.confirm) { setError('Passwords do not match'); return }
    setLoading(true)
    setError('')
    try {
      const res = await signup(form.email, form.password)
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userEmail', res.data.email)
      navigate('/connect', { replace: true })
    } catch (err) {
      setError(err.response?.data?.error || 'Something went wrong. Please try again.')
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
            Set up in minutes.
            <span className="block mt-2 text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-violet-400">
              Your first ticket in five.
            </span>
          </h2>
          <p className="mt-6 text-[#6e6f8c] text-xl leading-relaxed max-w-md">
            Connect Slack, and Relay starts reading your team's conversations immediately. No training, no configuration, no onboarding doc to read.
          </p>

          {/* Steps */}
          <div className="mt-10 space-y-7">
            {setupSteps.map(({ n, title, desc }) => (
              <div key={n} className="flex items-start gap-5">
                <span className="text-[#3a3b55] text-lg font-bold font-mono shrink-0 w-8 mt-0.5">{n}</span>
                <div>
                  <p className="text-[#e9eaf5] font-semibold text-lg">{title}</p>
                  <p className="text-[#6e6f8c] text-base mt-1 leading-relaxed">{desc}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Callout */}
          <div className="mt-10 bg-[#181929] border border-[#252641] rounded-xl p-5">
            <p className="text-[#9293af] text-base leading-relaxed">
              <span className="text-indigo-400 font-semibold">No Jira. No Linear. No copy-pasting.</span>
              {' '}Relay works where your team already works — Slack.
            </p>
          </div>
        </div>

        <p className="text-[#3c3d55] text-base">© {new Date().getFullYear()} Relay · Built for engineering teams</p>
      </div>

      {/* ── Right panel — form ── */}
      <div className="flex-1 flex items-center justify-center bg-[#0e0f1c] px-8 py-12">
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
              <h1 className="text-4xl font-bold text-[#e9eaf5] mb-2">Create your account</h1>
              <p className="text-[#6e6f8c] text-lg">Start catching every bug and task from day one</p>
            </div>

            {error && (
              <div className="mb-6 flex items-center gap-3 bg-red-500/10 border border-red-500/20 text-red-400 text-base px-4 py-3.5 rounded-xl">
                <AlertCircle size={18} className="shrink-0" />{error}
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
                <label className="block text-base font-semibold text-[#9293af] mb-2">Password</label>
                <div className="relative">
                  <Lock size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-[#4a4b6a]" />
                  <input
                    type="password" name="password" value={form.password} onChange={handleChange}
                    placeholder="••••••••" required className="input-field pl-11"
                  />
                </div>
              </div>

              <div>
                <label className="block text-base font-semibold text-[#9293af] mb-2">Confirm password</label>
                <div className="relative">
                  <Lock size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-[#4a4b6a]" />
                  <input
                    type="password" name="confirm" value={form.confirm} onChange={handleChange}
                    placeholder="••••••••" required className="input-field pl-11"
                  />
                </div>
              </div>

              {(form.password || form.confirm) && (
                <div className="space-y-2 pt-1">
                  {passwordRules.map(rule => (
                    <div key={rule.label} className="flex items-center gap-2.5">
                      <CheckCircle2 size={16} className={rule.met ? 'text-emerald-400' : 'text-[#3c3d55]'} />
                      <span className={`text-base ${rule.met ? 'text-emerald-400' : 'text-[#4a4b6a]'}`}>{rule.label}</span>
                    </div>
                  ))}
                </div>
              )}

              <div className="pt-1">
                <button type="submit" disabled={loading} className="btn-primary flex items-center justify-center gap-2">
                  {loading
                    ? <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    : <><span>Create account</span><ArrowRight size={18} /></>}
                </button>
              </div>
            </form>

            <div className="mt-7 pt-7 border-t border-[#252641] text-center">
              <p className="text-lg text-[#6e6f8c]">
                Already have an account?{' '}
                <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-semibold transition-colors">
                  Sign in
                </Link>
              </p>
            </div>
          </div>

          <p className="text-center text-[#3a3b55] text-sm mt-6">
            By creating an account you agree to our terms of service.
          </p>
        </div>
      </div>
    </div>
  )
}
