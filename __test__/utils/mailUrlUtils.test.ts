import { buildMailComposeUrl, buildMailtoUri } from '@common/utils/mailUrlUtils'

describe('buildMailtoUri', () => {
  it('joins valid addresses', () => {
    expect(
      buildMailtoUri(['alice@example.com', 'mailto:bob@example.com'])
    ).toBe('mailto:alice@example.com,bob@example.com')
  })

  it('leaves out an address smuggling mailto headers', () => {
    expect(
      buildMailtoUri([
        'hr@example.com?bcc=spy@evil.tld&body=hello',
        'alice@example.com'
      ])
    ).toBe('mailto:alice@example.com')
  })

  it.each([
    'a@example.com,spy@evil.tld',
    'a@example.com;spy@evil.tld',
    'a@example.com&bcc=spy@evil.tld',
    'a@example.com#x',
    'a b@example.com',
    'not-an-address'
  ])('rejects %s', address => {
    expect(buildMailtoUri([address])).toBeNull()
  })

  it('encodes each address', () => {
    expect(buildMailtoUri(["o'neil+tag@example.com"])).toBe(
      "mailto:o'neil%2Btag@example.com"
    )
  })
})

describe('buildMailComposeUrl', () => {
  it('builds the composer URL with the subject', () => {
    expect(
      buildMailComposeUrl('https://mail.example', ['a@example.com'], 'Q&A #1')
    ).toBe(
      'https://mail.example/mailto/?uri=mailto%3Aa%40example.com&subject=Q%26A%20%231'
    )
  })

  it('returns null when no address is valid', () => {
    expect(
      buildMailComposeUrl('https://mail.example', ['x@y.fr?bcc=z@evil.tld'])
    ).toBeNull()
  })
})
