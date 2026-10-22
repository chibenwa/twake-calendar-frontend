import { RepetitionObject } from '@common/types/Repetition'
import { formValueWeekday } from './formValueWeekday'

/**
 * Repetition to keep when the start of the event moves. A weekly repetition
 * whose only day is the weekday of the previous start was proposed by the
 * form, not picked: it follows the new start, otherwise the series would
 * begin on another day than the start shown in the form. Days the user
 * picked (several, or one that differs from the start) are kept.
 */
export function followStartWeekday(
  repetition: RepetitionObject,
  previousStart: string,
  nextStart: string
): RepetitionObject {
  if (repetition?.freq !== 'weekly' || repetition.byday?.length !== 1) {
    return repetition
  }
  const previousDay = formValueWeekday(previousStart)
  const nextDay = formValueWeekday(nextStart)
  if (!previousDay || !nextDay || previousDay === nextDay) return repetition
  if (repetition.byday[0] !== previousDay) return repetition

  return new RepetitionObject({ ...repetition, byday: [nextDay] })
}
