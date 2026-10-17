import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { SiteLayout } from '../components/SiteLayout'
import WorkspaceCard from '../components/WorkspaceCard'
import { spaces } from '../data/spaces'
import { getSavedSpaceIds, toggleSavedSpace } from '../data/savedSpaces'

function SavedPage(){const[ids,setIds]=useState(getSavedSpaceIds);useEffect(()=>{const update=()=>setIds(getSavedSpaceIds());window.addEventListener('spaceora:saved-spaces-updated',update);return()=>window.removeEventListener('spaceora:saved-spaces-updated',update)},[]);const items=spaces.filter((space)=>ids.includes(space.id));return <SiteLayout><main className="account-page container-fluid"><div className="breadcrumb-line"><Link to="/">Home</Link><span>›</span><span>Saved spaces</span></div><div className="account-heading"><div><span className="eyebrow">YOUR SPACEORA PICKS</span><h1>Saved spaces</h1><p>Keep the places you like close at hand.</p></div><Link className="btn btn-primary" to="/explore">Explore spaces</Link></div>{items.length?<div className="row g-3">{items.map((space)=><div className="col-md-6 col-xl-4" key={space.id}><WorkspaceCard space={space}/><button className="btn btn-link remove-saved" onClick={()=>toggleSavedSpace(space.id)}>Remove from saved</button></div>)}</div>:<section className="booking-empty surface"><div className="empty-illustration">♡</div><h2>Somewhere worth coming back to.</h2><p>Save a space from its detail page and it’ll be waiting here.</p><Link className="btn btn-primary mt-3" to="/explore">Explore workspaces</Link></section>}</main></SiteLayout>}
export default SavedPage
