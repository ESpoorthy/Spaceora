import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { SiteLayout } from '../components/SiteLayout'
import { bookingService, ownerService } from '../api/marketplaceService'
import axios from '../api/axiosInstance'

/* ------------------------------------------------------------------ */
/* Shared shell                                                         */
/* ------------------------------------------------------------------ */
function OwnerShell({ title, subtitle, children, action }) {
  return (
    <SiteLayout>
      <main className="account-page owner-page container-fluid">
        <div className="breadcrumb-line">
          <Link to="/">Home</Link><span>›</span><span>Owner workspace</span>
        </div>
        <div className="owner-top">
          <div>
            <span className="eyebrow">YOUR SPACEORA BUSINESS</span>
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>
          {action}
        </div>
        <nav className="owner-tabs" aria-label="Owner tools">
          <NavLink end to="/owner/dashboard">Overview</NavLink>
          <NavLink to="/owner/listings">My listings</NavLink>
          <NavLink to="/owner/availability">Availability</NavLink>
          <NavLink to="/owner/bookings">Bookings</NavLink>
        </nav>
        {children}
      </main>
    </SiteLayout>
  )
}

/* ------------------------------------------------------------------ */
/* Owner Dashboard                                                      */
/* ------------------------------------------------------------------ */
export function OwnerDashboardPage() {
  const [spaces, setSpaces] = useState([])
  const [recentBookings, setRecentBookings] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      axios.get('/api/spaces/owner/me').then((r) => r.data),
      // Fetch recent PENDING reservations across all spaces
    ])
      .then(([ownedSpaces]) => {
        setSpaces(ownedSpaces)
        // Collect pending reservations from first 3 spaces for the overview
        const fetches = ownedSpaces.slice(0, 3).map((s) =>
          bookingService.forSpace(s.id, { status: 'PENDING', page: 0, size: 5 })
            .then((d) => d.content || [])
            .catch(() => [])
        )
        return Promise.all(fetches)
      })
      .then((arrays) => setRecentBookings(arrays.flat()))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const activeCount = spaces.filter((s) => s.isActive).length

  return (
    <OwnerShell
      title="A clearer view of your space."
      subtitle="Manage your listings and stay on top of every booking."
      action={<Link className="btn btn-primary" to="/owner/listings/new">＋ Add a space</Link>}
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem' }}>
          <span className="spinner-border" /><p style={{ marginTop: '1rem' }}>Loading…</p>
        </div>
      ) : (
        <>
          <div className="owner-metrics row g-3">
            {[
              ['Active spaces', activeCount],
              ['Pending requests', recentBookings.length],
              ['Total listings', spaces.length],
            ].map(([name, value]) => (
              <div className="col-md-4" key={name}>
                <div className="metric-card surface">
                  <span>{name}</span>
                  <b>{value}</b>
                </div>
              </div>
            ))}
          </div>

          <div className="owner-content-grid">
            <section className="owner-panel surface">
              <div className="panel-title">
                <h2>Your spaces</h2>
                <Link to="/owner/listings">View all</Link>
              </div>
              {spaces.length === 0 ? (
                <div className="owner-empty">
                  <span>⌂</span>
                  <h3>Your first space starts here.</h3>
                  <p>Add your workspace to welcome your first guest.</p>
                  <Link className="btn btn-outline-dark" to="/owner/listings/new">Add a space</Link>
                </div>
              ) : (
                <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                  {spaces.slice(0, 5).map((s) => (
                    <li key={s.id} style={{ padding: '0.6rem 0', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between' }}>
                      <span>{s.name}</span>
                      <span style={{ color: s.isActive ? '#22c55e' : '#6b7280', fontSize: '0.8rem', fontWeight: 600 }}>
                        {s.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="owner-panel surface">
              <div className="panel-title">
                <h2>Pending requests</h2>
                <Link to="/owner/bookings">See all</Link>
              </div>
              {recentBookings.length === 0 ? (
                <div className="owner-empty compact">
                  <span>▤</span>
                  <p>New booking requests will appear here.</p>
                </div>
              ) : (
                <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                  {recentBookings.slice(0, 5).map((r) => (
                    <li key={r.id} style={{ padding: '0.6rem 0', borderBottom: '1px solid #f0f0f0' }}>
                      <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{r.spaceName}</div>
                      <div style={{ color: '#6b7280', fontSize: '0.82rem' }}>
                        {new Date(r.startDatetime).toLocaleString()} · ₹{Number(r.totalAmount).toFixed(2)}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      )}
    </OwnerShell>
  )
}

/* ------------------------------------------------------------------ */
/* Owner Listings                                                       */
/* ------------------------------------------------------------------ */
export function OwnerListingsPage() {
  const path = useLocation().pathname
  const navigate = useNavigate()
  const editing = path.endsWith('/new')
  const [spaces, setSpaces] = useState([])
  const [loading, setLoading] = useState(!editing)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    name: '', description: '', type: 'MEETING_ROOM', city: '', address: '',
    hourlyRate: '', capacity: '', imageUrl: '',
  })

  useEffect(() => {
    if (editing) return
    axios.get('/api/spaces/owner/me')
      .then((r) => setSpaces(r.data))
      .catch(() => setError('Could not load your listings.'))
      .finally(() => setLoading(false))
  }, [editing])

  async function handleSave(e) {
    e.preventDefault()
    setSaving(true); setError('')
    try {
      const payload = {
        name: form.name,
        description: form.description || undefined,
        type: form.type,
        city: form.city,
        address: form.address,
        hourlyRate: parseFloat(form.hourlyRate),
        capacity: parseInt(form.capacity, 10),
      }
      await ownerService.createWorkspace(payload)
      navigate('/owner/listings')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save listing.')
    } finally {
      setSaving(false)
    }
  }

  const field = (key) => ({
    value: form[key],
    onChange: (e) => setForm((f) => ({ ...f, [key]: e.target.value })),
  })

  return (
    <OwnerShell
      title={editing ? 'Add a space' : 'Manage listings'}
      subtitle={editing ? 'Share the details guests need to find your workspace.' : 'Keep your workspaces and listing details up to date.'}
      action={!editing && <Link className="btn btn-primary" to="/owner/listings/new">＋ Add a space</Link>}
    >
      {editing ? (
        <form className="listing-form surface" onSubmit={handleSave}>
          <h2>Space details</h2>
          <div className="row g-3">
            <div className="col-md-8">
              <label className="form-label">Space name
                <input className="form-control" required maxLength={120} {...field('name')} />
              </label>
            </div>
            <div className="col-md-4">
              <label className="form-label">Type
                <select className="form-select" {...field('type')}>
                  <option value="MEETING_ROOM">Meeting room</option>
                  <option value="COWORKING">Coworking</option>
                  <option value="STUDY_ROOM">Study room</option>
                  <option value="TRAINING_HALL">Training hall</option>
                </select>
              </label>
            </div>
            <div className="col-12">
              <label className="form-label">Description
                <textarea className="form-control" rows="4" maxLength={2000} {...field('description')} />
              </label>
            </div>
            <div className="col-md-6">
              <label className="form-label">City
                <input className="form-control" required {...field('city')} />
              </label>
            </div>
            <div className="col-md-6">
              <label className="form-label">Address
                <input className="form-control" required {...field('address')} />
              </label>
            </div>
            <div className="col-md-4">
              <label className="form-label">Hourly rate (₹)
                <input className="form-control" type="number" min="1" required {...field('hourlyRate')} />
              </label>
            </div>
            <div className="col-md-4">
              <label className="form-label">Guest capacity
                <input className="form-control" type="number" min="1" required {...field('capacity')} />
              </label>
            </div>
          </div>
          {error && <p className="error-copy mt-3" role="alert">{error}</p>}
          <div className="form-actions">
            <Link className="btn btn-outline-dark" to="/owner/listings">Cancel</Link>
            <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save listing'}</button>
          </div>
        </form>
      ) : loading ? (
        <div style={{ textAlign: 'center', padding: '3rem' }}><span className="spinner-border" /></div>
      ) : spaces.length === 0 ? (
        <section className="owner-panel surface">
          <div className="owner-empty">
            <span>⌂</span><h2>No listings yet</h2>
            <p>Add your first workspace to start accepting bookings.</p>
            <Link className="btn btn-outline-dark" to="/owner/listings/new">Add a space</Link>
          </div>
        </section>
      ) : (
        <section className="owner-panel surface">
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {spaces.map((s) => (
              <li key={s.id} style={{ padding: '1rem 0', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div>
                  <div style={{ fontWeight: 600 }}>{s.name}</div>
                  <div style={{ color: '#6b7280', fontSize: '0.85rem' }}>{s.type} · {s.city} · ₹{Number(s.hourlyRate).toFixed(0)}/hr · {s.capacity} guests</div>
                </div>
                <span style={{ color: s.isActive ? '#22c55e' : '#6b7280', fontWeight: 600, fontSize: '0.82rem' }}>
                  {s.isActive ? 'Active' : 'Inactive'}
                </span>
              </li>
            ))}
          </ul>
          {error && <p className="error-copy mt-2" role="alert">{error}</p>}
        </section>
      )}
    </OwnerShell>
  )
}

/* ------------------------------------------------------------------ */
/* Owner Availability (blocked periods)                                 */
/* ------------------------------------------------------------------ */
export function OwnerAvailabilityPage() {
  const [spaces, setSpaces] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [blockedPeriods, setBlockedPeriods] = useState([])
  const [schedule, setSchedule] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ startDatetime: '', endDatetime: '', reason: '' })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    axios.get('/api/spaces/owner/me').then((r) => {
      setSpaces(r.data)
      if (r.data.length > 0) setSelectedId(String(r.data[0].id))
    }).catch(() => {})
  }, [])

  useEffect(() => {
    if (!selectedId) return
    setBlockedPeriods([]); setSchedule([]); setError('')
    Promise.all([
      axios.get(`/api/spaces/${selectedId}/blocked-periods`).then((r) => r.data).catch(() => []),
      axios.get(`/api/spaces/${selectedId}/availability/schedule`).then((r) => r.data).catch(() => []),
    ]).then(([bp, sched]) => { setBlockedPeriods(bp); setSchedule(sched) })
  }, [selectedId])

  async function handleAddBlocked(e) {
    e.preventDefault(); setSaving(true); setError('')
    try {
      const start = new Date(form.startDatetime).toISOString()
      const end = new Date(form.endDatetime).toISOString()
      const created = await ownerService.addBlockedPeriod(Number(selectedId), {
        startDatetime: start, endDatetime: end, reason: form.reason || undefined,
      })
      setBlockedPeriods((prev) => [...prev, created])
      setShowForm(false)
      setForm({ startDatetime: '', endDatetime: '', reason: '' })
    } catch (err) {
      setError(err.response?.data?.message || 'Could not add blocked period.')
    } finally { setSaving(false) }
  }

  const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

  return (
    <OwnerShell
      title="Set the rhythm of your space."
      subtitle="Choose when guests can book and block time when you need it."
      action={selectedId && <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close' : '＋ Block time'}</button>}
    >
      <div className="availability-toolbar surface">
        <label className="form-label">Choose a listing
          <select className="form-select" value={selectedId} onChange={(e) => setSelectedId(e.target.value)} disabled={spaces.length === 0}>
            {spaces.length === 0
              ? <option>No listings available</option>
              : spaces.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)
            }
          </select>
        </label>
      </div>

      {/* Weekly schedule */}
      {schedule.length > 0 && (
        <section className="surface" style={{ padding: '1rem 1.25rem', marginBottom: '1rem' }}>
          <h3 style={{ marginBottom: '0.75rem', fontSize: '1rem' }}>Weekly opening hours</h3>
          <div className="calendar-days">
            {DAYS.map((day, i) => {
              const entry = schedule.find((s) => s.dayOfWeek === i)
              return (
                <div key={day}>
                  <b>{day}</b>
                  <span style={{ fontSize: '0.8rem' }}>
                    {entry ? `${entry.openTime}–${entry.closeTime}` : 'Closed'}
                  </span>
                </div>
              )
            })}
          </div>
        </section>
      )}

      {/* Block form */}
      {showForm && (
        <form className="blocked-form surface" onSubmit={handleAddBlocked}>
          <h2>Block time</h2>
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label">Start
                <input className="form-control" type="datetime-local" required value={form.startDatetime} onChange={(e) => setForm((f) => ({ ...f, startDatetime: e.target.value }))} />
              </label>
            </div>
            <div className="col-md-4">
              <label className="form-label">End
                <input className="form-control" type="datetime-local" required value={form.endDatetime} onChange={(e) => setForm((f) => ({ ...f, endDatetime: e.target.value }))} />
              </label>
            </div>
            <div className="col-md-4">
              <label className="form-label">Reason
                <input className="form-control" maxLength={200} placeholder="Optional" value={form.reason} onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} />
              </label>
            </div>
          </div>
          {error && <p className="error-copy mt-2" role="alert">{error}</p>}
          <button className="btn btn-primary mt-3" disabled={saving}>{saving ? 'Saving…' : 'Save blocked time'}</button>
        </form>
      )}

      {/* Blocked periods list */}
      <section className="availability-calendar surface">
        <h3 style={{ marginBottom: '0.75rem', fontSize: '1rem' }}>Blocked periods</h3>
        {blockedPeriods.length === 0 ? (
          <div className="calendar-empty">
            <span>◷</span>
            <h2>No blocked periods.</h2>
            <p>Use the button above to block time for maintenance or holidays.</p>
          </div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {blockedPeriods.map((bp) => (
              <li key={bp.id} style={{ padding: '0.6rem 0', borderBottom: '1px solid #f0f0f0', fontSize: '0.9rem' }}>
                <span>{new Date(bp.startDatetime).toLocaleString()} – {new Date(bp.endDatetime).toLocaleString()}</span>
                {bp.reason && <span style={{ color: '#6b7280', marginLeft: '0.5rem' }}>({bp.reason})</span>}
              </li>
            ))}
          </ul>
        )}
      </section>
    </OwnerShell>
  )
}

/* ------------------------------------------------------------------ */
/* Owner Bookings                                                       */
/* ------------------------------------------------------------------ */
const STATUS_COLOUR = { PENDING: '#f59e0b', CONFIRMED: '#22c55e', CANCELLED: '#6b7280', REJECTED: '#ef4444' }

export function OwnerBookingsPage() {
  const [spaces, setSpaces] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [reservations, setReservations] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [acting, setActing] = useState(null) // reservation id being actioned

  useEffect(() => {
    axios.get('/api/spaces/owner/me').then((r) => {
      setSpaces(r.data)
      if (r.data.length > 0) setSelectedId(String(r.data[0].id))
    }).catch(() => {})
  }, [])

  useEffect(() => {
    if (!selectedId) return
    setLoading(true); setError('')
    bookingService.forSpace(Number(selectedId), { page: 0, size: 50 })
      .then((d) => setReservations(d.content || []))
      .catch((err) => setError(err.response?.data?.message || 'Could not load reservations.'))
      .finally(() => setLoading(false))
  }, [selectedId])

  async function handleStatus(reservationId, status) {
    setActing(reservationId)
    try {
      const updated = await bookingService.updateStatus(reservationId, status)
      setReservations((prev) => prev.map((r) => (r.id === updated.id ? updated : r)))
    } catch (err) {
      alert(err.response?.data?.message || 'Could not update status.')
    } finally { setActing(null) }
  }

  return (
    <OwnerShell title="Every request, in one place." subtitle="Review guest requests and keep plans moving.">
      <div className="availability-toolbar surface">
        <label className="form-label">Space
          <select className="form-select" value={selectedId} onChange={(e) => setSelectedId(e.target.value)} disabled={spaces.length === 0}>
            {spaces.length === 0
              ? <option>No listings available</option>
              : spaces.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)
            }
          </select>
        </label>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: '2rem' }}><span className="spinner-border" /></div>}
      {error && <p className="error-copy mt-2" role="alert">{error}</p>}

      {!loading && !error && reservations.length === 0 && (
        <section className="owner-panel surface">
          <div className="owner-empty">
            <span>▤</span>
            <h2>No booking requests</h2>
            <p>Reservation requests for this space will appear here.</p>
          </div>
        </section>
      )}

      {!loading && reservations.length > 0 && (
        <section className="owner-panel surface">
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {reservations.map((r) => (
              <li key={r.id} style={{ padding: '1rem 0', borderBottom: '1px solid #f0f0f0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>Booking #{r.id}</div>
                    <div style={{ color: '#6b7280', fontSize: '0.85rem' }}>
                      {new Date(r.startDatetime).toLocaleString()} – {new Date(r.endDatetime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                    <div style={{ fontSize: '0.85rem', marginTop: '0.2rem' }}>
                      ₹{Number(r.totalAmount).toFixed(2)}
                      {r.notes && <span style={{ color: '#6b7280', marginLeft: '0.5rem' }}>· {r.notes}</span>}
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                    <span style={{
                      fontWeight: 700, fontSize: '0.78rem', padding: '0.2rem 0.5rem', borderRadius: 99,
                      background: (STATUS_COLOUR[r.status] || '#ccc') + '22',
                      color: STATUS_COLOUR[r.status] || '#333',
                      border: `1px solid ${(STATUS_COLOUR[r.status] || '#ccc')}44`,
                    }}>
                      {r.status}
                    </span>
                    {r.status === 'PENDING' && (
                      <>
                        <button
                          className="btn btn-success btn-sm"
                          disabled={acting === r.id}
                          onClick={() => handleStatus(r.id, 'CONFIRMED')}
                        >
                          {acting === r.id ? '…' : 'Confirm'}
                        </button>
                        <button
                          className="btn btn-outline-danger btn-sm"
                          disabled={acting === r.id}
                          onClick={() => handleStatus(r.id, 'REJECTED')}
                        >
                          Reject
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}
    </OwnerShell>
  )
}
