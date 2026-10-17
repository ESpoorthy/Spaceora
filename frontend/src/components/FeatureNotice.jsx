export function FeatureNotice({ children = 'This feature is waiting for its backend API. Nothing has been saved.' }) {
  return <div className="feature-notice" role="status"><span aria-hidden="true">ⓘ</span><p>{children}</p></div>
}

export function DevPreviewNotice() {
  return <div className="dev-preview-notice"><span>Development preview</span> Workspace examples are sample listings and are not loaded from or saved to Spaceora.</div>
}
