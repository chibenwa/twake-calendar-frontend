import React from 'react'

export interface MoreLinkContentProps {
  text: string
  shortText: string
  isCompact: boolean
}

// Narrow month cells cannot fit "+N more": show "+N" and keep the full text for screen readers
export const MoreLinkContent: React.FC<MoreLinkContentProps> = ({
  text,
  shortText,
  isCompact
}) => {
  if (!isCompact) {
    return <>{text}</>
  }

  return (
    <>
      <span aria-hidden="true">{shortText}</span>
      <span className="visually-hidden">{text}</span>
    </>
  )
}
