import { LogOut } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { Logo } from './Logo'

export default function AppNavbar() {
  const navigate = useNavigate()
  const email = localStorage.getItem('userEmail') || ''

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('userEmail')
    navigate('/login', { replace: true })
  }

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 border-b border-[#252641] bg-[#0e0f1c]/90 backdrop-blur-sm px-8 py-4 flex items-center justify-between">
      <Link to="/dashboard">
        <Logo size={32} textClass="text-xl" />
      </Link>
      <div className="flex items-center gap-5">
        <span className="text-[#6e6f8c] text-sm hidden sm:block">{email}</span>
        <button
          onClick={logout}
          className="flex items-center gap-2 text-[#6e6f8c] hover:text-[#e9eaf5] text-sm transition-colors"
        >
          <LogOut size={16} />
          Logout
        </button>
      </div>
    </nav>
  )
}
