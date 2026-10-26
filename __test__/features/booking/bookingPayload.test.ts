import {
  buildBookingPayload,
  buildUpdateBookingPayload
} from '../../../apps/private/src/features/booking/utils'

describe('booking payload colour', () => {
  const formData = {
    name: 'Meet me',
    duration: 30,
    calendarid: 'user1/cal1',
    active: true,
    availabilityRules: [],
    timezone: 'Europe/Paris',
    description: '',
    color: '#ABC',
    location: '',
    eventClass: 'PUBLIC',
    busy: 'OPAQUE',
    resourceIds: [],
    alarmList: [],
    extraAttendeesList: []
  }

  it('sends a three-digit colour as six digits on creation', () => {
    expect(buildBookingPayload(formData).color).toBe('#AABBCC')
  })

  it('sends a three-digit colour as six digits on update', () => {
    expect(buildUpdateBookingPayload(formData).color).toBe('#AABBCC')
  })

  it('sends a six-digit colour unchanged', () => {
    expect(buildBookingPayload({ ...formData, color: '#6B4ECC' }).color).toBe(
      '#6B4ECC'
    )
  })
})
