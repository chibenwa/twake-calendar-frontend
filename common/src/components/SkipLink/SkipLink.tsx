import { SxProps, Theme } from '@linagora/twake-mui'
import { useI18n } from 'twake-i18n'
import './SkipLink.styl'

/** Id of the element the skip link moves the focus to, one per page. */
export const MAIN_CONTENT_ID = 'main-content'

/** Hides a text from sight while screen readers keep announcing it. */
export const visuallyHidden: SxProps<Theme> = {
  border: 0,
  clip: 'rect(0 0 0 0)',
  height: '1px',
  margin: '-1px',
  overflow: 'hidden',
  padding: 0,
  position: 'absolute',
  width: '1px',
  whiteSpace: 'nowrap'
}

/** First focusable element of a page: jumps over the menus to its content. */
export const SkipLink: React.FC = () => {
  const { t } = useI18n()

  return (
    <a href={`#${MAIN_CONTENT_ID}`} className="skip-link">
      {t('a11y.skipToContent')}
    </a>
  )
}
