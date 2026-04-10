import { useEffect, useState, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listImages, listTags, regenerateAllThumbnails, getJob } from '../../api/admin'
import type { ImageResponse, TagResponse, AsyncJobResponse } from '../../api/types'

export default function ImagesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [images, setImages] = useState<ImageResponse[]>([])
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [tags, setTags] = useState<TagResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [regenJob, setRegenJob] = useState<AsyncJobResponse | null>(null)
  const [regenPollId, setRegenPollId] = useState<number | null>(null)

  const q = searchParams.get('q') ?? ''
  const tag = searchParams.get('tag') ?? ''
  const nsfw = searchParams.get('nsfw')
  const orphan = searchParams.get('orphan') === 'true'
  const sort = searchParams.get('sort') ?? 'uploadedAt'
  const dir = searchParams.get('dir') ?? 'desc'
  const page = Number(searchParams.get('page') ?? '0')
  const size = 20

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await listImages({
        q: q || undefined,
        tag: tag || undefined,
        nsfw: nsfw === 'true' ? true : nsfw === 'false' ? false : undefined,
        orphan: orphan || undefined,
        sort, dir, page, size,
      })
      setImages(res.data.content)
      setTotal(res.data.totalElements)
      setTotalPages(res.data.totalPages)
    } finally {
      setLoading(false)
    }
  }, [q, tag, nsfw, orphan, sort, dir, page, size])

  useEffect(() => { load() }, [load])
  useEffect(() => { listTags().then(r => setTags(r.data)) }, [])

  function setParam(key: string, value: string | null) {
    const p = new URLSearchParams(searchParams)
    if (value === null || value === '') p.delete(key)
    else p.set(key, value)
    p.delete('page')
    setSearchParams(p)
  }

  function toggleSort(field: string) {
    if (sort === field) setParam('dir', dir === 'asc' ? 'desc' : 'asc')
    else { setParam('sort', field); setParam('dir', 'desc') }
  }

  async function handleRegenAll() {
    const res = await regenerateAllThumbnails()
    const jobId = res.data.jobId
    pollJob(jobId)
  }

  function pollJob(jobId: string) {
    const id = setInterval(async () => {
      const res = await getJob(jobId)
      setRegenJob(res.data)
      if (res.data.status !== 'RUNNING') {
        clearInterval(id)
        setRegenPollId(null)
      }
    }, 3000) as unknown as number
    setRegenPollId(id)
    getJob(jobId).then(r => setRegenJob(r.data))
  }

  useEffect(() => () => { if (regenPollId) clearInterval(regenPollId) }, [regenPollId])

  const sortIcon = (field: string) => sort === field ? (dir === 'asc' ? ' ↑' : ' ↓') : ''

  return (
    <>
      <div className="admin-topbar">
        <h1>Images {total > 0 && <span className="text-muted" style={{ fontSize: '0.9rem', fontWeight: 400 }}>({total})</span>}</h1>
        <div className="flex gap-2">
          <Link to="/admin/images/bulk" className="btn btn-secondary btn-sm">⬆ Bulk Upload</Link>
          <button className="btn btn-secondary btn-sm" onClick={handleRegenAll}>🔄 Regen All Thumbnails</button>
          <Link to="/admin/images/new" className="btn btn-primary btn-sm">+ Add Image</Link>
        </div>
      </div>
      <div className="admin-content">
        {regenJob && (
          <div className={`alert ${regenJob.status === 'FAILED' ? 'alert-danger' : regenJob.status === 'COMPLETED' ? 'alert-success' : 'alert-info'}`}>
            {regenJob.status === 'RUNNING'
              ? <>Regenerating thumbnails… {regenJob.processedItems}/{regenJob.totalItems ?? '?'}</>
              : regenJob.status === 'COMPLETED'
                ? <>✓ Done: {regenJob.processedItems - regenJob.failedItems} succeeded, {regenJob.failedItems} failed</>
                : <>Regen failed</>}
            {regenJob.errorMessages?.length > 0 && (
              <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
                {regenJob.errorMessages.slice(0, 5).map((m, i) => <li key={i}>{m}</li>)}
                {regenJob.errorMessages.length > 5 && <li>…and {regenJob.errorMessages.length - 5} more</li>}
              </ul>
            )}
          </div>
        )}

        {/* Filters */}
        <div className="toolbar">
          <input
            className="form-control search-input"
            placeholder="Search title, artist, description…"
            value={q}
            onChange={e => setParam('q', e.target.value)}
          />
          <select className="form-control" style={{ width: 160 }} value={tag} onChange={e => setParam('tag', e.target.value)}>
            <option value="">All tags</option>
            {tags.map(t => <option key={t.publicId} value={t.publicId}>{t.name}</option>)}
          </select>
          <select className="form-control" style={{ width: 120 }} value={nsfw ?? ''} onChange={e => setParam('nsfw', e.target.value)}>
            <option value="">All (NSFW)</option>
            <option value="false">SFW only</option>
            <option value="true">NSFW only</option>
          </select>
          <label className="checkbox-item">
            <input type="checkbox" checked={orphan} onChange={e => setParam('orphan', e.target.checked ? 'true' : null)} />
            Orphans only
          </label>
        </div>

        {orphan && <div className="orphan-warning">⚠ Showing only images not assigned to any gallery.</div>}

        <div className="card" style={{ padding: 0 }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style={{ width: 60 }}>Thumb</th>
                  <th className="th-sortable" onClick={() => toggleSort('title')}>Title{sortIcon('title')}</th>
                  <th className="th-sortable" onClick={() => toggleSort('artistName')}>Artist{sortIcon('artistName')}</th>
                  <th>Tags</th>
                  <th>Galleries</th>
                  <th style={{ width: 60 }}>NSFW</th>
                  <th className="th-sortable" onClick={() => toggleSort('uploadedAt')}>Added{sortIcon('uploadedAt')}</th>
                  <th style={{ width: 60 }}></th>
                </tr>
              </thead>
              <tbody>
                {loading && (
                  <tr><td colSpan={8} style={{ textAlign: 'center', padding: 32, color: 'var(--admin-text-muted)' }}>Loading…</td></tr>
                )}
                {!loading && images.length === 0 && (
                  <tr><td colSpan={8} style={{ textAlign: 'center', padding: 32, color: 'var(--admin-text-muted)' }}>No images found.</td></tr>
                )}
                {images.map(img => (
                  <tr key={img.publicId}>
                    <td>
                      {img.thumbnailUrl
                        ? <img src={img.thumbnailUrl} className="thumb" alt="" />
                        : <div className="thumb-missing">no thumb</div>}
                    </td>
                    <td>
                      <Link to={`/admin/images/${img.publicId}`} className="text-link">
                        {img.title ?? <span className="text-muted">(untitled)</span>}
                      </Link>
                      <div style={{ fontSize: '0.75rem', color: 'var(--admin-text-muted)' }}>{img.shortId}</div>
                    </td>
                    <td>{img.artistName ?? '—'}</td>
                    <td>
                      <div className="flex gap-2" style={{ flexWrap: 'wrap' }}>
                        {img.tagNames.map(t => <span key={t} className="badge badge-gray">{t}</span>)}
                      </div>
                    </td>
                    <td>
                      {img.galleryPublicIds.length === 0
                        ? <span className="badge badge-warning">orphan</span>
                        : <span className="badge badge-info">{img.galleryPublicIds.length}</span>}
                    </td>
                    <td>{img.nsfw && <span className="badge badge-danger">NSFW</span>}</td>
                    <td style={{ fontSize: '0.75rem', color: 'var(--admin-text-muted)' }}>
                      {img.uploadedAt ? new Date(img.uploadedAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <Link to={`/admin/images/${img.publicId}`} className="btn btn-secondary btn-sm">Edit</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="pagination">
            <button className="page-btn" disabled={page === 0} onClick={() => setParam('page', String(page - 1))}>‹ Prev</button>
            {Array.from({ length: Math.min(totalPages, 10) }, (_, i) => {
              const p = totalPages <= 10 ? i : Math.max(0, page - 4) + i
              if (p >= totalPages) return null
              return <button key={p} className={`page-btn ${p === page ? 'active' : ''}`} onClick={() => setParam('page', String(p))}>{p + 1}</button>
            })}
            <button className="page-btn" disabled={page >= totalPages - 1} onClick={() => setParam('page', String(page + 1))}>Next ›</button>
            <span className="text-muted" style={{ fontSize: '0.85rem', marginLeft: 8 }}>
              Page {page + 1} of {totalPages} ({total} total)
            </span>
          </div>
        )}
      </div>
    </>
  )
}
