// The date shown by the calendar grid survives a page reload (F5) so the user
// lands back on the period they were looking at. Session scoped: a new tab
// still opens on today.
const DISPLAYED_DATE_KEY = 'displayedDate'

export function getDisplayedDate(): Date {
  try {
    const stored = sessionStorage.getItem(DISPLAYED_DATE_KEY)
    const date = stored ? new Date(stored) : new Date()
    return isNaN(date.getTime()) ? new Date() : date
  } catch {
    return new Date()
  }
}

export function setDisplayedDate(date: Date): void {
  try {
    sessionStorage.setItem(DISPLAYED_DATE_KEY, date.toISOString())
  } catch {
    // Ignore sessionStorage errors
  }
}
