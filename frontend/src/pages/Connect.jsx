import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle2, ArrowRight, Zap } from 'lucide-react'
import AppNavbar from '../components/AppNavbar'
import { getIntegrationStatus, getSlackConnectUrl } from '../api/integrations'

function SlackIcon({ size = 24, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 127 127" className={className}>
      <path d="M27.2 80c0 7.3-5.9 13.2-13.2 13.2C6.7 93.2.8 87.3.8 80c0-7.3 5.9-13.2 13.2-13.2h13.2V80zm6.6 0c0-7.3 5.9-13.2 13.2-13.2 7.3 0 13.2 5.9 13.2 13.2v33c0 7.3-5.9 13.2-13.2 13.2-7.3 0-13.2-5.9-13.2-13.2V80z" fill="#E01E5A"/>
      <path d="M47 27c-7.3 0-13.2-5.9-13.2-13.2C33.8 6.5 39.7.6 47 .6c7.3 0 13.2 5.9 13.2 13.2V27H47zm0 6.7c7.3 0 13.2 5.9 13.2 13.2 0 7.3-5.9 13.2-13.2 13.2H13.9C6.6 60.1.7 54.2.7 46.9c0-7.3 5.9-13.2 13.2-13.2H47z" fill="#36C5F0"/>
      <path d="M99.9 46.9c0-7.3 5.9-13.2 13.2-13.2 7.3 0 13.2 5.9 13.2 13.2 0 7.3-5.9 13.2-13.2 13.2H99.9V46.9zm-6.6 0c0 7.3-5.9 13.2-13.2 13.2-7.3 0-13.2-5.9-13.2-13.2V13.8C66.9 6.5 72.8.6 80.1.6c7.3 0 13.2 5.9 13.2 13.2v33.1z" fill="#2EB67D"/>
      <path d="M80.1 99.8c7.3 0 13.2 5.9 13.2 13.2 0 7.3-5.9 13.2-13.2 13.2-7.3 0-13.2-5.9-13.2-13.2V99.8h13.2zm0-6.6c-7.3 0-13.2-5.9-13.2-13.2 0-7.3 5.9-13.2 13.2-13.2h33.1c7.3 0 13.2 5.9 13.2 13.2 0 7.3-5.9 13.2-13.2 13.2H80.1z" fill="#ECB22E"/>
    </svg>
  )
}

export default function Connect() {
  const navigate = useNavigate()
  const [slackConnected, setSlackConnected] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getIntegrationStatus()
      .then(res => {
        setSlackConnected(res.data.slack)
        if (res.data.slack) {
          navigate('/dashboard', { replace: true })
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [navigate])

  const handleConnect = async () => {
    const res = await getSlackConnectUrl()
    window.location.href = res.data.url
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0e0f1c] flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[#0e0f1c] page-enter">
      <AppNavbar />

      <div className="pt-28 pb-16 px-6 max-w-xl mx-auto">

        {/* Header */}
        <div className="text-center mb-14">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-600/20 to-violet-600/20 border border-indigo-500/25 flex items-center justify-center mx-auto mb-6">
            <Zap size={26} className="text-indigo-400" />
          </div>
          <h1 className="text-4xl font-bold text-[#e9eaf5] mb-3">Set up your workspace</h1>
          <p className="text-[#6e6f8c] text-lg max-w-md mx-auto">
            Connect Slack so Relay can listen for issues and create tickets autonomously.
          </p>
        </div>

        {/* Slack card */}
        <div className={`rounded-2xl border p-7 transition-all ${slackConnected
          ? 'border-emerald-500/30 bg-emerald-500/5'
          : 'border-[#252641] bg-[#181929]'}`}
        >
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-[#1a0a1f] flex items-center justify-center">
                <SlackIcon size={26} />
              </div>
              <span className="text-[#e9eaf5] font-bold text-xl">Slack</span>
            </div>
            {slackConnected && (
              <span className="flex items-center gap-1.5 text-sm font-medium text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-3 py-1 rounded-full">
                <CheckCircle2 size={13} />
                Connected
              </span>
            )}
          </div>

          <p className="text-[#6e6f8c] text-base leading-relaxed mb-7">
            Relay listens to your channels and automatically detects issues, feature requests, and tasks — then proposes tickets for your approval.
          </p>

          {slackConnected ? (
            <div className="w-full py-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-base font-semibold text-center">
              Connected
            </div>
          ) : (
            <button
              onClick={handleConnect}
              className="flex items-center justify-center gap-2 w-full py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 text-white text-base font-semibold hover:from-indigo-500 hover:to-violet-500 transition-all"
            >
              Connect Slack
              <ArrowRight size={16} />
            </button>
          )}
        </div>

        {/* Bottom CTA */}
        {slackConnected && (
          <div className="mt-10 text-center">
            <button
              onClick={() => navigate('/dashboard', { replace: true })}
              className="inline-flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-indigo-600 to-violet-600 text-white font-semibold rounded-xl text-lg hover:from-indigo-500 hover:to-violet-500 transition-all"
            >
              Go to workspace <ArrowRight size={20} />
            </button>
          </div>
        )}

      </div>
    </div>
  )
}
