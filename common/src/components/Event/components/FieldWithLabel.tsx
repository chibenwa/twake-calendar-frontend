import { Box, SxProps, Theme, Typography } from '@linagora/twake-mui'
import React, { createContext, useContext, useId, useRef } from 'react'

// The id of the visible label of the field around, for controls that cannot
// be reached by a <label for>: Selects, toggle groups, search inputs
const FieldLabelContext = createContext<string | undefined>(undefined)

export const useFieldLabelId = (): string | undefined =>
  useContext(FieldLabelContext)

/**
 * Helper component for field with label
 * Supports two modes: normal (label on top) and expanded (label on left)
 */
export const FieldWithLabel = React.memo(
  ({
    label,
    isExpanded,
    children,
    sx
  }: {
    label: string | React.ReactNode
    isExpanded: boolean
    children: React.ReactNode
    sx?: SxProps<Theme>
  }) => {
    const labelId = useId()
    const contentRef = useRef<HTMLDivElement>(null)
    const isEmptyLabel =
      label === null ||
      label === undefined ||
      (typeof label === 'string' && label.trim() === '')
    // Like a <label for>: a click on the label moves to its field
    const labelProps = {
      id: labelId,
      component: 'div' as const,
      onClick: (e: React.MouseEvent<HTMLElement>): void => {
        if ((e.target as HTMLElement).closest('button, a, input')) return
        contentRef.current
          ?.querySelector<HTMLElement>(
            'input:not([type="hidden"]), textarea, [role="combobox"], button'
          )
          ?.focus()
      }
    }
    const content = (
      <FieldLabelContext.Provider value={isEmptyLabel ? undefined : labelId}>
        {children}
      </FieldLabelContext.Provider>
    )

    if (!isExpanded) {
      // Normal mode: label on top
      return (
        <Box
          ref={contentRef}
          sx={[
            {
              '& > *:not(:first-of-type)': {
                marginTop: isEmptyLabel ? 0 : '6px'
              },
              // Only apply margin to direct child MuiTextField-root
              '& > .MuiFormControl-root': {
                marginTop: '6px',
                marginBottom: 0
              },
              '& > .MuiTextField-root': {
                marginTop: '6px',
                marginBottom: 0
              },
              // Reset margin for nested MuiTextField-root
              '& .MuiFormControl-root .MuiTextField-root': {
                marginTop: 0,
                marginBottom: 0
              },
              '& .MuiTextField-root .MuiTextField-root': {
                marginTop: 0,
                marginBottom: 0
              },
              // Reset margin for nested Box children (DateTimeFields structure)
              '& > .MuiBox-root > .MuiBox-root': {
                marginTop: 0
              },
              '& > .MuiBox-root > .MuiBox-root > .MuiBox-root': {
                marginTop: 0
              }
            },
            ...[sx].flat().filter(Boolean)
          ]}
        >
          {!isEmptyLabel && (
            <Typography {...labelProps} variant="h6" sx={{ display: 'block' }}>
              {label}
            </Typography>
          )}
          {content}
        </Box>
      )
    }

    // Extended mode: label on left
    return (
      <Box
        ref={contentRef}
        sx={[
          {
            display: 'flex',
            alignItems: 'center'
          },
          ...(Array.isArray(sx) ? sx : sx ? [sx] : [])
        ]}
      >
        {!isEmptyLabel && (
          <Typography
            {...labelProps}
            variant="h6"
            sx={{
              minWidth: '115px',
              marginRight: '12px',
              flexShrink: 0
            }}
          >
            {label}
          </Typography>
        )}
        <Box
          sx={{
            flexGrow: 1,
            // Set margin-top: 8px for second row in DateTimeFields (4 fields layout)
            '& > .MuiBox-root > .MuiBox-root:nth-of-type(2)': {
              marginTop: '8px'
            },
            // Remove margin from MuiFormControl-root MuiFormControl-marginDense in extended mode
            '& .MuiFormControl-root.MuiFormControl-marginDense': {
              marginTop: 0,
              marginBottom: 0
            },
            '& .MuiTextField-root.MuiFormControl-marginDense': {
              marginTop: 0,
              marginBottom: 0
            }
          }}
        >
          {content}
        </Box>
      </Box>
    )
  }
)

FieldWithLabel.displayName = 'FieldWithLabel'
