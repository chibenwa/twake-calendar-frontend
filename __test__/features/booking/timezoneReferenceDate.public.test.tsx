import dayjs from 'dayjs'
import { getTimezoneReferenceDate } from '@public/features/booking/utils/timezoneReferenceDate'
import { getTimezoneOffset } from '@common/utils/timezone'

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
