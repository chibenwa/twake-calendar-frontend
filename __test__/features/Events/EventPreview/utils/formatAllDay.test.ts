import { formatDate } from '@common/components/EventPreview/utils/formatDate'
import { formatEnd } from '@common/components/EventPreview/utils/formatEnd'

describe('EventPreview all-day date formatting', () => {
  const t = (key: string): string => (key === 'locale' ? 'en-GB' : key)

  describe.each(['America/New_York', 'Pacific/Honolulu', 'Asia/Tokyo'])(
    'with display time zone %s',
    timeZone => {
      it('formats the start on the stored day', () => {
        expect(formatDate('2026-10-20', t, timeZone, true)).toBe(
          'Tuesday, 20 October 2026'
        )
      })

      it('returns no end for a single-day event', () => {
        expect(
          formatEnd({
            start: '2026-10-20',
            end: '2026-10-21',
            t,
            timeZone,
            allday: true
          })
        ).toBeNull()
      })

      it('formats the inclusive end of a multi-day event', () => {
        expect(
          formatEnd({
            start: '2026-10-19',
            end: '2026-10-22',
            t,
            timeZone,
            allday: true
          })
        ).toBe('21 Oct 2026')
      })
    }
  )
})
