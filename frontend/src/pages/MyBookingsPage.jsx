import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { SiteLayout } from '../components/SiteLayout'
import { bookingService } from '../api/marketplaceService'

const STATUS_COLOUR = {
  PENDING:   '#f59e0b',
  CONFIRMED: '#22c55e',
  CANCELLED: '#6b7280',
  REJECTED:  '#ef4444',
}

function ReservationCard({ reservation, onCancel }) {
  const [cancelling, setCancelling] = useState(false)
  const [error, setError] = useState('')

  const canCancel =
    (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') &&
    new Date(reservation.startDatetime) > new Date()

  async function handleCancel() {
    if (!window.confirm('Cancel this booking?')) return
    setCancelling(true)
    setError('')
    try {
      await onCancel(reservation.id)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not cancel. Please try again.')
    } finally {
      setCancelling(false)
    }
  }

  return (
    <div className="surface booking-card-item" style={{ padding: '1.25rem', marginBottom: '1rem', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '1.05rem' }}>{reservation.spaceName}</h3>
          <p style={{ margin: '0.25rem 0 0', color: '#6b7280', fontSize: '0.9rem' }}>
            {new Date(reservation.startDatetime).toLocaleString()} –{' '}
            {new Date(reservation.endDatetime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </p>
        </div>
        <span style={{
          fontWeight: 700,
          fontSize: '0.8rem',
          padding: '0.2rem 0.6rem',
          borderRadius: 99,
          background: STATUS_COLOUR[reservation.status] + '22',
          color: STATUS_COLOUR[reservation.status],
          border: `1px solid ${STATUS_COLOUR[reservation.status]}44`,
        }}>
          {reservation.status}
        </span>
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <span style={{ fontWeight: 600 }}>₹{Number(reservation.totalAmount).toFixed(2)}</span>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {canCancel && (
            <button
              className="btn btn-outline-danger btn-sm"
              onClick={handleCancel}
              disabled={cancelling}
            >
              {cancelling ? 'Cancelling…' : 'Cancel'}
            </button>
          )}
        </div>
      </div>
      {reservation.notes && (
        <p style={{ margin: '0.5rem 0 0', fontSize: '0.85rem', color: '#6b7280' }}>
          Note: {reservation.notes}
        </p>
      )}
      {error && <p className="error-copy mt-1" role="alert" style={{ fontSize: '0.85rem' }}>{error}</p>}
    </div>
  )
}

function MyBookingsPage() {
  const [reservations, setReservations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [tab, setTab] = useState('upcoming') // 'upcoming' | 'past'

  useEffect(() => {
    setLoading(true)
    setError('')
    bookingService.mine({ page: 0, size: 100 })
      .then((data) => setReservations(data.content || []))
      .catch((err) => {
        if (err.response?.status === 401) {
          setError('Please log in to see your bookings.')
        } else {
          setError(err.response?.data?.message || 'Could not load bookings. Please try again.')
        }
      })
      .finally(() => setLoading(false))
  }, [])

  async function handleCancel(id) {
    const updated = await bookingService.cancel(id)
    setReservations((prev) =>
      prev.map((r) => (r.id === updated.id ? updated : r))
    )
  }

  const now = new Date()
  const upcoming = reservations.filter(
    (r) => new Date(r.startDatetime) > now && r.status !== 'CANCELLED' && r.status !== 'REJECTED'
  )
  const past = reservations.filter(
    (r) => new Date(r.startDatetime) <= now || r.status === 'CANCELLED' || r.status === 'REJECTED'
  )
  const displayed = tab === 'upcoming' ? upcoming : past

  return (
    <SiteLayout>
      <main className="account-page container-fluid">
        <div className="breadcrumb-line">
          <Link to="/">Home</Link><span>›</span><span>My bookings</span>
        </div>
        <div className="account-heading">
          <div>
            <span className="eyebrow">YOUR SPACEORA ACCOUNT</span>
            <h1>My bookings</h1>
            <p>All your workspace plans, together in one place.</p>
          </div>
          <Link className="btn btn-primary" to="/explore">Find a space</Link>
        </div>

        <div className="booking-tabs" role="tablist">
          <button
            className={tab === 'upcoming' ? 'active' : ''}
            role="tab"
            aria-selected={tab === 'upcoming'}
            onClick={() => setTab('upcoming')}
          >
            Upcoming ({upcoming.length})
          </button>
          <button
            className={tab === 'past' ? 'active' : ''}
            role="tab"
            aria-selected={tab === 'past'}
            onClick={() => setTab('past')}
          >
            Past &amp; cancelled ({past.length})
          </button>
        </div>

        {loading && (
          <div className="surface booking-empty" style={{ textAlign: 'center', padding: '3rem' }}>
            <span className="spinner-border" role="status" />
            <p style={{ marginTop: '1rem' }}>Loading your bookings…</p>
          </div>
        )}

        {!loading && error && (
          <section className="booking-empty surface">
            <div className="empty-illustration">!</div>
            <h2>Could not load bookings</h2>
            <p className="error-copy">{error}</p>
            <Link className="btn btn-primary mt-3" to="/login">Sign in</Link>
          </section>
        )}

        {!loading && !error && displayed.length === 0 && (
          <section className="booking-empty surface">
            <div className="empty-illustration">▤</div>
            <h2>{tab === 'upcoming' ? 'No upcoming bookings.' : 'No past bookings.'}</h2>
            <p>
              {tab === 'upcoming'
                ? 'Find a workspace and make your first reservation.'
                : 'Your completed and cancelled reservations will appear here.'}
            </p>
            {tab === 'upcoming' && (
              <Link className="btn btn-primary mt-3" to="/explore">Explore workspaces</Link>
            )}
          </section>
        )}

        {!loading && !error && displayed.length > 0 && (
          <section style={{ marginTop: '1.5rem' }}>
            {displayed.map((r) => (
              <ReservationCard key={r.id} reservation={r} onCancel={handleCancel} />
            ))}
          </section>
        )}
      </main>
    </SiteLayout>
  )
}

export default MyBookingsPage
