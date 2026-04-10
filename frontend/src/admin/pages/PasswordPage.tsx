import { useState } from 'react'
import { changePassword } from '../../api/admin'

export default function PasswordPage() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSuccess(false)
    if (next.length < 8) { setError('New password must be at least 8 characters.'); return }
    if (next !== confirm) { setError('New passwords do not match.'); return }
    setLoading(true)
    try {
      await changePassword(current, next)
      setSuccess(true)
      setCurrent(''); setNext(''); setConfirm('')
    } catch (err: any) {
      setError(err.response?.data?.detail ?? 'Failed to change password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div className="admin-topbar"><h1>Change Password</h1></div>
      <div className="admin-content">
        <div className="card" style={{ maxWidth: 480 }}>
          {error && <div className="alert alert-danger">{error}</div>}
          {success && <div className="alert alert-success">Password changed successfully.</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Current Password</label>
              <input type="password" className="form-control" value={current} onChange={e => setCurrent(e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">New Password</label>
              <input type="password" className="form-control" value={next} onChange={e => setNext(e.target.value)} required minLength={8} />
              <div className="form-hint">Minimum 8 characters.</div>
            </div>
            <div className="form-group">
              <label className="form-label">Confirm New Password</label>
              <input type="password" className="form-control" value={confirm} onChange={e => setConfirm(e.target.value)} required />
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Saving…' : 'Change Password'}
            </button>
          </form>
        </div>
      </div>
    </>
  )
}
