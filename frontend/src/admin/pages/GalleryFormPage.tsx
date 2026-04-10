import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import {
  getGallery, createGallery, updateGallery, getThemes,
  listImages, addImagesToGallery, removeImageFromGallery,
  listTours, createTour, updateTour, deleteTour, reorderTours,
  exportGallery, importGallery,
} from '../../api/admin'
import type { GalleryResponse, ThemeOption, ImageResponse, TourResponse, BorderStyle, GalleryTheme } from '../../api/types'

const BORDER_STYLES: Array<{ value: BorderStyle | ''; label: string }> = [
  { value: '', label: 'Theme Default' },
  { value: 'NONE', label: 'None' },
  { value: 'THIN_LINE', label: 'Thin Line' },
  { value: 'DOUBLE_LINE', label: 'Double Line' },
  { value: 'SHADOW', label: 'Shadow' },
  { value: 'ROUNDED', label: 'Rounded' },
  { value: 'ORNATE_FRAME', label: 'Ornate Frame' },
  { value: 'POLAROID', label: 'Polaroid' },
]

export default function GalleryFormPage() {
  const { publicId } = useParams<{ publicId: string }>()
  const isNew = publicId === 'new'
  const navigate = useNavigate()

  const [loading, setLoading] = useState(!isNew)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [themes, setThemes] = useState<ThemeOption[]>([])
  const [activeTab, setActiveTab] = useState<'details' | 'images' | 'tours' | 'export'>('details')

  // Form state
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [subtitle, setSubtitle] = useState('')
  const [description, setDescription] = useState('')
  const [theme, setTheme] = useState<GalleryTheme>('LIGHT')
  const [borderStyle, setBorderStyle] = useState<BorderStyle | ''>('')
  const [visible, setVisible] = useState(true)
  const [showAllImagesTour, setShowAllImagesTour] = useState(true)
  const [bioPhotoUrl, setBioPhotoUrl] = useState('')
  const [bioText, setBioText] = useState('')
  const [bioLinks, setBioLinks] = useState<Array<{ label: string; url: string }>>([])
  const [adsEnabled, setAdsEnabled] = useState(false)
  const [adLandingBanner, setAdLandingBanner] = useState(false)
  const [adLandingBannerSlot, setAdLandingBannerSlot] = useState('')
  const [adImageDetailSidebar, setAdImageDetailSidebar] = useState(false)
  const [adImageDetailSidebarSlot, setAdImageDetailSidebarSlot] = useState('')

  // Images tab
  const [galleryImages, setGalleryImages] = useState<ImageResponse[]>([])
  const [allImages, setAllImages] = useState<ImageResponse[]>([])
  const [imageSearch, setImageSearch] = useState('')
  const [addingImage, setAddingImage] = useState(false)
  const [imagesLoaded, setImagesLoaded] = useState(false)

  // Tours tab
  const [tours, setTours] = useState<TourResponse[]>([])
  const [toursLoaded, setToursLoaded] = useState(false)
  const [showTourForm, setShowTourForm] = useState(false)
  const [editingTour, setEditingTour] = useState<TourResponse | null>(null)
  const [tourName, setTourName] = useState('')
  const [tourDesc, setTourDesc] = useState('')
  const [savingTour, setSavingTour] = useState(false)
  const [draggingTour, setDraggingTour] = useState<string | null>(null)

  // Export/import
  const [importJson, setImportJson] = useState('')
  const [importError, setImportError] = useState<string | null>(null)
  const [importing, setImporting] = useState(false)

  const [gallery, setGallery] = useState<GalleryResponse | null>(null)

  useEffect(() => {
    getThemes().then(r => setThemes(r.data))
    if (!isNew && publicId) {
      getGallery(publicId).then(r => {
        const g = r.data
        setGallery(g)
        setCode(g.code)
        setName(g.name)
        setSubtitle(g.subtitle ?? '')
        setDescription(g.description ?? '')
        setTheme(g.theme)
        setBorderStyle(g.borderStyle ?? '')
        setVisible(g.visible)
        setShowAllImagesTour(g.showAllImagesTour)
        setBioPhotoUrl(g.bioPhotoUrl ?? '')
        setBioText(g.bioText ?? '')
        setBioLinks(g.bioLinks ?? [])
        setAdsEnabled(g.adsEnabled)
        setAdLandingBanner(g.adLandingBanner)
        setAdLandingBannerSlot(g.adLandingBannerSlot ?? '')
        setAdImageDetailSidebar(g.adImageDetailSidebar)
        setAdImageDetailSidebarSlot(g.adImageDetailSidebarSlot ?? '')
      }).finally(() => setLoading(false))
    }
  }, [isNew, publicId])

  const loadImages = useCallback(async () => {
    if (!publicId || isNew || imagesLoaded) return
    const [gallRes, allRes] = await Promise.all([
      listImages({ tag: undefined, page: 0, size: 200, sort: 'uploadedAt', dir: 'desc' }),
      listImages({ page: 0, size: 500 }),
    ])
    // Gallery images are those with this gallery's publicId in their galleryPublicIds
    const gImgs = gallRes.data.content.filter(img => img.galleryPublicIds.includes(publicId!))
    setGalleryImages(gImgs)
    setAllImages(allRes.data.content)
    setImagesLoaded(true)
  }, [publicId, isNew, imagesLoaded])

  const loadTours = useCallback(async () => {
    if (!publicId || isNew || toursLoaded) return
    const res = await listTours(publicId)
    setTours(res.data)
    setToursLoaded(true)
  }, [publicId, isNew, toursLoaded])

  useEffect(() => {
    if (activeTab === 'images') loadImages()
    if (activeTab === 'tours') loadTours()
  }, [activeTab, loadImages, loadTours])

  async function handleSave() {
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const payload = {
        code,
        name,
        subtitle: subtitle || null,
        description: description || null,
        theme,
        borderStyle: borderStyle || null,
        visible,
        showAllImagesTour,
        bioPhotoUrl: bioPhotoUrl || null,
        bioText: bioText || null,
        bioLinks,
        adsEnabled,
        adLandingBanner,
        adLandingBannerSlot: adLandingBannerSlot || null,
        adImageDetailSidebar,
        adImageDetailSidebarSlot: adImageDetailSidebarSlot || null,
      }
      if (isNew) {
        const res = await createGallery(payload)
        navigate(`/admin/galleries/${res.data.publicId}`)
      } else {
        await updateGallery(publicId!, payload)
        setSuccess('Saved.')
      }
    } catch (e: unknown) {
      const r = (e as { response?: { data?: { detail?: string } } }).response
      setError(r?.data?.detail ?? 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  async function handleAddImage(imgPublicId: string) {
    if (!publicId) return
    setAddingImage(true)
    try {
      await addImagesToGallery(publicId, [imgPublicId])
      const img = allImages.find(i => i.publicId === imgPublicId)
      if (img) setGalleryImages(prev => [...prev, { ...img, galleryPublicIds: [...img.galleryPublicIds, publicId] }])
    } finally {
      setAddingImage(false)
    }
  }

  async function handleRemoveImage(imgPublicId: string) {
    if (!publicId) return
    try {
      await removeImageFromGallery(publicId, imgPublicId)
      setGalleryImages(prev => prev.filter(i => i.publicId !== imgPublicId))
    } catch (e: unknown) {
      const r = (e as { response?: { data?: { detail?: string } } }).response
      alert(r?.data?.detail ?? 'Remove failed')
    }
  }

  async function handleSaveTour() {
    if (!publicId) return
    setSavingTour(true)
    try {
      if (editingTour) {
        const res = await updateTour(publicId, editingTour.publicId, { name: tourName, description: tourDesc || null })
        setTours(prev => prev.map(t => t.publicId === editingTour.publicId ? res.data : t))
      } else {
        const res = await createTour(publicId, { name: tourName, description: tourDesc || null, tagPublicIds: [] })
        setTours(prev => [...prev, res.data])
      }
      setShowTourForm(false)
      setEditingTour(null)
      setTourName('')
      setTourDesc('')
    } catch (e: unknown) {
      const r = (e as { response?: { data?: { detail?: string } } }).response
      alert(r?.data?.detail ?? 'Save failed')
    } finally {
      setSavingTour(false)
    }
  }

  async function handleDeleteTour(tourPublicId: string, tourName: string) {
    if (!publicId) return
    if (!confirm(`Delete tour "${tourName}"?`)) return
    await deleteTour(publicId, tourPublicId)
    setTours(prev => prev.filter(t => t.publicId !== tourPublicId))
  }

  function startEditTour(t: TourResponse) {
    setEditingTour(t)
    setTourName(t.name)
    setTourDesc(t.description ?? '')
    setShowTourForm(true)
  }

  function handleTourDragStart(id: string) { setDraggingTour(id) }
  function handleTourDragOver(e: React.DragEvent, targetId: string) {
    e.preventDefault()
    if (!draggingTour || draggingTour === targetId) return
    const from = tours.findIndex(t => t.publicId === draggingTour)
    const to = tours.findIndex(t => t.publicId === targetId)
    if (from === -1 || to === -1) return
    const next = [...tours]
    next.splice(to, 0, next.splice(from, 1)[0])
    setTours(next)
  }
  async function handleTourDrop() {
    if (!publicId) return
    setDraggingTour(null)
    await reorderTours(publicId, tours.map((t, i) => ({ publicId: t.publicId, sortOrder: i })))
  }

  async function handleExport() {
    if (!publicId) return
    const res = await exportGallery(publicId)
    const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `gallery-${code}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  async function handleImport() {
    setImporting(true)
    setImportError(null)
    try {
      const data = JSON.parse(importJson)
      await importGallery(data)
      navigate('/admin/galleries')
    } catch (e: unknown) {
      if (e instanceof SyntaxError) {
        setImportError('Invalid JSON')
      } else {
        const r = (e as { response?: { data?: { detail?: string } } }).response
        setImportError(r?.data?.detail ?? 'Import failed')
      }
    } finally {
      setImporting(false)
    }
  }

  function addBioLink() { setBioLinks(prev => [...prev, { label: '', url: '' }]) }
  function removeBioLink(i: number) { setBioLinks(prev => prev.filter((_, j) => j !== i)) }
  function updateBioLink(i: number, field: 'label' | 'url', val: string) {
    setBioLinks(prev => prev.map((l, j) => j === i ? { ...l, [field]: val } : l))
  }

  const notInGallery = allImages.filter(img =>
    !galleryImages.some(gi => gi.publicId === img.publicId) &&
    (imageSearch === '' ||
      (img.title ?? '').toLowerCase().includes(imageSearch.toLowerCase()) ||
      img.shortId.includes(imageSearch))
  ).slice(0, 20)

  if (loading) return <div className="admin-content" style={{ padding: 40, textAlign: 'center', color: 'var(--admin-text-muted)' }}>Loading…</div>

  return (
    <>
      <div className="admin-topbar">
        <div className="flex items-center gap-2">
          <Link to="/admin/galleries" className="btn btn-secondary btn-sm">← Back</Link>
          <h1 style={{ margin: 0 }}>{isNew ? 'New Gallery' : (gallery?.name ?? 'Edit Gallery')}</h1>
        </div>
        {activeTab === 'details' && (
          <button className="btn btn-primary btn-sm" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving…' : (isNew ? 'Create Gallery' : 'Save Changes')}
          </button>
        )}
      </div>

      <div className="admin-content">
        {error && <div className="alert alert-danger">{error}</div>}
        {success && <div className="alert alert-success">✓ {success}</div>}

        {!isNew && (
          <div className="tabs">
            {(['details', 'images', 'tours', 'export'] as const).map(tab => (
              <div key={tab} className={`tab ${activeTab === tab ? 'active' : ''}`} onClick={() => setActiveTab(tab)}>
                {tab.charAt(0).toUpperCase() + tab.slice(1)}
              </div>
            ))}
          </div>
        )}

        {/* Details tab */}
        {activeTab === 'details' && (
          <div className="grid-2" style={{ gap: 24, alignItems: 'start' }}>
            <div>
              <div className="card">
                <div className="card-title">Basic Info</div>
                <div className="form-group">
                  <label className="form-label">Gallery Code</label>
                  <input className="form-control" value={code} onChange={e => setCode(e.target.value.toUpperCase())} placeholder="e.g. MAIN" maxLength={5} style={{ width: 120, fontFamily: 'monospace', fontWeight: 600 }} />
                  <div className="form-hint">1–5 uppercase letters/numbers. Used in visitor URLs: /{code || 'CODE'}</div>
                </div>
                <div className="form-group">
                  <label className="form-label">Name</label>
                  <input className="form-control" value={name} onChange={e => setName(e.target.value)} />
                </div>
                <div className="form-group">
                  <label className="form-label">Subtitle</label>
                  <input className="form-control" value={subtitle} onChange={e => setSubtitle(e.target.value)} />
                </div>
                <div className="form-group">
                  <label className="form-label">Description</label>
                  <textarea className="form-control" rows={3} value={description} onChange={e => setDescription(e.target.value)} />
                </div>
                <div className="form-group">
                  <label className="checkbox-item">
                    <input type="checkbox" checked={visible} onChange={e => setVisible(e.target.checked)} />
                    Visible to visitors (show on /galleries page)
                  </label>
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="checkbox-item">
                    <input type="checkbox" checked={showAllImagesTour} onChange={e => setShowAllImagesTour(e.target.checked)} />
                    Show "All Images" virtual tour
                  </label>
                </div>
              </div>

              <div className="card">
                <div className="card-title">Bio Section</div>
                <div className="form-group">
                  <label className="form-label">Bio Photo URL</label>
                  <input className="form-control" value={bioPhotoUrl} onChange={e => setBioPhotoUrl(e.target.value)} placeholder="https://…" />
                </div>
                <div className="form-group">
                  <label className="form-label">Bio Text</label>
                  <textarea className="form-control" rows={4} value={bioText} onChange={e => setBioText(e.target.value)} />
                </div>
                <div className="form-label" style={{ marginBottom: 8 }}>Bio Links</div>
                {bioLinks.map((link, i) => (
                  <div key={i} style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
                    <input className="form-control" placeholder="Label" value={link.label} onChange={e => updateBioLink(i, 'label', e.target.value)} style={{ flex: '0 0 120px' }} />
                    <input className="form-control" placeholder="https://…" value={link.url} onChange={e => updateBioLink(i, 'url', e.target.value)} style={{ flex: 1 }} />
                    <button className="btn btn-secondary btn-sm" onClick={() => removeBioLink(i)}>×</button>
                  </div>
                ))}
                <button className="btn btn-secondary btn-sm" onClick={addBioLink} style={{ marginTop: 4 }}>+ Add Link</button>
              </div>
            </div>

            <div>
              <div className="card">
                <div className="card-title">Appearance</div>
                <div className="form-group">
                  <label className="form-label">Theme</label>
                  <select className="form-control" value={theme} onChange={e => setTheme(e.target.value as GalleryTheme)}>
                    {themes.length > 0
                      ? themes.map(t => <option key={t.value} value={t.value}>{t.displayName}</option>)
                      : ['LIGHT','DARK','PASTEL','SPRING','WINTER','CYBERPUNK','SUNSET','OCEAN','MONOCHROME'].map(t =>
                          <option key={t} value={t}>{t}</option>
                        )
                    }
                  </select>
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Border Style Override</label>
                  <select className="form-control" value={borderStyle} onChange={e => setBorderStyle(e.target.value as BorderStyle | '')}>
                    {BORDER_STYLES.map(bs => <option key={bs.value} value={bs.value}>{bs.label}</option>)}
                  </select>
                </div>
              </div>

              <div className="card">
                <div className="card-title">Ad Settings</div>
                <div className="form-group">
                  <label className="checkbox-item">
                    <input type="checkbox" checked={adsEnabled} onChange={e => setAdsEnabled(e.target.checked)} />
                    Enable ads for this gallery
                  </label>
                </div>
                {adsEnabled && (
                  <>
                    <div className="form-group">
                      <label className="checkbox-item">
                        <input type="checkbox" checked={adLandingBanner} onChange={e => setAdLandingBanner(e.target.checked)} />
                        Landing page banner ad
                      </label>
                      {adLandingBanner && (
                        <input className="form-control" value={adLandingBannerSlot} onChange={e => setAdLandingBannerSlot(e.target.value)} placeholder="AdSense slot ID" style={{ marginTop: 6 }} />
                      )}
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label className="checkbox-item">
                        <input type="checkbox" checked={adImageDetailSidebar} onChange={e => setAdImageDetailSidebar(e.target.checked)} />
                        Image detail sidebar ad
                      </label>
                      {adImageDetailSidebar && (
                        <input className="form-control" value={adImageDetailSidebarSlot} onChange={e => setAdImageDetailSidebarSlot(e.target.value)} placeholder="AdSense slot ID" style={{ marginTop: 6 }} />
                      )}
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Images tab */}
        {activeTab === 'images' && !isNew && (
          <div>
            <div className="card">
              <div className="card-title">Images in this Gallery ({galleryImages.length})</div>
              {galleryImages.length === 0
                ? <p className="text-muted">No images yet.</p>
                : (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {galleryImages.map(img => (
                      <div key={img.publicId} style={{ position: 'relative', width: 80 }}>
                        {img.thumbnailUrl
                          ? <img src={img.thumbnailUrl} alt="" style={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 4, border: '1px solid var(--admin-border)' }} />
                          : <div className="thumb-missing" style={{ width: 80, height: 80 }}>no thumb</div>}
                        <div style={{ fontSize: '0.65rem', color: 'var(--admin-text-muted)', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {img.title ?? img.shortId}
                        </div>
                        <button
                          onClick={() => handleRemoveImage(img.publicId)}
                          style={{ position: 'absolute', top: 2, right: 2, width: 18, height: 18, borderRadius: '50%', background: 'rgba(220,53,69,0.85)', color: '#fff', border: 'none', cursor: 'pointer', fontSize: '0.7rem', lineHeight: 1, padding: 0 }}
                        >×</button>
                      </div>
                    ))}
                  </div>
                )
              }
            </div>

            <div className="card">
              <div className="card-title">Add Images</div>
              <input
                className="form-control"
                placeholder="Search by title or short ID…"
                value={imageSearch}
                onChange={e => setImageSearch(e.target.value)}
                style={{ marginBottom: 12, maxWidth: 300 }}
              />
              {notInGallery.length === 0
                ? <p className="text-muted" style={{ fontSize: '0.875rem' }}>No images to add.</p>
                : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th style={{ width: 60 }}>Thumb</th>
                          <th>Title</th>
                          <th>Short ID</th>
                          <th style={{ width: 60 }}></th>
                        </tr>
                      </thead>
                      <tbody>
                        {notInGallery.map(img => (
                          <tr key={img.publicId}>
                            <td>{img.thumbnailUrl ? <img src={img.thumbnailUrl} className="thumb" alt="" /> : <div className="thumb-missing">no thumb</div>}</td>
                            <td>{img.title ?? <span className="text-muted">(untitled)</span>}</td>
                            <td style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{img.shortId}</td>
                            <td>
                              <button className="btn btn-primary btn-sm" onClick={() => handleAddImage(img.publicId)} disabled={addingImage}>+</button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )
              }
            </div>
          </div>
        )}

        {/* Tours tab */}
        {activeTab === 'tours' && !isNew && (
          <div>
            <div className="card" style={{ marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <div className="card-title" style={{ margin: 0 }}>Tours</div>
                <button className="btn btn-primary btn-sm" onClick={() => { setShowTourForm(true); setEditingTour(null); setTourName(''); setTourDesc('') }}>
                  + New Tour
                </button>
              </div>

              {showTourForm && (
                <div className="card" style={{ background: 'var(--admin-surface-2)', marginBottom: 16 }}>
                  <div className="card-title">{editingTour ? 'Edit Tour' : 'New Tour'}</div>
                  <div className="form-group">
                    <label className="form-label">Tour Name</label>
                    <input className="form-control" value={tourName} onChange={e => setTourName(e.target.value)} autoFocus />
                  </div>
                  <div className="form-group" style={{ marginBottom: 12 }}>
                    <label className="form-label">Description</label>
                    <textarea className="form-control" rows={2} value={tourDesc} onChange={e => setTourDesc(e.target.value)} />
                  </div>
                  <div className="flex gap-2">
                    <button className="btn btn-primary btn-sm" onClick={handleSaveTour} disabled={savingTour || !tourName.trim()}>
                      {savingTour ? 'Saving…' : 'Save'}
                    </button>
                    <button className="btn btn-secondary btn-sm" onClick={() => { setShowTourForm(false); setEditingTour(null) }}>Cancel</button>
                  </div>
                </div>
              )}

              {tours.length === 0
                ? <p className="text-muted" style={{ margin: 0, fontSize: '0.875rem' }}>No tours yet. Create one to group images into a guided experience.</p>
                : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th style={{ width: 32 }}></th>
                          <th>Name</th>
                          <th>Description</th>
                          <th style={{ width: 80 }}>Images</th>
                          <th style={{ width: 120 }}></th>
                        </tr>
                      </thead>
                      <tbody>
                        {tours.map(t => (
                          <tr
                            key={t.publicId}
                            draggable
                            onDragStart={() => handleTourDragStart(t.publicId)}
                            onDragOver={e => handleTourDragOver(e, t.publicId)}
                            onDrop={handleTourDrop}
                            style={{ opacity: draggingTour === t.publicId ? 0.5 : 1, cursor: 'grab' }}
                          >
                            <td style={{ color: 'var(--admin-text-muted)', textAlign: 'center' }}>⠿</td>
                            <td>{t.name}</td>
                            <td style={{ fontSize: '0.8rem', color: 'var(--admin-text-muted)' }}>{t.description ?? '—'}</td>
                            <td>{t.imagePublicIds.length}</td>
                            <td>
                              <div className="flex gap-2">
                                <button className="btn btn-secondary btn-sm" onClick={() => startEditTour(t)}>Edit</button>
                                <button className="btn btn-danger btn-sm" onClick={() => handleDeleteTour(t.publicId, t.name)}>Del</button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )
              }
              <div className="form-hint" style={{ marginTop: 8 }}>Drag to reorder tours.</div>
            </div>
          </div>
        )}

        {/* Export/Import tab */}
        {activeTab === 'export' && !isNew && (
          <div className="grid-2" style={{ gap: 24 }}>
            <div className="card">
              <div className="card-title">Export Gallery</div>
              <p style={{ fontSize: '0.875rem', color: 'var(--admin-text-muted)' }}>
                Download this gallery's configuration (tours, settings, image references) as JSON.
              </p>
              <button className="btn btn-secondary" onClick={handleExport}>⬇ Export JSON</button>
            </div>
            <div className="card">
              <div className="card-title">Import Gallery</div>
              <p style={{ fontSize: '0.875rem', color: 'var(--admin-text-muted)' }}>
                Paste a previously exported gallery JSON to create a new gallery from it.
              </p>
              <textarea
                className="form-control"
                rows={8}
                value={importJson}
                onChange={e => setImportJson(e.target.value)}
                placeholder='{ "formatVersion": 1, … }'
                style={{ fontFamily: 'monospace', fontSize: '0.75rem', marginBottom: 8 }}
              />
              {importError && <div className="alert alert-danger">{importError}</div>}
              <button className="btn btn-primary" onClick={handleImport} disabled={importing || !importJson.trim()}>
                {importing ? 'Importing…' : '⬆ Import'}
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  )
}
