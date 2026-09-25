// The color of a calendar is chosen by its owner, possibly another user, and
// ends up in style rules: a value such as "#fff;}*{background:url(...)}"
// would inject CSS in the whole application.
//
// Hexadecimal CSS colors only (#rgb, #rgba, #rrggbb, #rrggbbaa): anything
// else, named colors included, is refused.
const SAFE_HEX_COLOR = /^#(?:[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$/i

export function isSafeColor(value: unknown): value is string {
  return typeof value === 'string' && SAFE_HEX_COLOR.test(value)
}
