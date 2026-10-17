const KEY = 'spaceora.saved-space-ids'
export function getSavedSpaceIds() {
  try { return JSON.parse(localStorage.getItem(KEY) || '[]').filter((id) => typeof id === 'string') }
  catch { return [] }
}
export function toggleSavedSpace(id) {
  const ids = getSavedSpaceIds()
  const next = ids.includes(id) ? ids.filter((savedId) => savedId !== id) : [...ids, id]
  localStorage.setItem(KEY, JSON.stringify(next))
  window.dispatchEvent(new CustomEvent('spaceora:saved-spaces-updated'))
  return next
}
