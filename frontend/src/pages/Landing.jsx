import { Link } from 'react-router-dom'
import { ArrowRight, Zap, GitBranch, RefreshCw, MessageSquare, Bot, LayoutGrid } from 'lucide-react'
import { Logo, LogoMark } from '../components/Logo'

function LandingNav() {
  return (
    <nav className="fixed top-0 left-0 right-0 z-50 border-b border-[#252641]/60 bg-[#0e0f1c]/80 backdrop-blur-md px-8 py-4 flex items-center justify-between">
      <Logo size={34} textClass="text-xl" />
      <div className="flex items-center gap-8">
        <a href="#how-it-works" className="text-[#6e6f8c] hover:text-[#e9eaf5] text-base font-medium transition-colors hidden md:block">How it works</a>
        <a href="#features" className="text-[#6e6f8c] hover:text-[#e9eaf5] text-base font-medium transition-colors hidden md:block">Features</a>
        <Link to="/login" className="text-[#6e6f8c] hover:text-[#e9eaf5] text-base font-medium transition-colors">Sign in</Link>
        <Link
          to="/signup"
          className="flex items-center gap-1.5 px-4 py-2 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white text-base font-semibold rounded-lg transition-all"
        >
          Get started <ArrowRight size={15} />
        </Link>
      </div>
    </nav>
  )
}

function SlackMockup() {
  return (
    <div className="flex flex-col items-center gap-4">
      <div className="relative">
        {/* Glow */}
        <div className="absolute -inset-4 bg-gradient-to-br from-indigo-600/20 to-violet-600/20 rounded-3xl blur-2xl" />

        <div className="relative bg-[#181929] border border-[#252641] rounded-2xl p-5 shadow-2xl w-full max-w-md">
          {/* Channel header */}
          <div className="flex items-center gap-2 mb-4 pb-3 border-b border-[#252641]">
            <span className="text-[#4a4b6a] text-sm font-mono">#</span>
            <span className="text-[#9293af] text-sm font-semibold">engineering</span>
          </div>

          {/* Alex's message */}
          <div className="flex items-start gap-3 mb-1">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-orange-400 to-rose-500 flex items-center justify-center text-white text-xs font-bold shrink-0 mt-0.5">A</div>
            <div>
              <div className="flex items-baseline gap-2">
                <span className="text-[#e9eaf5] text-sm font-semibold">Alex</span>
                <span className="text-[#3a3b55] text-xs">11:42 AM</span>
              </div>
              <p className="text-[#9293af] text-sm mt-0.5 leading-relaxed">
                hey — the payment gateway is throwing 500 errors in prod. multiple users can't checkout right now 🚨
              </p>
            </div>
          </div>

          {/* Priya reply */}
          <div className="flex items-start gap-3 mb-4 ml-11">
            <div>
              <div className="flex items-baseline gap-2">
                <span className="text-[#e9eaf5] text-sm font-semibold">Priya</span>
                <span className="text-[#3a3b55] text-xs">11:43 AM</span>
              </div>
              <p className="text-[#9293af] text-sm mt-0.5">
                oh god. Vaibhav can you take a look? you own the payments service
              </p>
            </div>
          </div>

          {/* Thread separator */}
          <div className="flex items-center gap-2 mb-3 ml-11">
            <div className="h-px flex-1 bg-[#252641]" />
            <span className="text-[#3a3b55] text-xs">Relay replied in thread</span>
            <div className="h-px flex-1 bg-[#252641]" />
          </div>

          {/* Bot proposal in thread */}
          <div className="ml-11">
            <div className="flex items-start gap-2.5">
              <LogoMark size={26} className="rounded-lg mt-0.5" />
              <div className="flex-1">
                <div className="flex items-baseline gap-2 mb-2">
                  <span className="text-[#e9eaf5] text-sm font-semibold">Relay</span>
                  <span className="text-[#3a3b55] text-xs">11:43 AM</span>
                </div>
                <div className="bg-[#111224] border border-[#252641] rounded-xl p-3">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-xs px-2 py-0.5 rounded bg-red-500/10 text-red-400 border border-red-500/20 font-medium">BUG</span>
                    <span className="text-xs px-2 py-0.5 rounded bg-red-500/15 text-red-300 border border-red-500/25 font-semibold">CRITICAL</span>
                  </div>
                  <p className="text-[#e9eaf5] text-sm font-semibold mb-1">Fix payment gateway 500 errors</p>
                  <p className="text-[#6e6f8c] text-xs mb-3">
                    Payment gateway throwing 500 errors affecting live checkouts. Multiple users unable to complete purchases.
                  </p>
                  <div className="flex items-center justify-between">
                    <span className="text-[#4a4b6a] text-xs">@Vaibhav</span>
                    <div className="flex gap-2">
                      <button className="px-3 py-1 bg-indigo-600 hover:bg-indigo-500 text-white text-xs rounded-lg font-medium transition-colors">
                        ✓ Create
                      </button>
                      <button className="px-3 py-1 bg-[#1a1b2e] border border-[#3a3b55] text-[#6e6f8c] text-xs rounded-lg transition-colors">
                        ✗ Dismiss
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <p className="text-[#3a3b55] text-sm text-center">
        Relay read the conversation above and opened this ticket proposal — automatically.
      </p>
    </div>
  )
}

