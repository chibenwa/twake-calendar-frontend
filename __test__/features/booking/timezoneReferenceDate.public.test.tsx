import dayjs from 'dayjs'
import { getTimezoneReferenceDate } from '@public/features/booking/utils/timezoneReferenceDate'
import { getTimezoneOffset } from '@common/utils/timezone'
import {
  formatDateInTimezone,
  formatDateTimeInTimezone
} from '@common/components/Event/utils/dateTimeFormatters'

describe('getTimezoneReferenceDate', () => {
  const october = new Date(2026, 9, 1)

  it('falls back to the visible month when no day is selected', () => {
    expect(getTimezoneReferenceDate(null, october, 'Europe/Paris')).toBe(
      october
    )
  })

  it('uses noon of the selected day in the selected zone', () => {
    const reference = getTimezoneReferenceDate(
      dayjs('2026-10-26'),
      october,
      'Europe/Paris'
    )

    expect(reference.toISOString()).toBe('2026-10-26T11:00:00.000Z')
  })

  it('labels the offset of a day after the autumn DST change', () => {
    const reference = getTimezoneReferenceDate(
      dayjs('2026-10-26'),
      october,
      'Europe/Paris'
    )

    expect(getTimezoneOffset('Europe/Paris', reference)).toBe('UTC+1')
  })

  it('labels the offset of a day before the autumn DST change', () => {
    const reference = getTimezoneReferenceDate(
      dayjs('2026-10-23'),
      october,
      'Europe/Paris'
    )

    expect(getTimezoneOffset('Europe/Paris', reference)).toBe('UTC+2')
  })

  it('labels the offset of a day after the spring DST change', () => {
    const reference = getTimezoneReferenceDate(
      dayjs('2026-03-30'),
      new Date(2026, 2, 1),
      'Europe/Paris'
    )

    expect(getTimezoneOffset('Europe/Paris', reference)).toBe('UTC+2')
  })
})

describe('owner and booker in time zones with different DST changes', () => {
  const PARIS = 'Europe/Paris'
  const NEW_YORK = 'America/New_York'
  const OWNER_SLOT_TIME = '10:00'

  // New York is on summer time from 8 March to 1 November 2026,
  // Paris from 29 March to 25 October 2026
  const days = [
    // day, period, Paris offset, New York offset,
    // Paris slot seen from New York, New York slot seen from Paris
    ['2026-01-14', 'both on winter time', 'UTC+1', 'UTC-5', '04:00', '16:00'],
    ['2026-03-08', 'New York changes', 'UTC+1', 'UTC-4', '05:00', '15:00'],
    [
      '2026-03-16',
      'only New York on summer time',
      'UTC+1',
      'UTC-4',
      '05:00',
      '15:00'
    ],
    ['2026-03-29', 'Paris changes', 'UTC+2', 'UTC-4', '04:00', '16:00'],
    ['2026-07-15', 'both on summer time', 'UTC+2', 'UTC-4', '04:00', '16:00'],
    ['2026-10-25', 'Paris changes', 'UTC+1', 'UTC-4', '05:00', '15:00'],
    [
      '2026-10-28',
      'only New York on summer time',
      'UTC+1',
      'UTC-4',
      '05:00',
      '15:00'
    ],
    ['2026-11-01', 'New York changes', 'UTC+1', 'UTC-5', '04:00', '16:00'],
    ['2026-11-04', 'both on winter time', 'UTC+1', 'UTC-5', '04:00', '16:00']
  ] as const

  const cases = days.flatMap(
    ([
      day,
      period,
      parisOffset,
      newYorkOffset,
      parisSlotInNewYork,
      newYorkSlotInParis
    ]) => [
      {
        day,
        period,
        owner: PARIS,
        booker: PARIS,
        time: OWNER_SLOT_TIME,
        offset: parisOffset
      },
      {
        day,
        period,
        owner: PARIS,
        booker: NEW_YORK,
        time: parisSlotInNewYork,
        offset: newYorkOffset
      },
      {
        day,
        period,
        owner: NEW_YORK,
        booker: PARIS,
        time: newYorkSlotInParis,
        offset: parisOffset
      },
      {
        day,
        period,
        owner: NEW_YORK,
        booker: NEW_YORK,
        time: OWNER_SLOT_TIME,
        offset: newYorkOffset
      }
    ]
  )

  it.each(cases)(
    '$day ($period): $owner slot at $time $offset for a $booker booker',
    ({ day, owner, booker, time, offset }) => {
      const slotStart = dayjs.tz(`${day}T${OWNER_SLOT_TIME}:00`, owner).toDate()
      const bookerDay = formatDateInTimezone(slotStart, booker)
      const reference = getTimezoneReferenceDate(
        dayjs(bookerDay),
        new Date(slotStart.getFullYear(), slotStart.getMonth(), 1),
        booker
      )

      expect(formatDateTimeInTimezone(slotStart.toISOString(), booker)).toBe(
        `${day}T${time}`
      )
      expect(getTimezoneOffset(booker, reference)).toBe(offset)
    }
  )
})
