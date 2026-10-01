import { withHighContrast } from '@common/theme/highContrastTheme'

describe('withHighContrast', () => {
  const base = { palette: { primary: { main: '#F67E35' } } }

  it('leaves the theme untouched while the mode is off', () => {
    expect(withHighContrast(base, false, 'fr')).toBe(base)
  })

  it('translates the MUI texts into the user language', () => {
    const options = withHighContrast(base, true, 'fr-fr') as {
      components: { MuiAlert: { defaultProps: { closeText: string } } }
    }

    expect(options.components.MuiAlert.defaultProps.closeText).toBe('Fermer')
  })

  it('keeps the application theme underneath', () => {
    const options = withHighContrast(base, true, 'en')

    expect(options?.palette).toBeDefined()
  })
})