function CommandsMockup() {
  const commands = [
    { user: 'you', text: '@Relay create a ticket to fix deployment issue in xyz service, assign to Vaibhav' },
    { user: 'relay', text: '✓ Ticket created: T-042  ·  TASK · HIGH  ·  Assignee: Vaibhav' },
    { user: 'you', text: '@Relay update T-042 move it to in progress' },
    { user: 'relay', text: '✏️ Updated T-042 — STATUS → IN_PROGRESS' },
    { user: 'you', text: '@Relay test-run' },
    { user: 'relay', text: '🧪 Test run started — replaying a 28-message conversation...' },
  ]

  return (
    <div className="relative">
      <div className="absolute -inset-4 bg-gradient-to-br from-violet-600/15 to-indigo-600/15 rounded-3xl blur-2xl" />
      <div className="relative bg-[#0c0d1a] border border-[#252641] rounded-2xl overflow-hidden shadow-2xl">
        {/* Terminal bar */}
        <div className="flex items-center gap-2 px-4 py-3 bg-[#111224] border-b border-[#252641]">
          <div className="w-3 h-3 rounded-full bg-red-500/60" />
          <div className="w-3 h-3 rounded-full bg-yellow-500/60" />
          <div className="w-3 h-3 rounded-full bg-emerald-500/60" />
          <span className="text-[#3a3b55] text-xs ml-2 font-mono">#engineering · Slack</span>
        </div>
        <div className="p-5 space-y-3">
          {commands.map((cmd, i) => (
            <div key={i} className={`flex items-start gap-3 ${cmd.user === 'relay' ? 'opacity-90' : ''}`}>
              {cmd.user === 'you' ? (
                <div className="w-6 h-6 rounded-md bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center text-white text-[10px] font-bold shrink-0 mt-0.5">Y</div>
              ) : (
                <LogoMark size={24} className="rounded-md mt-0.5" />
              )}
              <p className={`text-base font-mono leading-relaxed ${cmd.user === 'relay' ? 'text-emerald-400' : 'text-[#9293af]'}`}>
                {cmd.text}
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

const steps = [
  {
    n: '01',
    icon: MessageSquare,
    title: 'Connect Slack',
    desc: 'Authorize Relay in your workspace with one click. The bot quietly joins your channels — no noise, no configuration required.',
  },
  {
    n: '02',
    icon: Bot,
    title: 'AI reads your conversations',
    desc: 'Relay listens 24/7. When someone mentions a bug, feature request, or task — Relay catches it automatically, even without a @mention.',
  },
  {
    n: '03',
    icon: LayoutGrid,
    title: 'Tickets appear on your board',
    desc: 'Relay replies in-thread with a proposal — title, type, priority, and assignee already filled in. Click ✓ Create and it lands on your Kanban board instantly.',
  },
]

const features = [
  {
    icon: Bot,
    color: 'from-indigo-500/20 to-indigo-600/5',
    border: 'border-indigo-500/20',
    iconColor: 'text-indigo-400',
    title: 'Smart triage',
    desc: 'AI reads between the lines. "The export is broken again" becomes a BUG ticket — no manual typing, no dropped context.',
  },
  {
    icon: Zap,
    color: 'from-violet-500/20 to-violet-600/5',
    border: 'border-violet-500/20',
    iconColor: 'text-violet-400',
    title: '@Relay commands',
    desc: 'Create, update, and delete tickets directly from Slack using plain English. No syntax to memorize, no tool-switching.',
  },
  {
    icon: RefreshCw,
    color: 'from-amber-500/15 to-amber-600/5',
    border: 'border-amber-500/20',
    iconColor: 'text-amber-400',
    title: 'Duplicate detection',
    desc: 'Already have a ticket for that? Relay checks your open board before proposing anything new — no clutter, no repeats.',
  },
  {
    icon: GitBranch,
    color: 'from-emerald-500/15 to-emerald-600/5',
    border: 'border-emerald-500/20',
    iconColor: 'text-emerald-400',
    title: 'Git branch names',
    desc: 'Every ticket comes with a ready-to-use git branch name auto-generated from the title. Go straight from triage to code.',
  },
]

const flowSteps = ['Team chats in Slack', 'Relay detects the issue', 'Ticket created instantly']

export default function Landing() {
  return (
    <div className="min-h-screen bg-[#0e0f1c] text-[#e9eaf5] page-enter">
      <LandingNav />

      {/* Hero */}
      <section className="pt-32 pb-24 px-6 max-w-7xl mx-auto">
        <div className="grid lg:grid-cols-2 gap-16 items-center">
          {/* Left */}
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-indigo-500/10 border border-indigo-500/20 rounded-full text-indigo-400 text-sm font-medium mb-8">
              <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-pulse" />
              Slack conversations → tracked tickets, automatically
            </div>

            <h1 className="text-5xl lg:text-6xl font-bold leading-[1.08] tracking-tight mb-6">
              Never lose a ticket
              <span className="block text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 via-violet-400 to-indigo-300 mt-1">
                in Slack again.
              </span>
            </h1>

            <p className="text-[#6e6f8c] text-xl leading-relaxed mb-8 max-w-lg">
              Relay watches your team's Slack conversations, spots bugs, features, and tasks with AI, and turns them into tracked tickets — without anyone lifting a finger.
            </p>

            {/* Flow pills */}
            <div className="flex items-center gap-2 flex-wrap mb-10">
              {flowSteps.map((step, i) => (
                <div key={step} className="flex items-center gap-2">
                  <span className="px-3 py-1.5 bg-[#181929] border border-[#252641] rounded-lg text-[#9293af] text-sm font-medium whitespace-nowrap">
                    {step}
                  </span>
                  {i < flowSteps.length - 1 && (
                    <ArrowRight size={14} className="text-[#3a3b55] shrink-0" />
                  )}
                </div>
              ))}
            </div>

            <div className="flex items-center gap-4 flex-wrap">
              <Link
                to="/signup"
                className="flex items-center gap-2 px-6 py-3.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-semibold rounded-xl transition-all shadow-lg shadow-indigo-500/25 text-base"
              >
                Get started free <ArrowRight size={18} />
              </Link>
              <Link
                to="/login"
                className="flex items-center gap-2 px-6 py-3.5 border border-[#252641] hover:border-[#3a3b55] text-[#9293af] hover:text-[#e9eaf5] font-semibold rounded-xl transition-all text-base"
              >
                Sign in
              </Link>
            </div>

            <p className="text-[#3a3b55] text-sm mt-6">No credit card required · Works with any Slack workspace</p>
          </div>

          {/* Right — Slack mockup */}
          <div className="flex justify-center lg:justify-end">
            <SlackMockup />
          </div>
        </div>
      </section>

      {/* Divider */}
      <div className="max-w-7xl mx-auto px-6">
        <div className="h-px bg-gradient-to-r from-transparent via-[#252641] to-transparent" />
      </div>

      {/* How it works */}
      <section id="how-it-works" className="py-24 px-6 max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <p className="text-indigo-400 text-sm font-semibold uppercase tracking-widest mb-3">How it works</p>
          <h2 className="text-4xl font-bold">Set up in minutes. Works forever.</h2>
          <p className="text-[#6e6f8c] text-lg mt-4 max-w-lg mx-auto">
            Three steps from Slack authorization to your first automatically-created ticket.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-6 relative">
          {/* Connector line */}
          <div className="absolute top-10 left-1/3 right-1/3 h-px bg-gradient-to-r from-indigo-500/30 to-violet-500/30 hidden md:block" />

          {steps.map(({ n, icon: Icon, title, desc }) => (
            <div key={n} className="relative bg-[#181929] border border-[#252641] rounded-2xl p-7 hover:border-[#3a3b55] transition-colors">
              <div className="flex items-center gap-3 mb-5">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-600/20 to-violet-600/20 border border-indigo-500/20 flex items-center justify-center">
                  <Icon size={18} className="text-indigo-400" />
                </div>
                <span className="text-[#3a3b55] text-2xl font-bold font-mono">{n}</span>
              </div>
              <h3 className="text-[#e9eaf5] font-bold text-xl mb-3">{title}</h3>
              <p className="text-[#9293af] text-base leading-relaxed">{desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Divider */}
      <div className="max-w-7xl mx-auto px-6">
        <div className="h-px bg-gradient-to-r from-transparent via-[#252641] to-transparent" />
      </div>

      {/* Features */}
      <section id="features" className="py-24 px-6 max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <p className="text-indigo-400 text-sm font-semibold uppercase tracking-widest mb-3">Features</p>
          <h2 className="text-4xl font-bold">Everything your team needs.</h2>
          <p className="text-[#6e6f8c] text-lg mt-4 max-w-xl mx-auto">
            Built for engineering teams that move fast and can't afford to lose context.
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-5">
          {features.map(({ icon: Icon, color, border, iconColor, title, desc }) => (
            <div key={title} className={`bg-gradient-to-b ${color} border ${border} rounded-2xl p-6 hover:scale-[1.02] transition-transform`}>
              <div className={`w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center mb-4 ${iconColor}`}>
                <Icon size={20} />
              </div>
              <h3 className="text-[#e9eaf5] font-bold text-lg mb-3">{title}</h3>
              <p className="text-[#9293af] text-base leading-relaxed">{desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Divider */}
      <div className="max-w-7xl mx-auto px-6">
        <div className="h-px bg-gradient-to-r from-transparent via-[#252641] to-transparent" />
      </div>

      {/* Commands section */}
      <section className="py-24 px-6 max-w-7xl mx-auto">
        <div className="grid lg:grid-cols-2 gap-16 items-center">
          <div>
            <p className="text-indigo-400 text-sm font-semibold uppercase tracking-widest mb-3">Command interface</p>
            <h2 className="text-4xl font-bold mb-5">
              Your whole team already knows how to use it.
            </h2>
            <p className="text-[#6e6f8c] text-lg leading-relaxed mb-8">
              Mention <span className="text-indigo-400 font-mono">@Relay</span> in any channel and speak naturally. Create tickets, update statuses, reassign work — all without leaving Slack.
            </p>
            <div className="space-y-3">
              {[
                ['Create', '@Relay create a ticket to fix login bug, assign to Vaibhav'],
                ['Update', '@Relay move T-042 to ready for deployment'],
                ['Delete', '@Relay delete T-007'],
                ['Test', '@Relay test-run'],
              ].map(([label, cmd]) => (
                <div key={label} className="flex items-start gap-3">
                  <span className="text-sm font-semibold text-[#4a4b6a] w-14 mt-2 shrink-0 text-right">{label}</span>
                  <code className="text-base text-[#9293af] bg-[#111224] border border-[#252641] rounded-lg px-3 py-2 font-mono leading-relaxed">{cmd}</code>
                </div>
              ))}
            </div>
          </div>
          <div className="flex justify-center lg:justify-end">
            <CommandsMockup />
          </div>
        </div>
      </section>

      {/* Bottom CTA */}
      <section className="py-24 px-6">
        <div className="max-w-2xl mx-auto text-center">
          <div className="relative">
            <div className="absolute inset-0 bg-gradient-to-r from-indigo-600/20 to-violet-600/20 rounded-3xl blur-3xl" />
            <div className="relative bg-[#181929] border border-[#252641] rounded-3xl px-12 py-16">
              <LogoMark size={52} className="mx-auto mb-6" />
              <h2 className="text-4xl font-bold mb-4">
                Ready to bring order
                <span className="block text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-violet-400">
                  to your Slack?
                </span>
              </h2>
              <p className="text-[#6e6f8c] text-xl mb-8">
                Connect in one click. Your first tickets will appear within minutes.
              </p>
              <Link
                to="/signup"
                className="inline-flex items-center gap-2 px-8 py-4 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-semibold rounded-xl transition-all shadow-xl shadow-indigo-500/25 text-lg"
              >
                Get started free <ArrowRight size={20} />
              </Link>
              <p className="text-[#3a3b55] text-base mt-5">No credit card · No setup fee · Works immediately</p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-[#252641] py-8 px-6">
        <div className="max-w-7xl mx-auto flex items-center justify-between flex-wrap gap-4">
          <Logo size={28} textClass="text-base" />
          <p className="text-[#3a3b55] text-sm">© {new Date().getFullYear()} Relay. All rights reserved.</p>
          <div className="flex items-center gap-6">
            <Link to="/login" className="text-[#4a4b6a] hover:text-[#6e6f8c] text-sm transition-colors">Sign in</Link>
            <Link to="/signup" className="text-[#4a4b6a] hover:text-[#6e6f8c] text-sm transition-colors">Sign up</Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
