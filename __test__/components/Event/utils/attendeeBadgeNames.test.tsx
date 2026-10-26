import { renderAttendeeBadge } from '@common/components/Event/utils/eventUtils'
import { PartStat, userAttendee } from '@common/features/User/models/attendee'
import '@testing-library/jest-dom'
import { fireEvent, screen } from '@testing-library/react'
import { renderWithProviders } from '../../../utils/Renderwithproviders'

const t = (key: string): string => key

const bob = (partstat: PartStat): userAttendee =>
  new userAttendee({ cal_address: 'bob@test.com', partstat })

describe('participant answers have a text alternative (#1788)', () => {
  it('names a compact avatar after the participant', () => {
    renderWithProviders(
      renderAttendeeBadge({ a: bob('NEEDS-ACTION'), key: 'bob', t })
    )

    const trigger = screen.getByRole('button', { name: 'bob@test.com' })
    expect(screen.queryByText('bob@test.com')).not.toBeInTheDocument()

    fireEvent.click(trigger)

    expect(screen.getByText('bob@test.com')).toBeInTheDocument()
  })

  it('adds the answer to the name of a compact avatar', () => {
    renderWithProviders(
      renderAttendeeBadge({ a: bob('ACCEPTED'), key: 'bob', t })
    )

    expect(
      screen.getByRole('button', {
        name: 'bob@test.com, eventPreview.attendingStatus.ACCEPTED'
      })
    ).toBeInTheDocument()
  })

  it.each(['ACCEPTED', 'DECLINED'] as PartStat[])(
    'says the %s badge of a full participant row',
    partstat => {
      renderWithProviders(
        renderAttendeeBadge({ a: bob(partstat), key: 'bob', t, isFull: true })
      )

      // Inside the row's button, the icon is part of the button's name
      expect(
        screen.getByRole('button', {
          name: new RegExp(`eventPreview\\.attendingStatus\\.${partstat}`)
        })
      ).toBeInTheDocument()
    }
  )
})
