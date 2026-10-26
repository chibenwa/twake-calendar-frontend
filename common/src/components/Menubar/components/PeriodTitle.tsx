import { useAppSelector } from '@common/app/hooks'

/** The displayed period heads the calendar page: search and settings have their own title. */
export const usePeriodTitleTag = (): 'h1' | 'p' => {
  const view = useAppSelector(state => state.settings.view)
  return view === 'calendar' ? 'h1' : 'p'
}

export const PeriodTitle: React.FC<{ label: string }> = ({ label }) =>
  usePeriodTitleTag() === 'h1' ? (
    <h1 className="period-title">{label}</h1>
  ) : (
    <p className="period-title">{label}</p>
  )
