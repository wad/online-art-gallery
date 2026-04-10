import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listGalleries, setDefaultGallery, reorderGalleries, deleteGallery } from '../../api/admin'
import type { GalleryResponse } from '../../api/types'

export default function GalleriesPage() {
  const [galleries, setGalleries] = useState<GalleryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [dragging, setDragging] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    try {
      const res = await listGalleries()
      setGalleries(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  async function handleSetDefault(publicId: string) {
    try {
      await setDefaultGallery(publicId)
      setGalleries(prev => prev.map(g => ({ ...g, isDefault: g.publicId === publicId })))
    } catch {
      setError('Failed to set default gallery.')
    }
  }

  async function handleDelete(publicId: string, name: string) {
    if (!confirm(`Delete gallery "${name}"? This cannot be undone.`)) return
    try {
      await deleteGallery(publicId)
      setGalleries(prev => prev.filter(g => g.publicId !== publicId))
    } catch (e: unknown) {
      const r = (e as { response?: { data?: { detail?: string } } }).response
      setError(r?.data?.detail ?? 'Delete failed')
    }
  }

  // Drag-and-drop reorder
  function handleDragStart(publicId: string) { setDragging(publicId) }
  function handleDragOver(e: React.DragEvent, targetId: string) {
    e.preventDefault()
    if (!dragging || dragging === targetId) return
    const from = galleries.findIndex(g => g.publicId === dragging)
    const to = galleries.findIndex(g => g.publicId === targetId)
    if (from === -1 || to === -1) return
    const next = [...galleries]
    next.splice(to, 0, next.splice(from, 1)[0])
    setGalleries(next)
  }
  async function handleDrop() {
    setDragging(null)
    const items = galleries.map((g, i) => ({ publicId: g.publicId, sortOrder: i }))
    try {
      await reorderGalleries(items)
    } catch {
      setError('Failed to save order.')
    }
  }

  return (
    <>
      <div className="admin-topbar">
        <h1>Galleries</h1>
        <Link to="/admin/galleries/new" className="btn btn-primary btn-sm">+ New Gallery</Link>
      </div>
      <div className="admin-content">
        {error && <div className="alert alert-danger">{error}</div>}
        <div className="card" style={{ padding: 0 }}>
          {loading ? (
            <div style={{ textAlign: 'center', padding: 40, color: 'var(--admin-text-muted)' }}>Loading…</div>
          ) : galleries.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 40, color: 'var(--admin-text-muted)' }}>No galleries yet.</div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th style={{ width: 32 }}></th>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Theme</th>
                    <th style={{ width: 80 }}>Visible</th>
                    <th style={{ width: 80 }}>Default</th>
                    <th style={{ width: 80 }}>Ads</th>
                    <th style={{ width: 80 }}></th>
                  </tr>
                </thead>
                <tbody>
                  {galleries.map(g => (
                    <tr
                      key={g.publicId}
                      draggable
                      onDragStart={() => handleDragStart(g.publicId)}
                      onDragOver={e => handleDragOver(e, g.publicId)}
                      onDrop={handleDrop}
                      style={{ opacity: dragging === g.publicId ? 0.5 : 1, cursor: 'grab' }}
                    >
                      <td style={{ color: 'var(--admin-text-muted)', textAlign: 'center', fontSize: '1rem' }}>⠿</td>
                      <td>
                        <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{g.code}</span>
                      </td>
                      <td>
                        <Link to={`/admin/galleries/${g.publicId}`} className="text-link">{g.name}</Link>
                        {g.subtitle && <div style={{ fontSize: '0.75rem', color: 'var(--admin-text-muted)' }}>{g.subtitle}</div>}
                      </td>
                      <td><span className="badge badge-gray">{g.theme}</span></td>
                      <td>{g.visible ? <span className="badge badge-success">Yes</span> : <span className="badge badge-gray">No</span>}</td>
                      <td>
                        {g.isDefault
                          ? <span className="badge badge-info">Default</span>
                          : <button className="btn btn-secondary btn-sm" onClick={() => handleSetDefault(g.publicId)}>Set</button>}
                      </td>
                      <td>{g.adsEnabled ? <span className="badge badge-success">On</span> : <span className="badge badge-gray">Off</span>}</td>
                      <td>
                        <div className="flex gap-2">
                          <Link to={`/admin/galleries/${g.publicId}`} className="btn btn-secondary btn-sm">Edit</Link>
                          <button className="btn btn-danger btn-sm" onClick={() => handleDelete(g.publicId, g.name)}>Del</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        <div className="form-hint" style={{ textAlign: 'center' }}>Drag rows to reorder galleries.</div>
      </div>
    </>
  )
}
