/**
 * Utility functions for the mail application (mail composer) URL.
 */

import { EmailAddress } from '@common/types/EmailAddress'
import {
  resolveUriTemplate,
  UriTemplateContext
} from '@common/utils/uriTemplateUtils'

/**
 * Resolve the MAIL_SPA_URL configuration entry.
 *
 * MAIL_SPA_URL is a URI template (RFC 6570 style) so it can support platform
 * mode, the same way VIDEO_CONFERENCE_BASE_URL does. See
 * {@link resolveUriTemplate} for the list of supported expressions.
 *
 * @returns the resolved base URL, or null when MAIL_SPA_URL is not configured.
 */
export function resolveMailSpaUrl(
  context: UriTemplateContext = {}
): string | null {
  const template = window.MAIL_SPA_URL
  if (!template) return null

  return resolveUriTemplate(template, context)
}

// Characters that would end the address list or start the headers of a
// mailto URI (RFC 6068): an attendee address carrying them could add a
// hidden Bcc or a body to the message composed by the user.
const MAILTO_SEPARATORS = /[?&,;#\s]/

/**
 * Builds a mailto URI out of attendee addresses, which come from the
 * invitation and are chosen by its sender. Addresses that are not plain
 * email addresses are left out, and each one is encoded on its own.
 *
 * @returns the mailto URI, or null when no address is valid.
 */
export function buildMailtoUri(addresses: string[]): string | null {
  const valid = addresses
    .map(address => address.replace(/^mailto:/i, '').trim())
    .filter(address => !MAILTO_SEPARATORS.test(address))
    .map(address => EmailAddress.parse(address))
    .filter((address): address is EmailAddress => address !== null)
    .map(address => encodeURIComponent(address.value).replace(/%40/g, '@'))

  return valid.length > 0 ? `mailto:${valid.join(',')}` : null
}

/**
 * URL of the mail composer addressed to the given attendees.
 *
 * @returns the URL, or null when no address is valid.
 */
export function buildMailComposeUrl(
  mailSpaUrl: string,
  addresses: string[],
  subject?: string
): string | null {
  const mailto = buildMailtoUri(addresses)
  if (!mailto) return null

  const subjectParam =
    subject !== undefined ? `&subject=${encodeURIComponent(subject)}` : ''
  return `${mailSpaUrl}/mailto/?uri=${encodeURIComponent(mailto)}${subjectParam}`
}
