import { createTheme } from '@linagora/twake-mui'
import { getAccessiblePair } from '@common/utils/getAccessiblePair'

describe('getAccessiblePair', () => {
  const theme = createTheme()

  it('computes a contrasted color for a hexadecimal color', () => {
    expect(getAccessiblePair('#123456', theme)).toMatch(/^(#|rgb)/)
  })

  it.each(['red', 'transparent', 'not a color'])(
    'falls back instead of throwing on %s',
    color => {
      expect(getAccessiblePair(color, theme)).toBe(
        theme.palette.getContrastText('#000')
      )
    }
  )
})
