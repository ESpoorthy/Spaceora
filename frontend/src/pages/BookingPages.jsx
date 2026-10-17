import { useState } from 'react'
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom'
import { SiteLayout } from '../components/SiteLayout'
import { spaces } from '../data/spaces'
import { bookingService, workspaceService } from '../api/marketplaceService'

const today = new Date().toISOString().slice(0, 10)

// Build an ISO UTC datetime string from a date string and "HH:MM AM/PM" time string
function toISODatetime(dateStr, timeStr) {
  const [time, meridiem] = timeStr.split(' ')
  let [hours, minutes] = time.split(':').map(Number)
  if (meridiem === 'PM' && hours !== 12) hours += 12
  if (meridiem === 'AM' && hours === 12) hours = 0
  const pad = (n) => String(n).padStart(2, '0')
  return `${dateStr}T${pad(hours)}:${pad(minutes)}:00Z`
}

// Add duration hours to an ISO string
function addHours(isoStr, hours) {
  const d = new Date(isoStr)
  d.setTime(d.getTime() + hours * 60 * 60 * 1000)
  return d.toISOString().replace('.000Z', 'Z')
}

export function BookingReviewPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()

  // Try real backend space first (passed via router state), fall back to mock data
  const [space] = useState(() => {
    if (location.state?.space) return location.state.space
    // mock data fallback (slug-based IDs)
    const mock = spaces.find((item) => item.id === id)
    if (mock) return { ...mock, hourlyRate: mock.price, realId: null }
    return null
  })

  const [date, setDate] = useState(today)
  const [startTime, setStartTime] = useState('10:00 AM')
  const [duration, setDuration] = useState(1)
  const [notes, setNotes] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    // If we have a real backend space ID, call the API
    const realSpaceId = space?.id && !isNaN(Number(space.id)) ? Number(space.id) : space?.realId

    if (!realSpaceId) {
      // Mock-only path — just navigate to confirmation
      navigate('/booking/confirmation', { state: { mock: true, space } })
      return
    }

    setLoading(true)
    try {
      const startDatetime = toISODatetime(date, startTime)
      const endDatetime = addHours(startDatetime, Number(duration))

      const reservation = await bookingService.create({
        spaceId: realSpaceId,
        startDatetime,
        endDatetime,
        notes: notes || undefined,
      })

      navigate('/booking/confirmation', { state: { reservation, space } })
    } catch (err) {
      const msg = err.response?.data?.message || 'Booking failed. Please try a different time slot.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  if (!space) {
    return (
      <SiteLayout>
        <main className="container-fluid empty-state">
          <h1>Space not found</h1>
          <p>That listing may have moved or doesn't exist.</p>
          <Link className="btn btn-primary" to="/explore">Explore spaces</Link>
        </main>
      </SiteLayout>
    )
  }

  const rate = space.hourlyRate || space.price || '—'
  const total = rate !== '—' ? (Number(rate) * Number(duration)).toFixed(2) : '—'

  return (
    <SiteLayout>
      <main className="account-page container-fluid">
        <div className="breadcrumb-line">
          <Link to="/">Home</Link><span>›</span>
          <Link to="/explore">Explore spaces</Link><span>›</span>
          <span>Booking review</span>
        </div>

        <div className="booking-progress">
          <b>01　Review</b>
          <span>02　Confirmation</span>
        </div>

        <h1 className="checkout-title">Review your booking.</h1>

        <div className="checkout-layout">
          <form className="checkout-form surface" onSubmit={handleSubmit}>
            <h2>Your visit</h2>

            <label className="form-label">
              Date
              <input
                className="form-control"
                type="date"
                min={today}
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
              />
            </label>

            <div className="row g-3">
              <div className="col">
                <label className="form-label">
                  Start time
                  <select className="form-select" value={startTime} onChange={(e) => setStartTime(e.target.value)}>
                    <option>08:00 AM</option>
                    <option>09:00 AM</option>
                    <option>10:00 AM</option>
                    <option>11:00 AM</option>
                    <option>12:00 PM</option>
                    <option>01:00 PM</option>
                    <option>02:00 PM</option>
                    <option>03:00 PM</option>
                    <option>04:00 PM</option>
                    <option>05:00 PM</option>
                    <option>06:00 PM</option>
                  </select>
                </label>
              </div>
              <div className="col">
                <label className="form-label">
                  Duration
                  <select className="form-select" value={duration} onChange={(e) => setDuration(e.target.value)}>
                    <option value={1}>1 hour</option>
                    <option value={2}>2 hours</option>
                    <option value={3}>3 hours</option>
                    <option value={4}>4 hours</option>
                    <option value={5}>5 hours</option>
                    <option value={6}>6 hours</option>
                  </select>
                </label>
              </div>
            </div>

            <label className="form-label">
              Note to the host <span className="text-muted">(optional)</span>
              <textarea
                className="form-control"
                rows="3"
                maxLength={500}
                placeholder="Anything the host should know?"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </label>

            {error && (
              <p className="error-copy mt-2" role="alert">{error}</p>
            )}

            <button className="btn btn-primary w-100 mt-3" type="submit" disabled={loading}>
              {loading ? 'Submitting…' : 'Continue to confirmation'}
            </button>
          </form>

          <aside className="checkout-summary surface">
            <img src={space.image || space.thumbnailUrl || '/images/space-01.jpeg'} alt="" />
            <span className="eyebrow">{space.badge || space.type || 'SPACEORA'}</span>
            <h2>{space.name || 'Workspace'}</h2>
            <p>{space.city || ''}</p>
            <hr />
            <div className="fee-row">
              <span>₹{rate} × {duration} hour{duration > 1 ? 's' : ''}</span>
              <span>₹{total}</span>
            </div>
            <div className="fee-row">
              <b>Estimated total</b>
              <b>₹{total}</b>
            </div>
            <p className="booking-fineprint mt-2">
              Total is an estimate. Final amount is calculated server-side.
            </p>
          </aside>
        </div>
      </main>
    </SiteLayout>
  )
}

