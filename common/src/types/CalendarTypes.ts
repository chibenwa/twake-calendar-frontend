import { CalendarEvent } from '@common/types/EventsTypes'
import { OpenPaasUserData } from '@common/features/User/type/OpenPaasUserData'

export interface Calendar {
  id: string
  link: string
  name: string
  delegated?: boolean
  prodid?: string
  color?: Record<string, string>
  owner: OpenPaasUserData
  description?: string
  calscale?: string
  version?: string
  events: Record<string, CalendarEvent>
  visibility: 'private' | 'public'
  access?: DelegationAccess
  lastCacheCleared?: number
  lastRangesOutdated?: number
  syncToken?: string
  invite?: CalendarInvite[]
}

export interface DelegationAccess {
  freebusy: boolean
  read: boolean
  write: boolean
  'write-properties': boolean
  all: boolean
}

export type CalendarInvite = {
  href: string
  principal: string
  access: AccessRight
  inviteStatus: number
}

export type AccessRight = 2 | 3 | 5 // VIEW = 2, EDITOR = 3, ADMIN = 5

export const WRITE_ACCESS_LEVELS = [3, 5]
