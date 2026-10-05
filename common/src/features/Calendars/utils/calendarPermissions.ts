import {
  AccessRight,
  Calendar,
  WRITE_ACCESS_LEVELS
} from '@common/types/CalendarTypes'
import { normalizeEmail } from '@common/utils/normalizeEmail'

const ADMIN_ACCESS: AccessRight = 5

export function canWriteToCalendar(cal: Calendar, userId: string): boolean {
  if (!cal) return false

  const isOwner = cal.id?.split('/')[0] === userId
  const hasDelegatedWrite = !!(cal.delegated && cal.access?.write)

  return isOwner || hasDelegatedWrite
}

function isInvitedWith(
  cal: Calendar,
  userEmail: string | undefined,
  accesses: number[]
): boolean {
  const email = normalizeEmail(userEmail)
  if (!email) return false
  return !!cal.invite?.some(
    invite =>
      accesses.includes(invite.access) &&
      normalizeEmail(invite.href.replace(/^mailto:/i, '')) === email
  )
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

  return isInvitedWith(cal, user.email, [ADMIN_ACCESS])
}

/**
 * Whether a member of a team calendar may change its events: every member
 * sees them, but only those granted the read-write or the administration
 * right may write them back.
 */
export function canWriteToTeamCalendar(
  cal: Calendar,
  userEmail: string | undefined
): boolean {
  if (!cal) return false
  return isInvitedWith(cal, userEmail, WRITE_ACCESS_LEVELS)
}
