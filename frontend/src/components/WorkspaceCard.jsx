import { Link } from 'react-router-dom'

const labels = { COWORKING: 'COWORKING DESK', STUDY_ROOM: 'STUDY ROOM', MEETING_ROOM: 'MEETING ROOM', TRAINING_HALL: 'TRAINING HALL' }
export default function WorkspaceCard({ space }) {
  return <article className="workspace-card"><Link to={`/spaces/${space.id}`} className="workspace-image-link" aria-label={`View ${space.name}`}><img src={space.image} alt={`${space.name} workspace`} /><span className="image-open">↗</span></Link><div className="workspace-card-copy"><span className="eyebrow">{labels[space.type] || space.type} · {space.badge}</span><Link to={`/spaces/${space.id}`}><h3>{space.name}</h3></Link><p>{space.area} · {space.city}<br />{space.amenities.slice(0, 2).join(' · ')} · {space.capacity} guests</p><div className="workspace-card-price"><strong>₹{space.price.toLocaleString('en-IN')} <small>/ hour</small></strong><span>Preview</span></div></div></article>
}