export function BookingConfirmationPage() {
  const location = useLocation()
  const reservation = location.state?.reservation
  const space = location.state?.space
  const mock = location.state?.mock

  return (
    <SiteLayout>
      <main className="confirmation-page container-fluid">
        <section className="confirmation-card surface">
          {mock || !reservation ? (
            <>
              <div className="confirmation-icon">!</div>
              <span className="eyebrow">PREVIEW MODE</span>
              <h1>Booking preview only.</h1>
              <p>
                This space uses mock data. Log in and book a real space from{' '}
                <Link to="/explore">Explore</Link> to create an actual reservation.
              </p>
              <Link className="btn btn-primary mt-3" to="/explore">Explore spaces</Link>
            </>
          ) : (
            <>
              <div className="confirmation-icon" style={{ background: '#22c55e', color: '#fff' }}>✓</div>
              <span className="eyebrow">BOOKING REQUESTED</span>
              <h1>Your booking is pending.</h1>
              <p>
                The owner will confirm or reject your request. You'll see the status in{' '}
                <Link to="/my-bookings">My Bookings</Link>.
              </p>
              {reservation && (
                <div className="surface mt-3 p-3" style={{ textAlign: 'left', borderRadius: 8 }}>
                  <p><b>Space:</b> {reservation.spaceName}</p>
                  <p><b>From:</b> {new Date(reservation.startDatetime).toLocaleString()}</p>
                  <p><b>To:</b> {new Date(reservation.endDatetime).toLocaleString()}</p>
                  <p><b>Total:</b> ₹{reservation.totalAmount}</p>
                  <p><b>Status:</b> <span style={{ color: '#f59e0b', fontWeight: 600 }}>{reservation.status}</span></p>
                </div>
              )}
              <Link className="btn btn-primary mt-3" to="/my-bookings">View my bookings</Link>
            </>
          )}
        </section>
      </main>
    </SiteLayout>
  )
}
