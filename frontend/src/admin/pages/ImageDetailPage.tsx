import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import {
  getImage, updateImage, deleteImage, getImageUsage,
  regenerateThumbnail, listTags, createTag, addTagToImage, removeTagFromImage,
} from '../../api/admin'
import type { ImageResponse, TagResponse, ImageUsageResponse } from '../../api/types'

export default function ImageDetailPage() {
  const { publicId } = useParams<{ publicId: string }>()
  const navigate = useNavigate()

  const [image, setImage] = useState<ImageResponse | null>(null)
  const [allTags, setAllTags] = useState<TagResponse[]>([])
  const [usage, setUsage] = useState<ImageUsageResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saveSuccess, setSaveSuccess] = useState(false)

  // Form fields
  const [url, setUrl] = useState('')
  const [title, setTitle] = useState('')
  const [artistName, setArtistName] = useState('')
  const [description, setDescription] = useState('')
  const [artCreationDate, setArtCreationDate] = useState('')
  const [artistComments, setArtistComments] = useState('')
  const [notes, setNotes] = useState('')
  const [adminNotes, setAdminNotes] = useState('')
  const [nsfw, setNsfw] = useState(false)
  const [baseImageShortId, setBaseImageShortId] = useState('')
  const [tagPublicIds, setTagPublicIds] = useState<Set<string>>(new Set())

  // Tag creation
  const [newTagName, setNewTagName] = useState('')
  const [creatingTag, setCreatingTag] = useState(false)

  // Thumbnail regen
  const [regenStatus, setRegenStatus] = useState<'idle' | 'loading' | 'ok' | 'error'>('idle')
  const [regenError, setRegenError] = useState<string | null>(null)

  // URL change tracking
  const [originalUrl, setOriginalUrl] = useState('')
  const [urlChanged, setUrlChanged] = useState(false)

  // Delete dialog
  const [showDelete, setShowDelete] = useState(false)
  const [substituteShortId, setSubstituteShortId] = useState('00000000')
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!publicId) return
    setLoading(true)
    try {
      const [imgRes, tagsRes, usageRes] = await Promise.all([
        getImage(publicId),
        listTags(),
        getImageUsage(publicId),
      ])
      const img = imgRes.data
      setImage(img)
      setUrl(img.url)
      setOriginalUrl(img.url)
      setTitle(img.title ?? '')
      setArtistName(img.artistName ?? '')
      setDescription(img.description ?? '')
      setArtCreationDate(img.artCreationDate ?? '')
      setArtistComments(img.artistComments ?? '')
      setNotes(img.notes ?? '')
      setAdminNotes(img.adminNotes ?? '')
      setNsfw(img.nsfw)
      setBaseImageShortId('')
      setAllTags(tagsRes.data)
      setUsage(usageRes.data)
      // Build tag set by matching tagNames to tag publicIds
      const nameToId = new Map(tagsRes.data.map(t => [t.name, t.publicId]))
      setTagPublicIds(new Set(img.tagNames.map(n => nameToId.get(n)).filter(Boolean) as string[]))
    } finally {
      setLoading(false)
    }
  }, [publicId])

  useEffect(() => { load() }, [load])
  useEffect(() => { setUrlChanged(url !== originalUrl) }, [url, originalUrl])

  async function handleSave() {
    if (!publicId) return
    setSaving(true)
    setError(null)
    setSaveSuccess(false)
    try {
      await updateImage(publicId, {
        url,
        title: title || null,
        artistName: artistName || null,
        description: description || null,
        artCreationDate: artCreationDate || null,
        artistComments: artistComments || null,
        notes: notes || null,
        adminNotes: adminNotes || null,
        nsfw,
      })
      setSaveSuccess(true)
      setOriginalUrl(url)
      setUrlChanged(false)
      // Refresh to pick up new thumbnailUrl if URL changed
      const refreshed = await getImage(publicId)
      setImage(refreshed.data)
    } catch (e: unknown) {
      setError(extractError(e))
    } finally {
      setSaving(false)
    }
  }

  async function handleRegenThumbnail() {
    if (!publicId) return
    setRegenStatus('loading')
    setRegenError(null)
    try {
      await regenerateThumbnail(publicId)
      setRegenStatus('ok')
      const refreshed = await getImage(publicId)
      setImage(refreshed.data)
    } catch (e: unknown) {
      setRegenStatus('error')
      setRegenError(extractError(e))
    }
  }

  async function handleToggleTag(tagPublicId: string, checked: boolean) {
    if (!publicId) return
    const newSet = new Set(tagPublicIds)
    if (checked) {
      await addTagToImage(publicId, tagPublicId)
      newSet.add(tagPublicId)
    } else {
      await removeTagFromImage(publicId, tagPublicId)
      newSet.delete(tagPublicId)
    }
    setTagPublicIds(newSet)
  }

  async function handleCreateTag() {
    const name = newTagName.trim()
    if (!name || !publicId) return
    setCreatingTag(true)
    try {
      const res = await createTag(name)
      const newTag = res.data
      setAllTags(prev => [...prev, newTag])
      await addTagToImage(publicId, newTag.publicId)
      setTagPublicIds(prev => new Set([...prev, newTag.publicId]))
      setNewTagName('')
    } finally {
      setCreatingTag(false)
    }
  }

  async function handleDelete() {
    if (!publicId) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteImage(publicId, substituteShortId || '00000000')
      navigate('/admin/images')
    } catch (e: unknown) {
      setDeleteError(extractError(e))
      setDeleting(false)
    }
  }

  if (loading) return <div className="admin-content" style={{ padding: 40, textAlign: 'center', color: 'var(--admin-text-muted)' }}>Loading…</div>
  if (!image) return <div className="admin-content" style={{ padding: 40 }}>Image not found.</div>

  const isOrphan = image.galleryPublicIds.length === 0

  return (
    <>
      <div className="admin-topbar">
        <div className="flex items-center gap-2">
          <Link to="/admin/images" className="btn btn-secondary btn-sm">← Back</Link>
          <h1 style={{ margin: 0 }}>{image.title ?? <span className="text-muted">(untitled)</span>}</h1>
          {isOrphan && <span className="badge badge-warning">orphan</span>}
          {image.nsfw && <span className="badge badge-danger">NSFW</span>}
        </div>
        <div className="flex gap-2">
          <button className="btn btn-danger btn-sm" onClick={() => setShowDelete(true)}>Delete</button>
          <button className="btn btn-primary btn-sm" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </div>

      <div className="admin-content">
        {isOrphan && (
          <div className="alert alert-warning">
            ⚠ This image is not assigned to any gallery. Add it to a gallery from the gallery management page.
          </div>
        )}
        {error && <div className="alert alert-danger">{error}</div>}
        {saveSuccess && <div className="alert alert-success">✓ Saved successfully.{urlChanged ? '' : ''}</div>}

        <div className="grid-2" style={{ gap: 24, alignItems: 'start' }}>
          {/* Left column */}
          <div>
            {/* Thumbnail & URL */}
            <div className="card">
              <div className="card-title">Image Source</div>
              <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
                <div>
                  {image.thumbnailUrl
                    ? <img src={image.thumbnailUrl} alt="" style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 6, border: '1px solid var(--admin-border)' }} />
                    : <div className="thumb-missing" style={{ width: 100, height: 100 }}>no thumb</div>}
                  <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 4 }}>
                    <button className="btn btn-secondary btn-sm" onClick={handleRegenThumbnail} disabled={regenStatus === 'loading'}>
                      {regenStatus === 'loading' ? 'Regenerating…' : '🔄 Regen Thumb'}
                    </button>
                    {regenStatus === 'ok' && <span className="text-success" style={{ fontSize: '0.75rem' }}>✓ Done</span>}
                    {regenStatus === 'error' && <span className="text-danger" style={{ fontSize: '0.75rem' }}>{regenError}</span>}
                  </div>
                </div>
                <div style={{ flex: 1 }}>
                  <div className="form-group" style={{ marginBottom: 8 }}>
                    <label className="form-label">Short ID</label>
                    <div style={{ fontFamily: 'monospace', fontSize: '1rem', color: 'var(--admin-text-muted)' }}>{image.shortId}</div>
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label">Uploaded</label>
                    <div style={{ fontSize: '0.875rem', color: 'var(--admin-text-muted)' }}>
                      {image.uploadedAt ? new Date(image.uploadedAt).toLocaleString() : '—'}
                    </div>
                  </div>
                </div>
              </div>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Image URL {urlChanged && <span className="badge badge-warning" style={{ marginLeft: 6 }}>changed — will regen thumbnail on save</span>}</label>
                <input
                  className="form-control"
                  value={url}
                  onChange={e => setUrl(e.target.value)}
                  placeholder="https://…"
                />
              </div>
            </div>

            {/* Metadata */}
            <div className="card">
              <div className="card-title">Metadata</div>
              <div className="form-group">
                <label className="form-label">Title</label>
                <input className="form-control" value={title} onChange={e => setTitle(e.target.value)} placeholder="(untitled)" />
              </div>
              <div className="form-group">
                <label className="form-label">Artist Name</label>
                <input className="form-control" value={artistName} onChange={e => setArtistName(e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Art Creation Date</label>
                <input className="form-control" value={artCreationDate} onChange={e => setArtCreationDate(e.target.value)} placeholder="e.g. 2024, circa 1890, Spring 2023" />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea className="form-control" rows={3} value={description} onChange={e => setDescription(e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Artist Comments</label>
                <textarea className="form-control" rows={3} value={artistComments} onChange={e => setArtistComments(e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Notes (public)</label>
                <textarea className="form-control" rows={2} value={notes} onChange={e => setNotes(e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Admin Notes (private)</label>
                <textarea className="form-control" rows={2} value={adminNotes} onChange={e => setAdminNotes(e.target.value)} />
              </div>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="checkbox-item">
                  <input type="checkbox" checked={nsfw} onChange={e => setNsfw(e.target.checked)} />
                  NSFW (age-restricted content)
                </label>
              </div>
            </div>

            {/* Base image */}
            <div className="card">
              <div className="card-title">Base Image</div>
              <p style={{ fontSize: '0.875rem', color: 'var(--admin-text-muted)', marginTop: 0 }}>
                {image.baseImagePublicId
                  ? <>Base: <Link to={`/admin/images/${image.baseImagePublicId}`} className="text-link">{image.baseImagePublicId}</Link></>
                  : 'No base image set.'}
              </p>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Set base image by Short ID</label>
                <input className="form-control" value={baseImageShortId} onChange={e => setBaseImageShortId(e.target.value)} placeholder="8-digit short ID" style={{ width: 180 }} />
                <div className="form-hint">Links this as a variant of another image.</div>
              </div>
            </div>
          </div>

          {/* Right column */}
          <div>
            {/* Tags */}
            <div className="card">
              <div className="card-title">Tags</div>
              <div className="checkbox-list" style={{ marginBottom: 12 }}>
                {allTags.length === 0 && <span className="text-muted" style={{ fontSize: '0.875rem' }}>No tags yet.</span>}
                {allTags.map(tag => (
                  <label key={tag.publicId} className="checkbox-item">
                    <input
                      type="checkbox"
                      checked={tagPublicIds.has(tag.publicId)}
                      onChange={e => handleToggleTag(tag.publicId, e.target.checked)}
                    />
                    {tag.name}
                    <span className="text-muted" style={{ fontSize: '0.75rem', marginLeft: 4 }}>({tag.imageCount})</span>
                  </label>
                ))}
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  className="form-control"
                  placeholder="New tag name"
                  value={newTagName}
                  onChange={e => setNewTagName(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleCreateTag()}
                  style={{ flex: 1 }}
                />
                <button className="btn btn-secondary btn-sm" onClick={handleCreateTag} disabled={creatingTag || !newTagName.trim()}>
                  {creatingTag ? '…' : '+ Create'}
                </button>
              </div>
            </div>

            {/* Usage */}
            <div className="card">
              <div className="card-title">Usage</div>
              {!usage || usage.galleries.length === 0
                ? <p className="text-muted" style={{ fontSize: '0.875rem', margin: 0 }}>Not used in any gallery.</p>
                : usage.galleries.map(g => (
                  <div key={g.galleryPublicId} style={{ marginBottom: 12 }}>
                    <div style={{ fontWeight: 600, fontSize: '0.875rem', marginBottom: 4 }}>
                      <Link to={`/admin/galleries/${g.galleryPublicId}`} className="text-link">{g.galleryName}</Link>
                      <span className="text-muted" style={{ marginLeft: 6, fontWeight: 400 }}>{g.galleryCode}</span>
                    </div>
                    {g.tours.length === 0
                      ? <div className="text-muted" style={{ fontSize: '0.8rem', paddingLeft: 12 }}>Gallery images (not in any tour)</div>
                      : g.tours.map(t => (
                        <div key={t.tourPublicId} style={{ fontSize: '0.8rem', paddingLeft: 12, color: 'var(--admin-text-muted)' }}>
                          Tour: {t.tourName}
                        </div>
                      ))
                    }
                  </div>
                ))
              }
            </div>
          </div>
        </div>
      </div>

      {/* Delete modal */}
      {showDelete && (
        <div className="modal-overlay" onClick={() => setShowDelete(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-title">Delete Image</div>
            <p style={{ fontSize: '0.875rem', color: 'var(--admin-text-muted)' }}>
              Old shareable links (<code>/{'{code}'}/{image.shortId}</code>) need a substitute. Use <code>00000000</code> to show "image not available".
            </p>
            <div className="form-group">
              <label className="form-label">Substitute Short ID</label>
              <input
                className="form-control"
                value={substituteShortId}
                onChange={e => setSubstituteShortId(e.target.value)}
                placeholder="00000000"
                maxLength={8}
                style={{ width: 180, fontFamily: 'monospace' }}
              />
              <div className="form-hint">8 digits. Use 00000000 for "not available".</div>
            </div>
            {deleteError && <div className="alert alert-danger">{deleteError}</div>}
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowDelete(false)}>Cancel</button>
              <button className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
                {deleting ? 'Deleting…' : 'Delete Image'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function extractError(e: unknown): string {
  if (e && typeof e === 'object' && 'response' in e) {
    const r = (e as { response?: { data?: { detail?: string } } }).response
    if (r?.data?.detail) return r.data.detail
  }
  if (e instanceof Error) return e.message
  return 'An error occurred'
}
