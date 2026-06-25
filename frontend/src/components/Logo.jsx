export function LogoMark({ size = 36, className = '' }) {
  return (
    <div
      style={{ width: size, height: size }}
      className={`rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg shadow-indigo-500/25 shrink-0 ${className}`}
    >
      <svg width={Math.round(size * 0.54)} height={Math.round(size * 0.54)} viewBox="0 0 22 22" fill="none">
        <path d="M2 5L11 11L2 17" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" strokeOpacity="0.45"/>
        <path d="M11 5L20 11L11 17" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    </div>
  )
}

export function Logo({ size = 36, textClass = 'text-xl', className = '' }) {
  return (
    <div className={`flex items-center gap-3 ${className}`}>
      <LogoMark size={size} />
      <span className={`text-[#e9eaf5] font-bold tracking-tight ${textClass}`}>Relay</span>
    </div>
  )
}
