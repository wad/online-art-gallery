import { useState } from 'react'
import { getGalleryStats, getTourStats, getImageStats } from '../../api/admin'
import type { ViewStatsResponse } from '../../api/types'

type Tab = 'galleries' | 'tours' | 'images'

function fmt(n: number) { return n.toLocaleString() }

function defaultFrom() {
  const d = new Date()
  d.setDate(d.getDate() - 30)
  return d.toISOString().slice(0, 10)
}
function defaultTo() { return new Date().toISOString().slice(0, 10) }

export default function StatsPage() {
  const [tab, setTab] = useState<Tab>('galleries')
  const [from, setFrom] = useState(defaultFrom())
  const [to, setTo] = useState(defaultTo())
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<ViewStatsResponse[]>([])
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleLoad() {
    setLoading(true)
    setError(null)
    try {
      let res
      if (tab === 'galleries') res = await getGalleryStats(from, to)
      else if (tab === 'tours') res = await getTourStats(from, to)
      else res = await getImageStats(from, to)
      setData(res.data)
      setLoaded(true)
    } catch (e: unknown) {
      const r = (e as { response?: { data?: { detail?: string } } }).response
      setError(r?.data?.detail ?? 'Failed to load stats')
    } finally {
      setLoading(false)
    }
  }

  function handleTabChange(t: Tab) {
    setTab(t)
    setLoaded(false)
    setData([])
  }

  return (
    <>
      <div className="admin-topbar">
        <h1>View Statistics</h1>
      </div>
      <div className="admin-content">
        <div className="tabs">
          {(['galleries', 'tours', 'images'] as Tab[]).map(t => (
            <div key={t} className={`tab ${tab === t ? 'active' : ''}`} onClick={() => handleTabChange(t)}>
              {t.charAt(0).toUpperCase() + t.slice(1)}
            </div>
          ))}
        </div>

        <div className="card" style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">From</label>
            <input type="date" className="form-control" value={from} onChange={e => setFrom(e.target.value)} style={{ width: 160 }} />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">To</label>
            <input type="date" className="form-control" value={to} onChange={e => setTo(e.target.value)} style={{ width: 160 }} />
          </div>
          <button className="btn btn-primary" onClick={handleLoad} disabled={loading}>
            {loading ? 'Loading…' : 'Load Stats'}
          </button>
          {/* Quick ranges */}
          {[7, 30, 90].map(days => (
            <button key={days} className="btn btn-secondary btn-sm" onClick={() => {
              const d = new Date()
              const f = new Date(d)
              f.setDate(f.getDate() - days)
              setFrom(f.toISOString().slice(0, 10))
              setTo(d.toISOString().slice(0, 10))
            }}>
              Last {days}d
            </button>
          ))}
        </div>

        {error && <div className="alert alert-danger">{error}</div>}

        {loaded && data.length === 0 && (
          <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--admin-text-muted)' }}>
            No views recorded in this period.
          </div>
        )}

        {loaded && data.length > 0 && (
          <div className="card" style={{ padding: 0 }}>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Name</th>
                    {tab === 'galleries' && <th>Code</th>}
                    {tab === 'images' && <th>Short ID</th>}
                    <th style={{ textAlign: 'right' }}>Total</th>
                    {tab === 'images' && <th style={{ textAlign: 'right' }}>Direct</th>}
                    {tab === 'images' && <th style={{ textAlign: 'right' }}>From Tour</th>}
                  </tr>
                </thead>
                <tbody>
                  {data.map((row, i) => (
                    <tr key={i}>
                      <td style={{ color: 'var(--admin-text-muted)', width: 40 }}>{i + 1}</td>
                      <td>{row.entityName}</td>
                      {tab === 'galleries' && <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{row.entityCode}</td>}
                      {tab === 'images' && <td style={{ fontFamily: 'monospace' }}>{row.entityShortId}</td>}
                      <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(row.totalViews)}</td>
                      {tab === 'images' && <td style={{ textAlign: 'right' }}>{fmt(row.directViews)}</td>}
                      {tab === 'images' && <td style={{ textAlign: 'right' }}>{fmt(row.tourViews)}</td>}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </>
  )
}
