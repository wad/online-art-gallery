import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listGalleries, bulkLoadImages } from '../../api/admin'
import type { GalleryResponse } from '../../api/types'

export default function BulkLoadPage() {
  const navigate = useNavigate()
  const [galleries, setGalleries] = useState<GalleryResponse[]>([])
  const [galleryPublicId, setGalleryPublicId] = useState('')
  const [urlsText, setUrlsText] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [results, setResults] = useState<Array<{ url: string; success: boolean; publicId?: string; shortId?: string; error?: string }> | null>(null)

  useEffect(() => {
    listGalleries().then(r => {
      setGalleries(r.data)
      if (r.data.length > 0) setGalleryPublicId(r.data[0].publicId)
    })
  }, [])

  async function handleSubmit() {
    const urls = urlsText.split('\n').map(u => u.trim()).filter(Boolean)
    if (urls.length === 0) { setError('Enter at least one URL.'); return }
    if (!galleryPublicId) { setError('Select a gallery.'); return }

    setLoading(true)
    setError(null)
    setResults(null)
    try {
      const res = await bulkLoadImages({ urls, galleryPublicId })
      const data = res.data as { results: Array<{ url: string; success: boolean; publicId?: string; shortId?: string; error?: string }> }
      setResults(data.results)
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'response' in e) {
        const r = (e as { response?: { data?: { detail?: string } } }).response
        setError(r?.data?.detail ?? 'Bulk load failed')
      } else {
        setError('Bulk load failed')
      }
    } finally {
      setLoading(false)
    }
  }

  const successCount = results?.filter(r => r.success).length ?? 0
  const failCount = results?.filter(r => !r.success).length ?? 0

  return (
    <>
      <div className="admin-topbar">
        <div className="flex items-center gap-2">
          <Link to="/admin/images" className="btn btn-secondary btn-sm">← Back</Link>
          <h1 style={{ margin: 0 }}>Bulk Upload Images</h1>
        </div>
      </div>

      <div className="admin-content">
        <div className="card" style={{ maxWidth: 640 }}>
          <div className="card-title">Load Images from URLs</div>

          <div className="form-group">
            <label className="form-label">Gallery</label>
            <select className="form-control" value={galleryPublicId} onChange={e => setGalleryPublicId(e.target.value)}>
              {galleries.length === 0 && <option value="">No galleries yet</option>}
              {galleries.map(g => <option key={g.publicId} value={g.publicId}>{g.name} ({g.code})</option>)}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Image URLs (one per line)</label>
            <textarea
              className="form-control"
              rows={10}
              value={urlsText}
              onChange={e => setUrlsText(e.target.value)}
              placeholder="https://example.com/image1.jpg&#10;https://example.com/image2.jpg&#10;…"
              style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}
            />
            <div className="form-hint">
              Images will be fetched and thumbnails generated. If the first 3 consecutive images fail, the bulk load aborts.
            </div>
          </div>

          {error && <div className="alert alert-danger">{error}</div>}

          <button className="btn btn-primary" onClick={handleSubmit} disabled={loading || !galleryPublicId}>
            {loading ? 'Loading…' : `⬆ Load ${urlsText.split('\n').filter(l => l.trim()).length} Image(s)`}
          </button>
        </div>

        {results && (
          <div className="card" style={{ maxWidth: 640 }}>
            <div className="card-title">
              Results: {successCount} succeeded, {failCount} failed
            </div>
            {results.map((r, i) => (
              <div key={i} style={{
                display: 'flex', alignItems: 'flex-start', gap: 10, padding: '8px 0',
                borderBottom: i < results.length - 1 ? '1px solid var(--admin-border)' : 'none',
                fontSize: '0.8rem',
              }}>
                <span style={{ width: 20, flexShrink: 0 }}>{r.success ? '✓' : '✗'}</span>
                <div style={{ flex: 1, overflow: 'hidden' }}>
                  <div style={{ color: r.success ? 'var(--admin-success)' : 'var(--admin-danger)', wordBreak: 'break-all' }}>
                    {r.url}
                  </div>
                  {r.success && r.publicId && (
                    <div style={{ marginTop: 2 }}>
                      <Link to={`/admin/images/${r.publicId}`} className="text-link">
                        Edit image ({r.shortId})
                      </Link>
                    </div>
                  )}
                  {!r.success && r.error && (
                    <div style={{ color: 'var(--admin-text-muted)', marginTop: 2 }}>{r.error}</div>
                  )}
                </div>
              </div>
            ))}
            {successCount > 0 && (
              <div style={{ marginTop: 16 }}>
                <button className="btn btn-secondary btn-sm" onClick={() => navigate('/admin/images')}>
                  View All Images
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </>
  )
}
