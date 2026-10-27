import { VideoConferenceField } from '@common/components/Event/fields/VideoConferenceField'
import { createVideoConferenceRoom } from '@common/features/Events/VideoConferenceDao'
import '@testing-library/jest-dom'
import { fireEvent, screen } from '@testing-library/react'
import { renderWithProviders } from '../../../utils/Renderwithproviders'

jest.mock('@common/features/Events/VideoConferenceDao')

describe('VideoConferenceField room creation failure (#1821)', () => {
  const originalIntegration = window.MEET_BACKEND_INTEGRATION
  const setHasVideoConference = jest.fn()
  const setMeetingLink = jest.fn()

  const renderField = (showMore: boolean): void => {
    renderWithProviders(
      <VideoConferenceField
        hasVideoConference={false}
        setHasVideoConference={setHasVideoConference}
        meetingLink={null}
        setMeetingLink={setMeetingLink}
        description=""
        setDescription={jest.fn()}
        showMore={showMore}
      />
    )
  }

  beforeEach(() => {
    jest.clearAllMocks()
    window.MEET_BACKEND_INTEGRATION = true
    jest.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    window.MEET_BACKEND_INTEGRATION = originalIntegration
    jest.restoreAllMocks()
  })

  it.each([
    ['expanded', true],
    ['simple', false]
  ])(
    'tells the user the room could not be created in %s mode',
    async (_mode, showMore) => {
      ;(createVideoConferenceRoom as jest.Mock).mockRejectedValue(
        new Error('createVideoConferenceRoom failed with status 500')
      )
      renderField(showMore)

      fireEvent.click(screen.getByText('event.form.addVisioConference'))

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'event.form.videoConferenceCreationFailed'
      )
      expect(setHasVideoConference).not.toHaveBeenCalled()
      expect(
        screen.getByText('event.form.addVisioConference')
      ).toBeInTheDocument()
    }
  )

  it('shows no error when the room is created', async () => {
    ;(createVideoConferenceRoom as jest.Mock).mockResolvedValue(
      'https://meet.example.com/vep-txbc-trh'
    )
    renderField(true)

    fireEvent.click(screen.getByText('event.form.addVisioConference'))

    await screen.findByText('event.form.addVisioConference')
    expect(setMeetingLink).toHaveBeenCalledWith(
      'https://meet.example.com/vep-txbc-trh'
    )
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
