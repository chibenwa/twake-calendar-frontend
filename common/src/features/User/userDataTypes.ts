import { VObjectProperty } from '@common/features/Calendars/types/CalendarData'
import { stripMailto } from '@common/utils/normalizeIdentity'

export interface userData {
  email: string
  family_name: string
  given_name: string
  name: string
  sid: string
  sub: string
  openpaasId?: string
  language?: string
  timezone?: string | null
  workplaceFqdn?: string
}

export interface UserConfigurations {
  modules?: ModuleConfiguration[]
}

export interface NotificationSettings {
  email?: boolean
  push?: boolean
}

export type NotificationSettingsExtended = NotificationSettings & {
  [key: string]: unknown
}

export interface UserOrganiserOptions {
  cn?: string
  cal_address?: string
  sentBy?: string
  otherParams?: Record<string, string>
  paramOrder?: string[]
}

export class userOrganiser {
  cn: string
  cal_address: string
  // Set when a delegate scheduled on the organizer's behalf (SENT-BY), which
  // the user must be told about: the sender is not the stated organizer.
  sentBy?: string
  // Parameters the server stamps on the ORGANIZER (e.g. SCHEDULE-STATUS once
  // an attendee replied). Written back untouched: the scheduling plugin sees
  // any difference as an attendee changing the ORGANIZER and answers 403.
  otherParams?: Record<string, string>
  // Order the parameters were read in. The plugin compares the serialized
  // ORGANIZER, so a mere reordering of its parameters counts as a change.
  paramOrder?: string[]

  constructor({
    cn,
    cal_address,
    sentBy,
    otherParams,
    paramOrder
  }: UserOrganiserOptions = {}) {
    this.cn = cn ?? ''
    this.cal_address = cal_address ?? ''
    this.sentBy = sentBy ? stripMailto(sentBy) : undefined
    this.otherParams =
      otherParams && Object.keys(otherParams).length > 0
        ? otherParams
        : undefined
    this.paramOrder = paramOrder?.length ? paramOrder : undefined
  }

  asMailto(): string {
    return `mailto:${stripMailto(this.cal_address)}`
  }

  asJcal(): VObjectProperty {
    const written: Record<string, string> = {}

    if (this.cn) {
      written.cn = this.cn
    }

    if (this.sentBy) {
      written['sent-by'] = `mailto:${this.sentBy}`
    }

    Object.assign(written, this.otherParams)

    const params: Record<string, string> = {}
    for (const name of this.paramOrder ?? []) {
      if (name in written) {
        params[name] = written[name]
      }
    }

    return [
      'organizer',
      Object.assign(params, written),
      'cal-address',
      this.asMailto()
    ]
  }
}
// Type for configuration item
export interface ConfigurationItem {
  name: string
  value: unknown
}

// Type for module configuration
export interface ModuleConfiguration {
  name: string
  configurations: ConfigurationItem[]
}
