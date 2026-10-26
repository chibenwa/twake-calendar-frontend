import {
  MobileSelector,
  MobileSelectorHandle
} from '@common/components/MobileSelector'
import { ListItemText } from '@common/ds/ListItemText/ListItemText'
import {
  Box,
  FormControl,
  List,
  ListItem,
  ListItemButton,
  MenuItem,
  Select,
  Typography,
  useTheme
} from '@linagora/twake-mui'
import { Check as CheckIcon } from '@mui/icons-material'
import { useRef, type ReactElement } from 'react'
import { useI18n } from 'twake-i18n'
import { useAppDispatch, useAppSelector } from '@common/app/hooks'
import { updateUserConfigurations } from '@common/features/User/UserSlice'
import { useScreenSizeDetection } from '@common/useScreenSizeDetection'
import { THEME_SETTINGS, type ThemeSetting } from './constants'

// the bottom sheet the Language and Time zone fields open on a phone
function MobileThemeSelector({
  value,
  onChange
}: {
  value: ThemeSetting
  onChange: (theme: ThemeSetting) => void
}): ReactElement {
  const { t } = useI18n()
  const theme = useTheme()
  const selectorRef = useRef<MobileSelectorHandle>(null)

  const handleChange = (setting: ThemeSetting): void => {
    onChange(setting)
    selectorRef.current?.onClose()
  }

  return (
    <MobileSelector
      ref={selectorRef}
      label={t('settings.theme')}
      displayText={t(`settings.themes.${value}`)}
    >
      <List sx={{ overflow: 'auto', flex: 1, pt: 0 }}>
        {THEME_SETTINGS.map(setting => (
          <ListItem key={setting} disablePadding>
            <ListItemButton
              selected={value === setting}
              aria-selected={value === setting}
              onClick={() => handleChange(setting)}
              sx={{ py: 1 }}
            >
              <ListItemText
                primary={
                  <Typography>{t(`settings.themes.${setting}`)}</Typography>
                }
              />
              {value === setting && (
                <CheckIcon
                  sx={{
                    color: theme.vars.palette.primary.main,
                    fontSize: '20px'
                  }}
                />
              )}
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </MobileSelector>
  )
}

export function ThemeSelector({
  inputMinWidth,
  onThemeError
}: {
  inputMinWidth: string | number
  onThemeError: () => void
}): ReactElement {
  const { t } = useI18n()
  const dispatch = useAppDispatch()
  const { isTooSmall: isMobile } = useScreenSizeDetection()
  const savedTheme = useAppSelector(state => state.user.coreConfig.theme)

  const handleThemeChange = (theme: ThemeSetting): void => {
    dispatch(updateUserConfigurations({ theme })).unwrap().catch(onThemeError)
  }

  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="h6" component="h2" sx={{ mb: 1 }}>
        {t('settings.theme')}
      </Typography>
      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
        {t('settings.themeDescription')}
      </Typography>
      {isMobile ? (
        <MobileThemeSelector
          value={savedTheme ?? 'auto'}
          onChange={handleThemeChange}
        />
      ) : (
        <FormControl size="small" sx={{ minWidth: inputMinWidth }}>
          <Select
            value={savedTheme ?? 'auto'}
            onChange={e => handleThemeChange(e.target.value)}
            variant="outlined"
            aria-label={t('settings.themeSelector')}
          >
            {THEME_SETTINGS.map(theme => (
              <MenuItem key={theme} value={theme}>
                {t(`settings.themes.${theme}`)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )}
    </Box>
  )
}
