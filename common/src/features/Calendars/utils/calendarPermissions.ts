import { Calendar } from '@common/types/CalendarTypes'
import { normalizeEmail } from '@common/utils/normalizeEmail'

export function canWriteToCalendar(cal: Calendar, userId: string): boolean {
  if (!cal) return false

  const isOwner = cal.id?.split('/')[0] === userId
  const hasDelegatedWrite = !!(cal.delegated && cal.access?.write)

  return isOwner || hasDelegatedWrite
}

/**
 * Whether the user manages who may use a calendar -- its rights and its public
 * visibility: its owner, or anybody it was lent to with the administration
 * right, be it a user's calendar, a team calendar or a resource.
 */
export function canAdministerCalendar(
  cal: Calendar,
  user: { openpaasId?: string; email?: string }
): boolean {
  if (!cal) return false
  if (user.openpaasId && cal.id?.split('/')[0] === user.openpaasId) return true

  const email = normalizeEmail(user.email)
  if (!email) return false
  return !!cal.invite?.some(
    invite =>
      invite.access === 5 &&
      normalizeEmail(invite.href.replace(/^mailto:/i, '')) === email
  )
}
