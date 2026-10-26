// The booking side-service only accepts six-digit colours (#RRGGBB), while
// calendar colours may be short, typed in the picker or set by another
// CalDAV client: #ABC stands for #AABBCC.
const SHORT_HEX_COLOR = /^#([0-9a-f])([0-9a-f])([0-9a-f])$/i

export function expandHexColor(color: string): string {
  return color.replace(
    SHORT_HEX_COLOR,
    (_, r: string, g: string, b: string) => `#${r}${r}${g}${g}${b}${b}`
  )
}
