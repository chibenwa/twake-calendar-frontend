import { EventFormValues } from '@common/components/Event/EventFormFields.types'
import { formatLocalDateTime } from '@common/components/Event/utils/dateTimeFormatters'
import { CalendarEvent } from '@common/types/EventsTypes'
import { fetchEvent } from './EventDao'
import { parseFetchedEvent } from './transformers/parseFetchedEvent'

function isStandaloneTimedEvent(event: CalendarEvent): boolean {
  return !event.uid.includes('/') && !event.allday
}

// The event held by the grid comes from the expanded calendar REPORT, where
// its zone is the viewer's one: writing it back rewrites DTSTART/DTEND in that
// zone, which Sabre forbids to an attendee (#1562). The stored calendar object
// tells the zone the organizer wrote the event in.
export async function keepStoredSchedule(
  formValues: EventFormValues,
  event: CalendarEvent
): Promise<EventFormValues> {
  if (!isStandaloneTimedEvent(event)) return formValues

  try {
    const stored = parseFetchedEvent(event, await fetchEvent(event))
    if (!stored.end) return formValues
    return {
      ...formValues,
      start: formatLocalDateTime(new Date(stored.start), stored.timezone),
      end: formatLocalDateTime(new Date(stored.end), stored.timezone),
      timezone: stored.timezone
    }
  } catch (error) {
    console.error('Failed to fetch the stored event:', error)
    return formValues
  }
}
