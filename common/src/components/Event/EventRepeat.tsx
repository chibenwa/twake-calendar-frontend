import { RepetitionObject } from '@common/types/Repetition'
import {
  Box,
  FormControl,
  FormControlLabel,
  MenuItem,
  Radio,
  RadioGroup,
  Select,
  SelectChangeEvent,
  Stack,
  TextField,
  Typography
} from '@linagora/twake-mui'
import { DatePicker } from '@mui/x-date-pickers/DatePicker'
import { TwakeLocalizationProvider } from '@common/components/DateTimePicker'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useI18n } from 'twake-i18n'
import { ReadOnlyDateField } from './components/ReadOnlyPickerField'
import {
  formatLocalizedDate,
  getLongDateFormat
} from './utils/dateTimeFormatters'
import { FC_DAYS, WeekDaySelector } from './WeekDaySelector'
import { formValueWeekday } from './utils/formValueWeekday'
import { useScreenSizeDetection } from '@common/useScreenSizeDetection'
import {
  preventFloatNumber,
  toPositiveInt
} from '@common/utils/preventFloatNumber'
import { useResponsiveInputSize } from '@common/hooks/useResponsiveInputSize'
import { getDateFieldSlotProps } from './components/DateTimeFields/dateTimePickerSlotProps'
import {
  isValidRepeatInterval,
  MAX_REPEAT_INTERVAL
} from './utils/formValidation'
// Horizontal input padding (14px each side) plus room for proportional glyphs
// wider than the "0" a `ch` measures, such as "W" or "M".
const END_DATE_FIELD_PADDING = 40

const numericSlotProps = {
  htmlInput: {
    inputMode: 'numeric'
  }
}

const parseInterval = (raw: string): number =>
  raw.trim() === '' ? 0 : Number(raw)

