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
}

export class userOrganiser {
  cn: string
  cal_address: string
  // Set when a delegate scheduled on the organizer's behalf (SENT-BY), which
  // the user must be told about: the sender is not the stated organizer.
  sentBy?: string

  constructor({ cn, cal_address, sentBy }: UserOrganiserOptions = {}) {
    this.cn = cn ?? ''
    this.cal_address = cal_address ?? ''
    this.sentBy = sentBy ? stripMailto(sentBy) : undefined
  }

  asMailto(): string {
    return `mailto:${stripMailto(this.cal_address)}`
  }

  asJcal(): VObjectProperty {
    const params: Record<string, string> = {}

    if (this.cn) {
      params.cn = this.cn
    }

    if (this.sentBy) {
      params['sent-by'] = `mailto:${this.sentBy}`
    }

    return ['organizer', params, 'cal-address', this.asMailto()]
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
