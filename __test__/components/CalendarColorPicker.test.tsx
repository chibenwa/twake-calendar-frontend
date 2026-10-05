import { ColorPicker } from '@common/components/Calendar/CalendarColorPicker'
import { defaultColors } from '@common/utils/defaultColors'
import { fireEvent, screen } from '@testing-library/react'
import { renderWithProviders } from '../utils/Renderwithproviders'

describe('ColorPicker', () => {
  const loadedColor = { light: '#F5CFD0', dark: '#8E1E22' }

  function renderLoadedAfterMount(onChange: jest.Mock): void {
    // Mimics CalendarModal: first rendered with the first preset, then
    // with the calendar colour once loaded
    const { rerender } = renderWithProviders(
      <ColorPicker selectedColor={defaultColors[0]} onChange={onChange} />
    )
    rerender(<ColorPicker selectedColor={loadedColor} onChange={onChange} />)
  }

  it('cancelling the custom picker restores the current colour', () => {
    const onChange = jest.fn()
    renderLoadedAfterMount(onChange)

    fireEvent.click(screen.getByLabelText('colorPicker.selectCustom'))
    fireEvent.click(screen.getByRole('button', { name: 'common.cancel' }))

    expect(onChange).toHaveBeenLastCalledWith(loadedColor)
  })

  it('the custom picker starts from the current colour', () => {
    renderLoadedAfterMount(jest.fn())

    fireEvent.click(screen.getByLabelText('colorPicker.selectCustom'))

    expect(screen.getByDisplayValue(loadedColor.light)).toBeInTheDocument()
  })
})
