// timezone.ts

// Ensure ICAL is imported before using this module.
import ICAL from 'ical.js'
import moment from 'moment-timezone'
import { detectDateTimeFormat } from '@common/components/Event/utils/dateTimeHelpers'

// TIMEZONES data must be imported or defined separately.
import { TIMEZONES } from './timezone-data'

export const browserDefaultTimeZone =
  Intl.DateTimeFormat().resolvedOptions().timeZone

// Core timezone registration functionality
export function registerTimezones() {
  for (const [key, data] of Object.entries(TIMEZONES.zones)) {
    ICAL.TimezoneService.register(key, buildTimezone(key, data.ics))
  }

  for (const [key, data] of Object.entries(TIMEZONES.aliases)) {
    ICAL.TimezoneService.register(key, findTimezone(data.aliasTo))
  }
}

function buildTimezone(tzid: string, ics: string): ICAL.Timezone {
  return (
    ICAL.TimezoneService.get(tzid) ||
    new ICAL.Timezone(new ICAL.Component(ICAL.parse(ics)))
  )
}

type TimezoneData = (typeof TIMEZONES.zones)[string]

/**
 * The data of a known zone. A plain index would also return the members
 * inherited from Object.prototype: a TZID named `toString` or `constructor`
 * (written by another client) must be as unknown as any other bogus name.
 */
export function getTimezoneData(
  tzName: string | undefined
): TimezoneData | undefined {
  return tzName !== undefined && Object.hasOwn(TIMEZONES.zones, tzName)
    ? TIMEZONES.zones[tzName]
    : undefined
}

function getTimezoneAlias(tzName: string): string | undefined {
  return Object.hasOwn(TIMEZONES.aliases, tzName)
    ? TIMEZONES.aliases[tzName].aliasTo
    : undefined
}

function findTimezone(tzid: string): ICAL.Timezone {
  const timezoneData = getTimezoneData(tzid)
  if (timezoneData) {
    return buildTimezone(tzid, timezoneData.ics)
  }

  const alias = getTimezoneAlias(tzid)
  if (alias) {
    return findTimezone(alias)
  }

  throw new Error(`Unknown timezone alias: ${tzid}`)
}

export function resolveTimezone(tzName: string): string {
  if (getTimezoneData(tzName)) {
    return tzName
  }
  return getTimezoneAlias(tzName) ?? tzName
}

/**
 * Whether moment-timezone knows the zone. moment.tz.zone() looks the name up
 * in plain objects: `constructor` reaches Object.prototype and makes it throw
 * a TypeError instead of answering null.
 */
export function isMomentZone(tzName: string | undefined): boolean {
  if (!tzName) return false
  try {
    return moment.tz.zone(tzName) !== null
  } catch {
    return false
  }
}

/**
 * Whether the browser knows the zone: Intl throws a RangeError on any other,
 * and a stored configuration may well hold one (written by another client, or
 * dropped from the browser's tz database).
 */
export function isValidTimeZone(tzName: unknown): tzName is string {
  if (typeof tzName !== 'string' || !tzName) return false
  try {
    new Intl.DateTimeFormat(undefined, { timeZone: tzName })
    return true
  } catch {
    return false
  }
}

export function resolveTimezoneId(tzid?: string): string | undefined {
  if (!tzid) return undefined
  return resolveTimezone(tzid)
}

export function convertEventDateTimeToISO(
  datetime: string | undefined,
  timezone: string,
  options?: { isAllDay?: boolean }
): string | undefined {
  if (!datetime || !timezone) return undefined
  if (options?.isAllDay) return undefined

  let normalized = datetime
  const match = datetime.match(
    /^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})?(Z|[+-]\d{2}:?\d{2})?$/
  )
  if (match) {
    const [, year, month, day, hour, min, sec, tz] = match
    let tzFormatted = tz ?? ''
    if (tz && tz !== 'Z' && !tz.includes(':')) {
      tzFormatted = tz.slice(0, 3) + ':' + tz.slice(3)
    }
    normalized = `${year}-${month}-${day}T${hour}:${min}${sec ? `:${sec}` : ''}${tzFormatted}`
  }

  if (normalized.includes('Z') || normalized.match(/[+-]\d{2}:\d{2}$/)) {
    return normalized
  }

  const dateOnlyRegex = /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/
  if (dateOnlyRegex.test(normalized)) {
    return undefined
  }

  const format = detectDateTimeFormat(normalized)
  const momentDate = moment.tz(normalized, format, timezone)
  if (!momentDate.isValid()) {
    console.warn(
      `[convertEventDateTimeToISO] Invalid datetime: "${datetime}" with format "${format}" in timezone "${timezone}"`
    )
    return undefined
  }
  return momentDate.toISOString()
}

export function getTimezoneOffset(
  tzName: string,
  date: Date = new Date()
): string {
  const fmt = new Intl.DateTimeFormat(undefined, {
    timeZone: tzName,
    timeZoneName: 'shortOffset'
  })

  const currentDate = moment(date).isValid() ? date : new Date()
  const parts = fmt.formatToParts(currentDate)
  const offsetPart = parts.find(p => p.type === 'timeZoneName')
  return offsetPart?.value.replace('GMT', 'UTC') ?? ''
}

/**
 * Returns a formatted timezone label with offset and readable timezone name.
 * Example: "(UTC+2) Europe Paris"
 */
export function formatTimezoneLabel(
  tzName: string = browserDefaultTimeZone
): string {
  return `(${getTimezoneOffset(tzName)}) ${tzName.replace(/_/g, ' ')}`
}
