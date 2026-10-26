import { expandHexColor } from '../expandHexColor'

describe('expandHexColor', () => {
  it('expands a three-digit colour to six digits', () => {
    expect(expandHexColor('#ABC')).toBe('#AABBCC')
    expect(expandHexColor('#1f3')).toBe('#11ff33')
  })

  it('keeps a six-digit colour as is', () => {
    expect(expandHexColor('#6B4ECC')).toBe('#6B4ECC')
  })

  it('leaves values that are not short hex colours untouched', () => {
    expect(expandHexColor('#ABCD')).toBe('#ABCD')
    expect(expandHexColor('ABC')).toBe('ABC')
  })
})
