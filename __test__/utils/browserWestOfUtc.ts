/**
 * Jest runs in UTC (see setupTests). Make Date#getDay answer as a browser
 * five hours west of UTC (New York in winter) would: midnight UTC is still
 * the day before there.
 */
export function mockBrowserWestOfUtc(): jest.SpyInstance {
  return jest.spyOn(Date.prototype, 'getDay').mockImplementation(function (
    this: Date
  ) {
    return new Date(this.getTime() - 5 * 60 * 60 * 1000).getUTCDay()
  })
}
