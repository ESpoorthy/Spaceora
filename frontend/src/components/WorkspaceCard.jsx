import { Link } from 'react-router-dom'

const labels = { COWORKING: 'COWORKING DESK', STUDY_ROOM: 'STUDY ROOM', MEETING_ROOM: 'MEETING ROOM', TRAINING_HALL: 'TRAINING HALL' }
export default function WorkspaceCard({ space }) {
  const location = [space.area, space.city].filter(Boolean).join(' · ')
  const perks = [...(space.amenities || []).slice(0, 2), `${space.capacity} guests`].join(' · ')
  const price = Number(space.price ?? 0)
  return <article className="workspace-card"><Link to={`/spaces/${space.id}`} className="workspace-image-link" aria-label={`View ${space.name}`}><img src={space.image || '/images/space-01.jpeg'} alt={`${space.name} workspace`} /><span className="image-open">↗</span></Link><div className="workspace-card-copy"><span className="eyebrow">{labels[space.type] || space.type}{space.badge ? ` · ${space.badge}` : ''}</span><Link to={`/spaces/${space.id}`}><h3>{space.name}</h3></Link><p>{location}<br />{perks}</p><div className="workspace-card-price"><strong>₹{price.toLocaleString('en-IN')} <small>/ hour</small></strong><span>Available</span></div></div></article>
}
