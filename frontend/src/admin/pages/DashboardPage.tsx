import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listImages, listGalleries, listTags } from '../../api/admin'

export default function DashboardPage() {
  const [imageCount, setImageCount] = useState<number | null>(null)
  const [orphanCount, setOrphanCount] = useState<number | null>(null)
  const [galleryCount, setGalleryCount] = useState<number | null>(null)
  const [tagCount, setTagCount] = useState<number | null>(null)

  useEffect(() => {
    listImages({ page: 0, size: 1 }).then(r => setImageCount(r.data.totalElements))
    listImages({ orphan: true, page: 0, size: 1 }).then(r => setOrphanCount(r.data.totalElements))
    listGalleries().then(r => setGalleryCount(r.data.length))
    listTags().then(r => setTagCount(r.data.length))
  }, [])

  return (
    <>
      <div className="admin-topbar"><h1>Dashboard</h1></div>
      <div className="admin-content">
        <div className="grid-3" style={{ marginBottom: 24 }}>
          <div className="card stat-card">
            <div className="stat-value">{imageCount ?? '…'}</div>
            <div className="stat-label">Total Images</div>
            {orphanCount != null && orphanCount > 0 && (
              <div className="mt-2">
                <Link to="/admin/images?orphan=true" className="badge badge-warning">
                  {orphanCount} orphan{orphanCount !== 1 ? 's' : ''}
                </Link>
              </div>
            )}
          </div>
          <div className="card stat-card">
            <div className="stat-value">{galleryCount ?? '…'}</div>
            <div className="stat-label">Galleries</div>
          </div>
          <div className="card stat-card">
            <div className="stat-value">{tagCount ?? '…'}</div>
            <div className="stat-label">Tags</div>
          </div>
        </div>

        <div className="card">
          <div className="card-title">Quick Actions</div>
          <div className="flex gap-2" style={{ flexWrap: 'wrap' }}>
            <Link to="/admin/images" className="btn btn-secondary">📷 Manage Images</Link>
            <Link to="/admin/images/bulk" className="btn btn-secondary">⬆ Bulk Upload</Link>
            <Link to="/admin/galleries" className="btn btn-secondary">🏛 Manage Galleries</Link>
            <Link to="/admin/tags" className="btn btn-secondary">🏷 Manage Tags</Link>
            <Link to="/admin/stats" className="btn btn-secondary">📊 View Statistics</Link>
          </div>
        </div>
      </div>
    </>
  )
}
