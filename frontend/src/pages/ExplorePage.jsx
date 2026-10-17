import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { SiteLayout } from '../components/SiteLayout'
import { DevPreviewNotice } from '../components/FeatureNotice'
import WorkspaceCard from '../components/WorkspaceCard'
import { spaces, spaceTypes } from '../data/spaces'

const defaultDate = '2026-10-18'
function ExplorePage() {
  const [query, setQuery] = useSearchParams()
  const [city, setCity] = useState(query.get('city') || 'Hyderabad')
  const [date, setDate] = useState(query.get('date') || defaultDate)
  const [start, setStart] = useState(query.get('from') || '10:00')
  const [end, setEnd] = useState(query.get('until') || '12:00')
  const [type, setType] = useState(query.get('type') || '')
  const [minimum, setMinimum] = useState(query.get('minPrice') || '0')
  const [maximum, setMaximum] = useState(query.get('maxPrice') || '2000')
  const [capacity, setCapacity] = useState(query.get('minCapacity') || '8')
  const [amenities, setAmenities] = useState(query.get('amenities')?.split(',') || ['Wi-Fi'])
  const [sort, setSort] = useState(query.get('sort') || 'recommended')
  const [filtersOpen, setFiltersOpen] = useState(false)

  const visibleSpaces = useMemo(() => {
    const items = spaces.filter((space) => (!city || `${space.city} ${space.area}`.toLowerCase().includes(city.toLowerCase())) && (!type || space.type === type) && (!capacity || space.capacity >= Number(capacity)) && space.price >= Number(minimum || 0) && (!maximum || space.price <= Number(maximum)) && amenities.every((amenity) => space.amenities.includes(amenity)))
    if (sort === 'price-asc') items.sort((a,b) => a.price - b.price)
    if (sort === 'price-desc') items.sort((a,b) => b.price - a.price)
    return items
  }, [city, type, capacity, minimum, maximum, amenities, sort])

  function apply(event) {
    event.preventDefault()
    const next = new URLSearchParams()
    if (city) next.set('city', city)
    if (date) next.set('date', date)
    if (start) next.set('from', start)
    if (end) next.set('until', end)
    if (type) next.set('type', type)
    if (minimum) next.set('minPrice', minimum)
    if (maximum) next.set('maxPrice', maximum)
    if (capacity) next.set('minCapacity', capacity)
    if (amenities.length) next.set('amenities', amenities.join(','))
    if (sort !== 'recommended') next.set('sort', sort)
    setQuery(next)
    setFiltersOpen(false)
  }
  function clear() { setCity(''); setDate(''); setStart(''); setEnd(''); setType(''); setMinimum('0'); setMaximum(''); setCapacity(''); setAmenities([]); setSort('recommended'); setQuery({}) }
  function toggleAmenity(amenity) { setAmenities((current) => current.includes(amenity) ? current.filter((item) => item !== amenity) : [...current, amenity]) }
  const dateLabel = date === defaultDate ? 'Sun, 18 Oct' : date ? `${date.slice(8)} ${['','Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][Number(date.slice(5,7))]} ${date.slice(0,4)}` : 'Any date'
  const formatTime = (value) => { if (!value) return ''; const [hour, minute] = value.split(':').map(Number); return `${hour % 12 || 12}${minute ? `:${String(minute).padStart(2,'0')}` : ''} ${hour < 12 ? 'AM' : 'PM'}` }
  const timeLabel = start && end ? `${formatTime(start)} – ${formatTime(end)}` : 'Any time'
  const filterCount = Number(Boolean(type)) + Number(Boolean(capacity)) + amenities.length

  return <SiteLayout><main className="explore-page container-fluid"><div className="breadcrumb-line"><Link to="/">Home</Link><span>›</span><span>Explore spaces</span></div>
    <header className="explore-title"><span className="eyebrow">ROOM FOR WHAT’S NEXT</span><h1>A space for your next good idea.</h1><p>{city || 'Everywhere'} · {dateLabel} · {timeLabel} · {capacity ? `${capacity}+ guests` : 'Any group size'}</p></header>
    <div className="mobile-search-summary"><b>{city || 'Choose a city'}</b><span>{dateLabel} · {timeLabel} · {capacity ? `${capacity}+ guests` : 'Any size'}</span></div>
    <div className="explore-toolbar-mobile"><button className="tag" onClick={() => setFiltersOpen(!filtersOpen)}>☷ Filters {filterCount ? `· ${filterCount}` : ''}</button><label className="tag">⌄ <select aria-label="Sort results" value={sort} onChange={(e) => setSort(e.target.value)}><option value="recommended">Recommended</option><option value="price-asc">Price: low to high</option><option value="price-desc">Price: high to low</option></select></label></div>
    <div className="explore-layout"><form className={`explore-sidebar surface ${filtersOpen ? 'mobile-open' : ''}`} onSubmit={apply}>
      <h2>Find your fit</h2><label className="filter-field"><span>City</span><input className="form-control" aria-label="City" value={city} onChange={(e)=>setCity(e.target.value)} placeholder="City or neighbourhood" /></label>
      <label className="filter-field"><span>Date</span><input className="form-control" aria-label="Date" type="date" value={date} onChange={(e)=>setDate(e.target.value)} /></label>
      <div className="filter-time"><label><span>From</span><input className="form-control" type="time" aria-label="From" value={start} onChange={(e)=>setStart(e.target.value)} /></label><label><span>Until</span><input className="form-control" type="time" aria-label="Until" value={end} onChange={(e)=>setEnd(e.target.value)} /></label></div>
      <fieldset className="filter-group"><legend>Space type</legend>{spaceTypes.map((item)=><label className="filter-radio" key={item.value}><input type="radio" name="spaceType" value={item.value} checked={type===item.value} onChange={()=>setType(item.value)} /><span>{item.label}</span></label>)}</fieldset>
      <fieldset className="filter-group"><legend>Hourly budget</legend><div className="budget-range"><label><span>Min ₹</span><input className="form-control" aria-label="Minimum price" type="number" min="0" value={minimum} onChange={(e)=>setMinimum(e.target.value)} /></label><label><span>Max ₹</span><input className="form-control" aria-label="Maximum price" type="number" min="0" value={maximum} onChange={(e)=>setMaximum(e.target.value)} /></label></div></fieldset>
      <label className="filter-field"><span>Minimum capacity</span><select className="form-select" value={capacity} onChange={(e)=>setCapacity(e.target.value)}>{[['','Any group size'],['2','2 guests'],['4','4 guests'],['8','8 guests'],['12','12 guests'],['16','16 guests']].map(([value,label])=><option value={value} key={value}>{label}</option>)}</select></label>
      <fieldset className="filter-group amenity-filter"><legend>Amenities</legend>{['Wi-Fi','Whiteboard','Projector','Parking'].map((item)=><label className="filter-radio" key={item}><input type="checkbox" checked={amenities.includes(item)} onChange={()=>toggleAmenity(item)} /><span>{item}</span></label>)}</fieldset>
      <button className="btn btn-primary w-100" type="submit">Apply filters</button><button className="btn btn-link reset-button w-100" type="button" onClick={clear}>Clear all</button>
    </form>
    <section className="explore-results"><div className="results-desktop-bar"><b>{visibleSpaces.length} sample {visibleSpaces.length===1?'space':'spaces'}</b><label>⌄ <select aria-label="Sort results" value={sort} onChange={(e)=>setSort(e.target.value)}><option value="recommended">Sort: Recommended</option><option value="price-asc">Price: low to high</option><option value="price-desc">Price: high to low</option></select></label></div><div className="active-filter-tags">{capacity&&<span className="tag active">{capacity}+ guests　×</span>}{amenities.map((item)=><span className="tag active" key={item}>{item}　×</span>)}<button onClick={clear}>Clear all</button></div><div className="availability-preview">Availability can’t be checked until the workspace and availability API is available.</div><DevPreviewNotice />
      {visibleSpaces.length?<div className="row g-3 explore-card-grid">{visibleSpaces.map((space)=><div className="col-12 col-xl-6" key={space.id}><WorkspaceCard space={space}/></div>)}</div>:<div className="empty-state surface"><span>⌕</span><h2>No sample spaces match those filters.</h2><p>Try a different city, capacity or budget.</p><button className="btn btn-primary" onClick={clear}>Clear filters</button></div>}
    </section></div></main></SiteLayout>
}
export default ExplorePage