export const RepeatEvent: React.FC<{
  repetition: RepetitionObject
  eventStart: string
  setRepetition: (repetition: RepetitionObject) => void
  isOwn?: boolean
}> = ({ repetition, eventStart, setRepetition, isOwn = true }) => {
  const { t, lang } = useI18n()
  const { isTooSmall: isMobile } = useScreenSizeDetection()
  const inputSize = useResponsiveInputSize()
  const dateCalendarLayoutSx = {
    '& .MuiDateCalendar-root.MuiDateCalendar-root': {
      width: '260px',
      maxWidth: '260px',
      padding: '0 15px'
    }
  }

  // Fully derived — occurrences takes priority over endDate
  const getEndOption = (): 'after' | 'on' | 'never' => {
    if (repetition.occurrences) return 'after'
    if (repetition.endDate) return 'on'
    return 'never'
  }

  const endOption = getEndOption()

  const defaultEndDate = dayjs(eventStart).add(1, 'day').format('YYYY-MM-DD')
  // The field grows with the long date it shows, which is wider on touch
  // screens where the font is larger, so that no part of it gets cut.
  const endDateFontSize = isMobile ? 16 : 14
  const endDateLabel = formatLocalizedDate(
    repetition.endDate || defaultEndDate,
    lang
  )

  // Keep the raw input so that empty or out-of-range values stay visible
  // and can be flagged instead of being silently coerced.
  const [intervalInput, setIntervalInput] = useState(
    String(repetition.interval ?? 1)
  )

  useEffect(() => {
    if (parseInterval(intervalInput) !== repetition.interval) {
      setIntervalInput(String(repetition.interval ?? 1))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [repetition.interval])

  const isIntervalValid = isValidRepeatInterval(parseInterval(intervalInput))

  return (
    <Box>
      <Stack>
        {/* Interval */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
          <Typography variant="h6">{t('event.repeat.every')}</Typography>
          <TextField
            type="number"
            value={intervalInput}
            onKeyDown={preventFloatNumber}
            disabled={!isOwn}
            onChange={e => {
              setIntervalInput(e.target.value)
              setRepetition(
                new RepetitionObject({
                  ...repetition,
                  interval: parseInterval(e.target.value)
                })
              )
            }}
            error={!isIntervalValid}
            size={inputSize}
            style={{ width: 80 }}
            slotProps={{
              htmlInput: {
                ...numericSlotProps.htmlInput,
                min: 1,
                max: MAX_REPEAT_INTERVAL,
                step: 1,
                'aria-label': t('event.repeat.repeatEvery'),
                'data-testid': 'repeat-interval',
                style: {
                  textAlign: 'center',
                  paddingRight: 5
                }
              }
            }}
          />
          <FormControl size="small" style={{ minWidth: 120 }}>
            <Select
              value={repetition.freq ?? 'daily'}
              disabled={!isOwn}
              SelectDisplayProps={{
                'aria-label': t('event.repeat.frequencyAriaLabel')
              }}
              onChange={(e: SelectChangeEvent) => {
                if (e.target.value === 'weekly') {
                  const icsDay = formValueWeekday(eventStart)
                  setRepetition(
                    new RepetitionObject({
                      ...repetition,
                      freq: e.target.value,
                      byday: icsDay ? [icsDay] : null
                    })
                  )
                } else {
                  setRepetition(
                    new RepetitionObject({
                      ...repetition,
                      freq: e.target.value,
                      byday: null
                    })
                  )
                }
              }}
            >
              <MenuItem value="daily">
                {t('event.repeat.frequency.days')}
              </MenuItem>
              <MenuItem value="weekly">
                {t('event.repeat.frequency.weeks')}
              </MenuItem>
              <MenuItem value="monthly">
                {t('event.repeat.frequency.months')}
              </MenuItem>
              <MenuItem value="yearly">
                {t('event.repeat.frequency.years')}
              </MenuItem>
            </Select>
          </FormControl>
        </Box>
        {!isIntervalValid && (
          <Typography
            variant="caption"
            color="error"
            role="alert"
            data-testid="repeat-interval-error"
            sx={{ mb: 1 }}
          >
            {t('event.validation.invalidRepeatInterval', {
              max: MAX_REPEAT_INTERVAL
            })}
          </Typography>
        )}

        {/* Weekly selection */}
        {repetition.freq === 'weekly' && (
          <Box sx={{ mb: 2 }}>
            <WeekDaySelector
              selectedDays={(repetition.byday ?? [])
                .map(ics => FC_DAYS.find(d => d.ics === ics)?.fc ?? -1)
                .filter(d => d !== -1)}
              onChange={fcDays => {
                const icsDays = fcDays
                  .map(fc => FC_DAYS.find(d => d.fc === fc)?.ics ?? '')
                  .filter(Boolean)
                setRepetition(
                  new RepetitionObject({
                    ...repetition,
                    byday: icsDays.length > 0 ? icsDays : null
                  })
                )
              }}
              disabled={!isOwn}
            />
          </Box>
        )}

        {/* End options */}
        <Box>
          <Typography variant="h6" gutterBottom>
            {t('event.repeat.end.label')}
          </Typography>
          <RadioGroup
            value={endOption}
            onChange={e => {
              const value = e.target.value
              if (value === endOption) return

              if (value === 'never') {
                setRepetition(
                  new RepetitionObject({
                    ...repetition,
                    occurrences: null,
                    endDate: null
                  })
                )
              }
              if (value === 'after') {
                setRepetition(
                  new RepetitionObject({
                    ...repetition,
                    occurrences:
                      repetition.occurrences && repetition.occurrences > 0
                        ? repetition.occurrences
                        : 1,
                    endDate: null
                  })
                )
              }
              if (value === 'on') {
                setRepetition(
                  new RepetitionObject({
                    ...repetition,
                    occurrences: null,
                    endDate: repetition.endDate || defaultEndDate
                  })
                )
              }
            }}
          >
            <FormControlLabel
              disabled={!isOwn}
              value="never"
              control={<Radio />}
              label={
                <Typography variant="h6">
                  {t('event.repeat.end.never')}
                </Typography>
              }
            />

            <FormControlLabel
              disabled={!isOwn}
              value="on"
              sx={{ mt: 1 }}
              control={<Radio />}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography variant="h6">
                    {t('event.repeat.end.on')}
                  </Typography>
                  <TwakeLocalizationProvider>
                    <Box
                      data-testid="event-repeat-end-date-container"
                      sx={{
                        fontSize: endDateFontSize,
                        width: `calc(${endDateLabel.length}ch + ${END_DATE_FIELD_PADDING}px)`,
                        minWidth: 220,
                        maxWidth: '100%',
                        '& .MuiInputBase-root': {
                          fontSize: endDateFontSize
                        },
                        '& .MuiInputBase-input': {
                          fontSize: endDateFontSize
                        }
                      }}
                    >
                      <DatePicker
                        sx={{ width: '100%' }}
                        format={getLongDateFormat(lang)}
                        minDate={dayjs(eventStart)}
                        value={
                          repetition.endDate
                            ? dayjs(repetition.endDate)
                            : dayjs(defaultEndDate)
                        }
                        onChange={value => {
                          if (!value || !value.isValid()) return
                          const newDateStr = value.format('YYYY-MM-DD')
                          setRepetition(
                            new RepetitionObject({
                              ...repetition,
                              occurrences: null,
                              endDate: newDateStr
                            })
                          )
                        }}
                        onOpen={() => {
                          if (!isOwn || endOption === 'on') return
                          setRepetition(
                            new RepetitionObject({
                              ...repetition,
                              occurrences: null,
                              endDate: repetition.endDate || defaultEndDate
                            })
                          )
                        }}
                        slots={{ field: ReadOnlyDateField }}
                        slotProps={{
                          field: getDateFieldSlotProps(
                            'event-repeat-end-date',
                            false,
                            t('event.repeat.end.endDateAriaLabel'),
                            isMobile
                          ),
                          layout: { sx: dateCalendarLayoutSx }
                        }}
                        disabled={!isOwn}
                      />
                    </Box>
                  </TwakeLocalizationProvider>
                </Box>
              }
            />

            <FormControlLabel
              disabled={!isOwn}
              value="after"
              control={<Radio />}
              sx={{ mt: 1 }}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography variant="h6">
                    {t('event.repeat.end.after')}
                  </Typography>
                  <TextField
                    type="number"
                    size={inputSize}
                    value={repetition.occurrences || 1}
                    onChange={e => {
                      const value = toPositiveInt(e.target.value)
                      setRepetition(
                        new RepetitionObject({
                          ...repetition,
                          endDate: null,
                          occurrences: value > 0 ? value : 1
                        })
                      )
                    }}
                    sx={{ width: 100 }}
                    onKeyDown={preventFloatNumber}
                    disabled={!isOwn}
                    slotProps={{
                      htmlInput: {
                        ...numericSlotProps.htmlInput,
                        min: 1,
                        step: 1,
                        'aria-label': t(
                          'event.repeat.end.occurrencesAriaLabel'
                        ),
                        'data-testid': 'occurrences-input'
                      }
                    }}
                  />
                  <Typography variant="h6">
                    {t('event.repeat.end.occurrences')}
                  </Typography>
                </Box>
              }
            />
          </RadioGroup>
        </Box>
      </Stack>
    </Box>
  )
}

export default RepeatEvent
