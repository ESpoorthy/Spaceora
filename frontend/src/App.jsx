import { BrowserRouter, Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import HomePage from './pages/HomePage'
import ExplorePage from './pages/ExplorePage'
import SpaceDetailsPage from './pages/SpaceDetailsPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import NotFoundPage from './pages/NotFoundPage'
import ProfilePage from './pages/ProfilePage'
import MyBookingsPage from './pages/MyBookingsPage'
import SavedPage from './pages/SavedPage'
import { BookingConfirmationPage, BookingReviewPage } from './pages/BookingPages'
import { OwnerAvailabilityPage, OwnerBookingsPage, OwnerDashboardPage, OwnerListingsPage } from './pages/OwnerPages'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './hooks/useAuth'
import './App.css'
import './navigation.css'
import './auth.css'

function RequireAuth({ children, roles }) {
  const { token, user, loading } = useAuth(); const location = useLocation()
  if (loading) return <div className="full-page-loading" role="status"><span className="spinner-border" />Loading your account…</div>
  if (!token || !user) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/forbidden" replace />
  return children
}
function ForbiddenPage() { return <main className="empty-state"><span>403</span><h1>That page isn’t available to this account.</h1><p>Sign in with an account that has access, or return to Explore.</p><Link className="btn btn-primary" to="/explore">Explore spaces</Link></main> }

function AppRoutes() {
  return <Routes><Route path="/" element={<HomePage />} /><Route path="/explore" element={<ExplorePage />} /><Route path="/spaces/:id" element={<SpaceDetailsPage />} /><Route path="/spaces/:id/book" element={<RequireAuth roles={['CUSTOMER']}><BookingReviewPage /></RequireAuth>} /><Route path="/booking/confirmation" element={<RequireAuth roles={['CUSTOMER']}><BookingConfirmationPage /></RequireAuth>} /><Route path="/login" element={<LoginPage />} /><Route path="/register" element={<RegisterPage />} /><Route path="/profile" element={<RequireAuth><ProfilePage /></RequireAuth>} /><Route path="/my-bookings" element={<RequireAuth roles={['CUSTOMER']}><MyBookingsPage /></RequireAuth>} /><Route path="/saved" element={<SavedPage />} /><Route path="/owner/dashboard" element={<RequireAuth roles={['OWNER']}><OwnerDashboardPage /></RequireAuth>} /><Route path="/owner/listings" element={<RequireAuth roles={['OWNER']}><OwnerListingsPage /></RequireAuth>} /><Route path="/owner/listings/new" element={<RequireAuth roles={['OWNER']}><OwnerListingsPage /></RequireAuth>} /><Route path="/owner/availability" element={<RequireAuth roles={['OWNER']}><OwnerAvailabilityPage /></RequireAuth>} /><Route path="/owner/bookings" element={<RequireAuth roles={['OWNER']}><OwnerBookingsPage /></RequireAuth>} /><Route path="/forbidden" element={<ForbiddenPage />} /><Route path="*" element={<NotFoundPage />} /></Routes>
}
function App() { return <AuthProvider><BrowserRouter><AppRoutes /></BrowserRouter></AuthProvider> }
export default App
