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

  it('names each preset swatch through the translations', () => {
    renderWithProviders(
      <ColorPicker selectedColor={defaultColors[0]} onChange={jest.fn()} />
    )

    expect(
      screen.getByRole('button', {
        name: `colorPicker.selectPreset(color=${defaultColors[0].light})`
      })
    ).toBeInTheDocument()
  })

  it('flags an invalid hex value and disables Save until it is fixed', () => {
    const onChange = jest.fn()
    renderLoadedAfterMount(onChange)

    fireEvent.click(screen.getByLabelText('colorPicker.selectCustom'))
    const hex = screen.getByRole('textbox', { name: 'colorPicker.hex' })
    const save = screen.getByRole('button', { name: 'actions.save' })

    fireEvent.change(hex, { target: { value: 'zzz' } })
    fireEvent.blur(hex)

    expect(hex).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('colorPicker.invalidHex')).toBeInTheDocument()
    expect(save).toBeDisabled()
    expect(onChange).not.toHaveBeenCalledWith(
      expect.objectContaining({ light: 'ZZZ' })
    )

    fireEvent.change(hex, { target: { value: '#12345' } })
    expect(save).toBeDisabled()

    fireEvent.change(hex, { target: { value: '#123456' } })
    fireEvent.blur(hex)

    expect(hex).toHaveAttribute('aria-invalid', 'false')
    expect(screen.queryByText('colorPicker.invalidHex')).not.toBeInTheDocument()
    expect(save).toBeEnabled()
    fireEvent.click(save)
    expect(onChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ light: '#123456' })
    )
  })

  it('opens again on a valid colour after an invalid value was cancelled', () => {
    renderLoadedAfterMount(jest.fn())

    fireEvent.click(screen.getByLabelText('colorPicker.selectCustom'))
    fireEvent.change(screen.getByRole('textbox', { name: 'colorPicker.hex' }), {
      target: { value: 'zzz' }
    })
    fireEvent.click(screen.getByRole('button', { name: 'common.cancel' }))
    fireEvent.click(screen.getByLabelText('colorPicker.selectCustom'))

    expect(screen.getByDisplayValue(loadedColor.light)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'actions.save' })).toBeEnabled()
  })
})
