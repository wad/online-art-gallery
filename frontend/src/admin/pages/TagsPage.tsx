import { useEffect, useState } from 'react'
import { listTags, createTag, deleteTag } from '../../api/admin'
import type { TagResponse } from '../../api/types'

export default function TagsPage() {
  const [tags, setTags] = useState<TagResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [newName, setNewName] = useState('')
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    try {
      const res = await listTags()
      setTags(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  async function handleCreate() {
    const name = newName.trim()
    if (!name) return
    setCreating(true)
    setError(null)
    try {
      const res = await createTag(name)
      setTags(prev => [...prev, res.data].sort((a, b) => a.name.localeCompare(b.name)))
      setNewName('')
    } catch (e: unknown) {
      const r = (e as { response?: { data?: { detail?: string } } }).response
      setError(r?.data?.detail ?? 'Failed to create tag')
    } finally {
      setCreating(false)
    }
  }

  async function handleDelete(publicId: string, name: string) {
    if (!confirm(`Delete tag "${name}"? It will be removed from all images.`)) return
    try {
      await deleteTag(publicId)
      setTags(prev => prev.filter(t => t.publicId !== publicId))
    } catch (e: unknown) {
      const r = (e as { response?: { data?: { detail?: string } } }).response
      setError(r?.data?.detail ?? 'Failed to delete tag')
    }
  }

  return (
    <>
      <div className="admin-topbar">
        <h1>Tags</h1>
      </div>
      <div className="admin-content">
        {error && <div className="alert alert-danger">{error}</div>}

        <div className="card" style={{ maxWidth: 500, marginBottom: 20 }}>
          <div className="card-title">Create Tag</div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              className="form-control"
              placeholder="Tag name"
              value={newName}
              onChange={e => setNewName(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleCreate()}
              style={{ flex: 1 }}
            />
            <button className="btn btn-primary" onClick={handleCreate} disabled={creating || !newName.trim()}>
              {creating ? 'Creating…' : '+ Create'}
            </button>
          </div>
        </div>

        <div className="card" style={{ padding: 0 }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th style={{ width: 100 }}>Images</th>
                  <th style={{ width: 80 }}></th>
                </tr>
              </thead>
              <tbody>
                {loading && (
                  <tr><td colSpan={3} style={{ textAlign: 'center', padding: 32, color: 'var(--admin-text-muted)' }}>Loading…</td></tr>
                )}
                {!loading && tags.length === 0 && (
                  <tr><td colSpan={3} style={{ textAlign: 'center', padding: 32, color: 'var(--admin-text-muted)' }}>No tags yet.</td></tr>
                )}
                {tags.map(tag => (
                  <tr key={tag.publicId}>
                    <td><span className="badge badge-gray">{tag.name}</span></td>
                    <td>{tag.imageCount}</td>
                    <td>
                      <button className="btn btn-danger btn-sm" onClick={() => handleDelete(tag.publicId, tag.name)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  )
}
