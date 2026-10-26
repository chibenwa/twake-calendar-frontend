import { userAttendee } from '@common/features/User/models/attendee'
import { ownerPartstatOf } from '@common/features/Search/searchResultsComponents'
import { SearchEventData } from '@common/features/Search/types/SearchEventData'
import { Calendar } from '@common/types/CalendarTypes'

const calendar = {
  id: 'alice/cal',
  name: 'Alice',
  owner: { emails: ['alice@example.com'], firstname: 'Alice' },
  events: {}
} as unknown as Calendar

const result = (attendees: userAttendee[]): SearchEventData => ({
  uid: 'event-1',
  userId: 'alice',
  calendarId: 'cal',
  start: '2026-10-26T10:00:00Z',
  attendees,
  organizer: { cn: 'Bob', email: 'bob@example.com' }
})

describe('ownerPartstatOf (#1784)', () => {
  it('reads the answer of the calendar owner off a search result', () => {
    expect(
      ownerPartstatOf(
        calendar,
        result([
          new userAttendee({
            cal_address: 'bob@example.com',
            partstat: 'ACCEPTED'
          }),
          new userAttendee({
            cal_address: 'alice@example.com',
            partstat: 'DECLINED'
          })
        ])
      )
    ).toBe('DECLINED')
  })

  it('has no answer when the owner is not invited', () => {
    expect(ownerPartstatOf(calendar, result([]))).toBeUndefined()
  })
})
