import { api } from '@common/utils/apiUtils'
import { fetchEntityById } from './EntityDAO'
import {
  OpenPaasUserData,
  normalizeOpenPaasUser
} from './type/OpenPaasUserData'
import { ModuleConfiguration } from './userDataTypes'
import { SearchResponseItem } from '@common/types/SearchResponseItem'

export async function fetchCurrentUser(): Promise<OpenPaasUserData> {
  const response = await api.get(`api/user`)
  const data: OpenPaasUserData = await response.json()

  return normalizeOpenPaasUser(data)
}

export async function fetchUserByEmail(
  email: string
): Promise<Array<Record<string, string>>> {
  const r = await api(`api/users?email=${encodeURIComponent(email)}`)
  if (!r.ok) {
    return []
  }
  return r.json()
}

export async function fetchUserById(id: string): Promise<OpenPaasUserData> {
  const entity = await fetchEntityById(id)
  if (!entity.user) {
    throw new Error(`User entity not found for id ${id}`)
  }
  return normalizeOpenPaasUser(entity.user)
}

export async function searchPeople(
  query: string,
  objectTypes: string[] = ['user', 'contact']
): Promise<SearchResponseItem[]> {
  const response: SearchResponseItem[] = await api
    .post(`api/people/search`, {
      body: JSON.stringify({
        limit: 10,
        objectTypes,
        q: query
      })
    })
    .json()
  return response.map(item => new SearchResponseItem(item))
}

// Configuration writes are queued rather than raced: the timezone detection
// saves in the background while the settings page saves on a click, and two
// of them in flight together could reach the backend in the wrong order and
// leave it holding the older choice.
let pendingConfigurationPatch: Promise<unknown> = Promise.resolve()

export async function patchConfigurations(
  modules: ModuleConfiguration[]
): Promise<Response> {
  const patch = pendingConfigurationPatch.then(() =>
    api.patch(`api/configurations?scope=user`, {
      json: modules
    })
  )
  // A write that failed must not hold the next one back, nor reject it.
  pendingConfigurationPatch = patch.catch(() => undefined)

  return await patch
}
