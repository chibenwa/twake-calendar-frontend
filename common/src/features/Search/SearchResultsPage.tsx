import { useAppSelector } from '@common/app/hooks'
import { useDocumentTitle } from '@common/hooks/useDocumentTitle'
import { useScreenSizeDetection } from '@common/useScreenSizeDetection'
import { useI18n } from 'twake-i18n'
import DesktopSearchResultsPage from './DesktopSearchResultsPage'
import MobileSearchResultsPage from './MobileSearchResultsPage'
import './searchResult.styl'

const SearchResultsPage: React.FC = () => {
  const { t } = useI18n()
  const query = useAppSelector(state => state.searchResult.searchParams.search)
  useDocumentTitle(t('pageTitle.search', { query }))
  const { isTooSmall: isMobile, isTablet } = useScreenSizeDetection()

  if (isMobile || isTablet) {
    return <MobileSearchResultsPage />
  } else {
    return <DesktopSearchResultsPage />
  }
}

export default SearchResultsPage
