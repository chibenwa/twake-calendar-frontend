const ONE_DAY_MS = 24 * 60 * 60 * 1000

// The zone to read the days of a result in. An all-day result starts at UTC
// midnight of its day: read in the zone of the user, that is the day before
// west of UTC.
export const dayZoneOf = (allDay: boolean, timeZone: string): string =>
  allDay ? 'UTC' : timeZone

// The end (DTEND) of an all-day event is the day following its last day.
// An end before the start (accepted by the server) falls back to the start.
export const lastDayOf = (
  startDate: Date,
  endDate: Date,
  allDay: boolean
): Date =>
  new Date(
    Math.max(
      startDate.getTime(),
      allDay ? endDate.getTime() - ONE_DAY_MS : endDate.getTime()
    )
  )
