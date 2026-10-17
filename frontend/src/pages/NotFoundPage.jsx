import { Link } from 'react-router-dom'
import { SiteLayout } from '../components/SiteLayout'

function NotFoundPage() { return <SiteLayout><main className="empty-state"><span>404</span><h1>That space doesn’t exist.</h1><p>The page may have moved. Let’s find a better place to land.</p><Link className="btn btn-primary" to="/explore">Explore spaces</Link></main></SiteLayout> }
export default NotFoundPage
