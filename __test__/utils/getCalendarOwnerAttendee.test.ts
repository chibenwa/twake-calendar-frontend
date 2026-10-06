import { userAttendee } from '@common/features/User/models/attendee'
import { Calendar } from '@common/types/CalendarTypes'
import { getCalendarOwnerAttendee } from '@common/utils/getCalendarOwnerAttendee'

const attendee = (
  cal_address: string,
  partstat: userAttendee['partstat']
): userAttendee => new userAttendee({ cal_address, partstat })

const personalCalendar = {
  id: 'alice/cal',
  name: 'Alice',
  owner: { emails: ['alice@example.com'], firstname: 'Alice' },
  events: {}
} as unknown as Calendar

const teamCalendar = {
  id: 'team/cal',
  name: 'Team',
  owner: {
    emails: ['ghassen@example.com', 'benoit@example.com'],
    firstname: 'Team',
    teamCalendar: true
  },
  events: {}
} as unknown as Calendar

describe('getCalendarOwnerAttendee', () => {
  it('picks the attendee owning a personal calendar', () => {
    const attendees = [
      attendee('bob@example.com', 'ACCEPTED'),
      attendee('Alice@Example.com', 'TENTATIVE')
    ]

    expect(
      getCalendarOwnerAttendee(personalCalendar, attendees, {
        cal_address: 'bob@example.com'
      })?.partstat
    ).toBe('TENTATIVE')
  })

  it('picks the organizer of a team event, not the first member invited', () => {
    const attendees = [
      attendee('ghassen@example.com', 'NEEDS-ACTION'),
      attendee('benoit@example.com', 'ACCEPTED')
    ]

    expect(
      getCalendarOwnerAttendee(teamCalendar, attendees, {
        cal_address: 'mailto:Benoit@example.com'
      })?.partstat
    ).toBe('ACCEPTED')
  })

  it('picks the organizer of a team event inviting external participants', () => {
    const attendees = [
      attendee('outsider@external.test', 'NEEDS-ACTION'),
      attendee('ghassen@example.com', 'NEEDS-ACTION'),
      attendee('benoit@example.com', 'ACCEPTED')
    ]

    expect(
      getCalendarOwnerAttendee(teamCalendar, attendees, {
        cal_address: 'mailto:benoit@example.com'
      })?.partstat
    ).toBe('ACCEPTED')
  })

  it('falls back to the members on a team event organized from outside', () => {
    const attendees = [
      attendee('outsider@external.test', 'ACCEPTED'),
      attendee('ghassen@example.com', 'DECLINED')
    ]

    expect(
      getCalendarOwnerAttendee(teamCalendar, attendees, {
        cal_address: 'outsider@external.test'
      })?.partstat
    ).toBe('DECLINED')
  })

  it('finds nobody on a team event whose organizer does not attend', () => {
    const attendees = [attendee('ghassen@example.com', 'NEEDS-ACTION')]

    expect(
      getCalendarOwnerAttendee(teamCalendar, attendees, {
        cal_address: 'benoit@example.com'
      })
    ).toBeUndefined()
  })

  it('falls back to the members on a team event without organizer', () => {
    const attendees = [attendee('ghassen@example.com', 'DECLINED')]

    const owner = getCalendarOwnerAttendee(teamCalendar, attendees)

    expect(owner?.partstat).toBe('DECLINED')
  })

  it('finds nobody without calendar', () => {
    const attendees = [attendee('alice@example.com', 'ACCEPTED')]

    expect(getCalendarOwnerAttendee(undefined, attendees)).toBeUndefined()
  })
})
