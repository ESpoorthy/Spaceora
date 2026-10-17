import { Header } from './SiteLayout'

export function AuthShell({ children }) {
  return <><Header/><main className="auth-page container-fluid"><div className="auth-layout"><section className="auth-photo-panel"><img src="/images/auth-workspace.jpeg" alt="Bright coworking studio with natural light and shared work tables"/><div className="auth-photo-copy"><h2>Great days start in the right space.</h2><p>Find focus. Bring people together. Make room for what’s next.</p><small>SPACEORA · BY TEAM ORBITX</small></div></section><section className="auth-form-column">{children}</section></div><footer className="auth-footer">© 2026 Spaceora · Privacy · Terms · Help</footer></main></>
}
