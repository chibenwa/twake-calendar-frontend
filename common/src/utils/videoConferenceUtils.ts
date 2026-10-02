/**
 * Utility functions for video conference meeting generation
 */

import {
  resolveUriTemplate,
  type UriTemplateContext
} from '@linagora/twake-utils'

/**
 * Generate a random meeting ID in format xxx-xxxx-xxx
 * @returns {string} Random meeting ID
 */
export function generateMeetingId(): string {
  const chars = 'abcdefghijklmnopqrstuvwxyz'
  const generateSegment = (length: number): string => {
    return Array.from(
      { length },
      () => chars[Math.floor(Math.random() * chars.length)]
    ).join('')
  }

  return `${generateSegment(3)}-${generateSegment(4)}-${generateSegment(3)}`
}

/**
 * Context used to resolve the template expressions of VIDEO_CONFERENCE_BASE_URL.
 */
export type VisioTemplateContext = UriTemplateContext

/**
 * Resolve a URI-template (RFC 6570 style) VIDEO_CONFERENCE_BASE_URL.
 *
 * See {@link resolveUriTemplate} for the list of supported expressions.
 */
export function resolveVisioTemplate(
  template: string,
  context: VisioTemplateContext
): string {
  return resolveUriTemplate(template, context)
}

/**
 * Generate a complete meeting link from the VIDEO_CONFERENCE_BASE_URL template.
 */
export function generateMeetingLink(
  context: VisioTemplateContext = {},
  baseUrl?: string
): string {
  const template = baseUrl || window.VIDEO_CONFERENCE_BASE_URL
  if (!template) return ''

  const base = resolveUriTemplate(template, context)
  const meetingId = generateMeetingId()
  return `${base}/${meetingId}`
}
