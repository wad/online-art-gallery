import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { clearToken } from '../../api/client'
import '../admin.css'

export default function AdminLayout() {
  const navigate = useNavigate()

  function logout() {
    clearToken()
    navigate('/admin/login')
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <NavLink to="/admin" className="admin-sidebar-logo">
          🖼 Art Gallery Admin
        </NavLink>
        <nav className="admin-nav">
          <div className="admin-nav-section">Content</div>
          <NavLink to="/admin/images" className={({ isActive }) => isActive ? 'active' : ''}>
            📷 Images
          </NavLink>
          <NavLink to="/admin/galleries" className={({ isActive }) => isActive ? 'active' : ''}>
            🏛 Galleries
          </NavLink>
          <NavLink to="/admin/tags" className={({ isActive }) => isActive ? 'active' : ''}>
            🏷 Tags
          </NavLink>
          <div className="admin-nav-section">Reports</div>
          <NavLink to="/admin/stats" className={({ isActive }) => isActive ? 'active' : ''}>
            📊 View Stats
          </NavLink>
          <div className="admin-nav-section">Account</div>
          <NavLink to="/admin/password" className={({ isActive }) => isActive ? 'active' : ''}>
            🔑 Change Password
          </NavLink>
          <a href="#" onClick={e => { e.preventDefault(); logout() }}>
            🚪 Logout
          </a>
        </nav>
      </aside>
      <div className="admin-main">
        <Outlet />
      </div>
    </div>
  )
}
