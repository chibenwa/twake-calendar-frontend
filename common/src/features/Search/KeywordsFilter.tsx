import { useAppDispatch, useAppSelector } from '@common/app/hooks'
import { MobileSelector } from '@common/components/MobileSelector'
import { setFilters } from '@common/features/Search/SearchSlice'
import { Box, InputLabel, TextField } from '@linagora/twake-mui'
import { useId } from 'react'
import { useI18n } from 'twake-i18n'

interface Props {
  mode: 'popover' | 'mobile'
  error?: boolean
  onErrorClear?: () => void
  onKeywordsChange?: (keywords: string) => void
  onEnter?: () => void
}

export const KeywordsFilter: React.FC<Props> = ({
  mode,
  error,
  onErrorClear,
  onKeywordsChange,
  onEnter
}) => {
  const { t } = useI18n()
  const dispatch = useAppDispatch()
  const inputId = useId()
  const filters = useAppSelector(
    state => state.searchResult.searchParams.filters
  )

  const field = (
    <TextField
      id={inputId}
      fullWidth
      error={error}
      helperText={error ? t('search.error.emptySearch') : ''}
      placeholder={t('search.keywordsPlaceholder')}
      value={filters.keywords}
      onChange={e => {
        dispatch(setFilters({ keywords: e.target.value }))
        if (e.target.value.trim()) {
          onErrorClear?.()
        }
      }}
      onKeyDown={e => {
        if (e.key === 'Enter') {
          onKeywordsChange?.(filters.keywords)
          onEnter?.()
        }
      }}
      size="small"
      slotProps={{
        // not `required`: the popover is no form, and an empty keywords
        // field is reported by the search itself
        htmlInput: { 'aria-required': true }
      }}
    />
  )

  if (mode === 'mobile') {
    return (
      <MobileSelector
        ref={null}
        displayText={`${t('search.keywords')}*`}
        bottomSheetChildren={() => <Box sx={{ p: 2 }}>{field}</Box>}
      />
    )
  }

  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: '140px 1fr',
        gap: 2,
        alignItems: 'center'
      }}
    >
      <InputLabel htmlFor={inputId} required sx={{ m: 0 }}>
        {t('search.keywords')}
      </InputLabel>
      {field}
    </Box>
  )
}
