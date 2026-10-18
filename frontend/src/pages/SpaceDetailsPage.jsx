import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { SiteLayout } from '../components/SiteLayout'
import { workspaceService, mapSpaceDetail } from '../api/marketplaceService'
import { getSavedSpaceIds, toggleSavedSpace } from '../data/savedSpaces'

const today = new Date().toISOString().slice(0,10)
function SpaceDetailsPage() {
  const { id } = useParams(); const navigate = useNavigate()
  const [space, setSpace] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeImage, setActiveImage] = useState(0)
  const [saved, setSaved] = useState(() => getSavedSpaceIds().includes(id))
  const [shareMessage, setShareMessage] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true); setError(''); setSpace(null)
    workspaceService.getById(id)
      .then((dto) => { if (active) setSpace(mapSpaceDetail(dto)) })
      .catch((err) => { if (active) setError(err.response?.status === 404 ? 'That listing may have moved.' : (err.response?.data?.message || 'Couldn’t load this space. Please try again.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  async function share() { try { if (!space) return; if (navigator.share) await navigator.share({ title: space.name, url: window.location.href }); else { await navigator.clipboard.writeText(window.location.href); setShareMessage('Link copied to clipboard.') } } catch (error) { if (error.name !== 'AbortError') setShareMessage('Sharing is not available in this browser.') } }

  if (loading) return <SiteLayout><main className="container-fluid empty-state" role="status"><span className="spinner-border" />Loading this space…</main></SiteLayout>
  if (error || !space) return <SiteLayout><main className="container-fluid empty-state"><h1>Space not found</h1><p>{error || 'That listing may have moved.'}</p><Link className="btn btn-primary" to="/explore">Explore spaces</Link></main></SiteLayout>
  const images = [space.image, space.image2, space.image3]
  return <SiteLayout><main className="container-fluid details-page"><div className="breadcrumb-line"><Link to="/">Home</Link><span>›</span><Link to="/explore">Explore spaces</Link><span>›</span><span>{space.name}</span></div><div className="detail-title"><div><span className="eyebrow">{space.badge}</span><h1>{space.name}</h1><p>⌖　{space.address || space.city} · {space.city}</p></div><div className="detail-actions"><button className={`tag ${saved ? 'active' : ''}`} onClick={() => setSaved(toggleSavedSpace(space.id).includes(space.id))} aria-pressed={saved}>{saved ? '♥ Saved' : '♡ Save'}</button><button className="tag" onClick={share}>↗ Share</button></div></div>{shareMessage&&<p className="share-message" role="status">{shareMessage}</p>}
    <div className="gallery-grid"><button className="gallery-main" onClick={() => setActiveImage((activeImage+1)%images.length)} aria-label="Show next workspace photo"><img src={images[activeImage]} alt={`${space.name}, photo ${activeImage+1}`} /><span>▣　{activeImage+1} / {images.length}</span></button><button className="gallery-side" onClick={() => setActiveImage((activeImage+1)%images.length)} aria-label="Show another workspace photo"><img src={images[(activeImage+1)%images.length]} alt={`${space.name}, workspace view`} /></button><button className="gallery-side" onClick={() => setActiveImage((activeImage+2)%images.length)} aria-label="Show another workspace photo"><img src={images[(activeImage+2)%images.length]} alt={`${space.name}, seating view`} /></button></div>
    <div className="details-columns"><section className="detail-copy"><div className="detail-summary"><div><h2>{space.type.replace('_',' ').toLowerCase()} in {space.area || space.city}</h2><p>{space.capacity} guests · Hosted workspace</p></div><span className="host-avatar">S</span></div><hr /><h2>About this space</h2><p className="long-copy">{space.description || 'Details for this workspace will appear here once the owner adds them.'}</p><hr /><h2>What this place offers</h2><div className="amenity-grid">{(space.amenities || []).map((item) => <span key={item}>✓　{item}</span>)}<span>✓　Natural light</span><span>✓　Comfortable seating</span></div><hr /><h2>Location</h2><div className="location-placeholder">⌖<span>{space.area || space.city}, {space.city}</span><small>Map preview</small></div></section>
      <aside className="booking-card surface"><div className="rate-line"><b>₹{space.price}</b><span>/ hour</span><span className="review-mark">★ 4.9</span></div><div className="booking-inputs"><label className="form-label">Date<input className="form-control" type="date" min={today} /></label><div className="row g-2"><div className="col"><label className="form-label">Start time<select className="form-select"><option>10:00 AM</option><option>11:00 AM</option><option>1:00 PM</option><option>2:00 PM</option></select></label></div><div className="col"><label className="form-label">Guests<select className="form-select">{Array.from({length:space.capacity},(_,i)=><option key={i+1}>{i+1}</option>)}</select></label></div></div></div><button className="btn btn-primary w-100" onClick={() => navigate(`/spaces/${space.id}/book`, { state: { space } })}>Review booking</button><p className="booking-fineprint">Final amount is calculated server-side when you confirm.</p><hr /><div className="fee-row"><span>₹{space.price} × 1 hour</span><span>₹{space.price}</span></div><div className="fee-row"><b>Estimate</b><b>₹{space.price}</b></div></aside>
    </div>
  </main></SiteLayout>
}
export default SpaceDetailsPage
