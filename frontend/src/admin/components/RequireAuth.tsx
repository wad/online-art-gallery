import { Navigate, Outlet } from 'react-router-dom'
import { getToken } from '../../api/client'

export default function RequireAuth() {
  const token = getToken()
  if (!token) {
    return <Navigate to="/admin/login" replace />
  }
  return <Outlet />
}
