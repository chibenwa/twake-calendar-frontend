import { getTimezoneReferenceDate } from '@public/features/booking/utils/timezoneReferenceDate'
import { getTimezoneOffset } from '@common/utils/timezone'
import dayjs from 'dayjs'

describe('getTimezoneReferenceDate', () => {
  const firstOfOctober = new Date(2026, 9, 1)

  it('gives the offset of the selected day after the switch to winter time', () => {
    const date = getTimezoneReferenceDate(
      dayjs('2026-10-26'),
      firstOfOctober,
      'Europe/Paris'
    )
    expect(getTimezoneOffset('Europe/Paris', date)).toBe('UTC+1')
  })

  it('gives the offset of the selected day before the switch to winter time', () => {
    const date = getTimezoneReferenceDate(
      dayjs('2026-10-23'),
      firstOfOctober,
      'Europe/Paris'
    )
    expect(getTimezoneOffset('Europe/Paris', date)).toBe('UTC+2')
  })

  it('gives the offset of the selected day after the switch to summer time', () => {
    const date = getTimezoneReferenceDate(
      dayjs('2027-03-29'),
      new Date(2027, 2, 1),
      'Europe/Paris'
    )
    expect(getTimezoneOffset('Europe/Paris', date)).toBe('UTC+2')
  })

  it('reads the day in the selected zone, not in the browser one', () => {
    const date = getTimezoneReferenceDate(
      dayjs('2026-11-02'),
      firstOfOctober,
      'America/New_York'
    )
    expect(dayjs(date).tz('America/New_York').format('YYYY-MM-DD')).toBe(
      '2026-11-02'
    )
    expect(getTimezoneOffset('America/New_York', date)).toBe('UTC-5')
  })

  it('falls back on the given date without a selected day', () => {
    expect(getTimezoneReferenceDate(null, firstOfOctober, 'Europe/Paris')).toBe(
      firstOfOctober
    )
  })
})
