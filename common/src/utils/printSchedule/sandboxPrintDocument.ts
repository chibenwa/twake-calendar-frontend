const escapeAttribute = (value: string): string =>
  value.replace(/&/g, '&amp;').replace(/"/g, '&quot;')

const escapeText = (value: string): string =>
  value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

/**
 * Wraps the printable schedule into a sandboxed iframe.
 *
 * The schedule holds titles, locations and calendar names written by other
 * people. The print window is opened by the application and shares its
 * origin: rendered there directly, any escaping mistake would run a script
 * with the application's privileges. In an iframe sandboxed without
 * allow-same-origin, the schedule gets an opaque origin of its own and can
 * reach neither the application, nor its storage, nor its opener. Scripts
 * (the call to print) and modals (the print dialog) stay allowed.
 */
export function sandboxPrintDocument(html: string, title: string): string {
  return (
    '<!doctype html><html><head><meta charset="utf-8">' +
    `<title>${escapeText(title)}</title>` +
    '<style>html,body{margin:0;height:100%}' +
    'iframe{display:block;border:0;width:100%;height:100%}</style>' +
    '</head><body>' +
    `<iframe sandbox="allow-scripts allow-modals" title="${escapeAttribute(title)}" srcdoc="${escapeAttribute(html)}"></iframe>` +
    '</body></html>'
  )
}
