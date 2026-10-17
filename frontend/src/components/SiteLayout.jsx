import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function Header() {
  const { user, logout, loading } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  return <header className="site-header"><div className="container-fluid header-inner">
    <Link className="wordmark" to="/" aria-label="Spaceora home">spaceora<span>.</span></Link>
    <nav className="desktop-nav" aria-label="Main navigation">
      <NavLink to="/explore">Explore spaces</NavLink><a href="/#how-it-works">How it works</a><a href="/#list-your-space">List your space</a>
    </nav>
    <div className="header-actions">{!loading && (user ? <><Link className="header-profile" to="/profile">{user.firstName || 'Account'}</Link><button className="header-logout" onClick={logout}>Log out</button></> : <><Link className="login-link" to="/login">Log in</Link><Link className="btn btn-primary btn-sm" to="/register">Get started</Link></>)}</div>
    <button className="mobile-menu-trigger" type="button" aria-expanded={menuOpen} aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'} onClick={()=>setMenuOpen(!menuOpen)}>{menuOpen?'×':'☰'}</button>
  </div>{menuOpen&&<nav className="mobile-menu" aria-label="Mobile menu"><Link onClick={()=>setMenuOpen(false)} to="/explore">Explore spaces</Link><a onClick={()=>setMenuOpen(false)} href="/#how-it-works">How it works</a><a onClick={()=>setMenuOpen(false)} href="/#list-your-space">List your space</a><Link onClick={()=>setMenuOpen(false)} to={user?'/profile':'/login'}>{user?'My account':'Log in'}</Link>{!loading&&!user&&<Link onClick={()=>setMenuOpen(false)} to="/register">Get started</Link>}</nav>}</header>
}

export function Footer() {
  return <footer className="site-footer"><div className="container-fluid footer-top"><div><Link className="wordmark" to="/">spaceora<span>.</span></Link><p>Space for your focus, your team and your next big idea.</p><small>Thoughtfully built by team OrbitX.</small></div><div><b>Discover</b><Link to="/explore?type=COWORKING">Coworking desks</Link><Link to="/explore?type=STUDY_ROOM">Study rooms</Link><Link to="/explore?type=MEETING_ROOM">Meeting rooms</Link></div><div><b>Spaceora</b><a href="/#how-it-works">How it works</a><a href="/#list-your-space">List your space</a><Link to="/my-bookings">My bookings</Link><Link to="/profile">My profile</Link></div></div><div className="container-fluid footer-bottom"><span>© 2026 Spaceora. All rights reserved.</span><span>Privacy　 Terms　 INR · English</span></div></footer>
}

export function MobileNav() {
  return <nav className="mobile-bottom" aria-label="Mobile navigation"><Link className={location.pathname.startsWith('/explore') ? 'active' : ''} to="/explore"><i>⌕</i><span>Explore</span></Link><Link className={location.pathname.startsWith('/my-bookings') ? 'active' : ''} to="/my-bookings"><i>▤</i><span>Bookings</span></Link><Link className={location.pathname.startsWith('/saved') ? 'active' : ''} to="/saved"><i>♡</i><span>Saved</span></Link><Link className={location.pathname.startsWith('/profile') ? 'active' : ''} to="/profile"><i>◯</i><span>Profile</span></Link></nav>
}

export function SiteLayout({ children, footer = true }) {
  return <><Header />{children}{footer && <Footer />}<MobileNav /></>
}
