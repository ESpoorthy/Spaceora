import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Footer, Header } from '../components/SiteLayout'
import WorkspaceCard from '../components/WorkspaceCard'
import { featuredSpaces, spaceTypes } from '../data/spaces'

function SearchForm({ compact = false }) {
  const navigate = useNavigate()
  const [city, setCity] = useState('Hyderabad')
  const [type, setType] = useState('')
  const [capacity, setCapacity] = useState('')
  function submit(event) { event.preventDefault(); const query = new URLSearchParams(); if (city.trim()) query.set('city', city.trim()); if (type) query.set('type', type); if (capacity) query.set('minCapacity', capacity); navigate(`/explore?${query}`) }
  return <form className={`home-search surface ${compact ? 'home-search-compact' : ''}`} onSubmit={submit} aria-label="Search workspaces">
    <label><span>Where</span><input aria-label="City" value={city} onChange={(e) => setCity(e.target.value)} placeholder="City or neighbourhood" /></label>
    <label><span>Space</span><select aria-label="Workspace type" value={type} onChange={(e) => setType(e.target.value)}>{spaceTypes.map((t) => <option key={t.value} value={t.value}>{t.value ? t.label : 'Any type'}</option>)}</select></label>
    <label><span>Guests</span><select aria-label="Minimum capacity" value={capacity} onChange={(e) => setCapacity(e.target.value)}><option value="">Any size</option>{[2,4,8,12,16].map((n) => <option key={n} value={n}>{n}+ guests</option>)}</select></label>
    <button className="btn btn-primary" type="submit"><span aria-hidden="true">⌕</span> Find a space</button>
  </form>
}

function HomePage() {
  const cities = [
    { name: 'Hyderabad', area: 'HITEC City · Jubilee Hills', image: '/images/space-01.jpeg', city: 'Hyderabad' },
    { name: 'Bengaluru', area: 'Indiranagar · Koramangala', image: '/images/space-02.png', city: 'Bengaluru' },
    { name: 'Pune', area: 'Baner · Viman Nagar', image: '/images/space-09.png', city: 'Pune' },
  ]
  return <><Header /><main>
    <section className="home-hero container-fluid"><div className="hero-copy"><span className="eyebrow">A LITTLE SPACE. A LOT OF POSSIBILITY.</span><h1>Find your space.<br />Make room for<br />something great.</h1><p>From a quiet corner to your next team offsite, discover thoughtful workspaces. Book by the hour. Make it yours.</p><div className="hero-assurance"><span>◉ Flexible hourly booking</span><span>▣ Clear, upfront prices</span></div></div><div className="hero-visual"><img src="/images/space-01.jpeg" alt="Sunlit modern meeting room with city views" /><div className="hero-caption"><div><b>The Atrium, Hyderabad</b><span>Room for fresh perspectives. 8 guests.</span></div><Link to="/spaces/atrium" aria-label="See The Atrium">↗</Link></div></div><SearchForm /></section>
    <section className="container-fluid section home-categories"><h2>Whatever’s on your agenda.</h2><div className="category-pills">{spaceTypes.map((type) => <Link className={`tag ${!type.value ? 'active' : ''}`} key={type.label} to={type.value ? `/explore?type=${type.value}` : '/explore'}>{type.label}</Link>)}</div></section>
    <section className="container-fluid section home-featured"><div className="section-heading"><div><h2>Spaces worth stepping into.</h2><p className="section-subtitle">Thoughtful places for focused days and brighter ideas.</p></div><Link className="text-link" to="/explore">Explore all spaces ↗</Link></div><div className="row g-3">{featuredSpaces.map((space) => <div className="col-md-4" key={space.id}><WorkspaceCard space={space} /></div>)}</div><p className="fixture-note">Development preview · sample spaces</p></section>
    <section className="container-fluid section city-section"><div className="section-heading"><div><h2>Your city. A new way to work.</h2><p className="section-subtitle">A change of scenery, without going far.</p></div></div><div className="row g-3">{cities.map((city) => <div className="col-md-4" key={city.name}><Link className="city-card" to={`/explore?city=${city.city}`}><img src={city.image} alt={`${city.name} workspace`} /><span className="city-name">{city.name}<i>↗</i></span><small>{city.area}</small></Link></div>)}</div></section>
    <section className="container-fluid section"><div className="benefit-panel"><h2>A better place to get there.</h2><div className="row g-4 mt-1"><div className="col-md-4"><span className="benefit-icon">◷</span><b>Your time, your terms</b><p>Book just the hours you need. No long leases, no unnecessary commitments.</p></div><div className="col-md-4"><span className="benefit-icon">◎</span><b>Know before you go</b><p>See the space, amenities and capacity before you make a booking.</p></div><div className="col-md-4"><span className="benefit-icon">▣</span><b>Nothing hidden</b><p>Hourly rates and any service fee, clearly shown before you pay.</p></div></div></div></section>
    <section id="how-it-works" className="container-fluid section how-section"><span className="eyebrow">LESS PLANNING. MORE DOING.</span><h2>From “where?” to “see you there.”</h2><div className="row g-4 steps-row"><div className="col-md-4"><em>01</em><b>Find your fit</b><p>Choose a city, your kind of space and a time that works.</p></div><div className="col-md-4"><em>02</em><b>Make it yours</b><p>Review your slot, guest count and the complete price.</p></div><div className="col-md-4"><em>03</em><b>Show up inspired</b><p>Get your booking details, head over and settle in.</p></div></div><div className="story-block"><img src="/images/space-05.jpeg" alt="Open coworking studio with natural light" /><div><span className="eyebrow">A CHANGE OF SPACE</span><h2>Fresh surroundings. Fresh thinking.</h2><p>A desk by the window. A room for the whole team. A quiet place to finally finish that chapter. Good spaces don’t just hold your work — they help it happen.</p><Link className="btn btn-outline-dark btn-sm" to="/explore">Discover your next workspace</Link></div></div></section>
    <section id="list-your-space" className="container-fluid"><div className="owner-banner"><div><span className="eyebrow">FOR SPACE OWNERS</span><h2>Your extra space. Someone’s next possibility.</h2><p>List your workspace, set your availability and welcome a new way to earn.</p></div><Link className="btn btn-primary" to="/register?role=OWNER">List your space</Link></div></section>
    <section className="container-fluid section faq-section"><h2>A few things you<br />might be wondering.</h2><div><details><summary>Can I book a space by the hour?</summary><p>Yes. Choose an available slot and see the hourly rate and total before confirming. Booking requests become real only when the booking service is available.</p></details><details><summary>What’s included in my booking?</summary><p>The amenities listed on each space page are included. Check the space details for equipment, access and guest capacity.</p></details><details><summary>Can I cancel a reservation?</summary><p>Cancellation terms are shown before booking. Your reservations will appear in My Bookings when the booking API is connected.</p></details></div></section>
  </main><Footer /></>
}
export default HomePage
