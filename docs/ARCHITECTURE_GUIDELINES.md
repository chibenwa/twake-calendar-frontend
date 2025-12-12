# ⚠️ Point 3: Architecture & Code Organization Guidelines

> **Problem**: Massive components (800-1,210 lines), 207 relative imports, logic duplication, no clear separation of concerns.

> **Solution**: Component composition, custom hooks, path aliases, clear layering.

---

## 📋 Table of Contents

1. [Current Problems](#current-problems)
2. [File Organization Rules](#file-organization-rules)
3. [Component Guidelines](#component-guidelines)
4. [Path Aliases & Imports](#path-aliases--imports)
5. [Code Duplication](#code-duplication)
6. [Migration Examples](#migration-examples)
7. [Checklist](#checklist)

---

## 🔴 Current Problems

### Massive Components

```typescript
// ❌ EventUpdateModal.tsx: 1,210 lines
export function EventUpdateModal() {
  // 43 useState hooks
  // 15 useEffect hooks
  // Complex form logic
  // API calls
  // Validation
  // UI rendering
  // Error handling
  // ... all in one component
}
```

**Issues:**
- 🔴 Impossible to test in isolation
- 🔴 Cannot reuse any logic
- 🔴 Props drilling nightmare
- 🔴 Re-renders entire form on any change
- 🔴 Merge conflicts guaranteed

### Import Hell

```typescript
// ❌ Deep relative imports
import { parseEvent } from '../../../features/Events/eventUtils';
import { formatDate } from '../../../../utils/dateUtils';
import { Calendar } from '../../../components/Calendar/Calendar';
```

**Issues:**
- 🔴 Hard to refactor (move files)
- 🔴 Difficult to understand dependencies
- 🔴 IDE autocomplete struggles

### Logic Duplication

```typescript
// ❌ Event parsing logic duplicated in 3 files:
// - src/features/Events/eventUtils.ts
// - src/components/Event/utils/eventUtils.tsx
// - src/features/Events/EventApi.ts
```

---

## ✅ File Organization Rules

### Rule #1: Component Size Limits

```typescript
/**
 * MAXIMUM LINES PER FILE:
 * - Components: 200 lines
 * - Hooks: 100 lines
 * - Utilities: 150 lines
 * - Services: 200 lines
 *
 * If exceeded, split into:
 * - Sub-components
 * - Custom hooks
 * - Helper functions
 */
```

### Rule #2: Single Responsibility

```typescript
// ✅ Each file has ONE clear purpose

// Component: UI rendering only
// EventModal/index.tsx
export function EventModal() {
  const form = useEventForm();
  return <EventForm {...form} />;
}

// Hook: Logic only
// EventModal/useEventForm.ts
export function useEventForm() {
  // Form state & validation
}

// Service: Business logic only
// services/eventService.ts
export function validateEvent() {
  // Pure validation function
}
```

### Rule #3: Folder Structure by Feature

```
features/
└── events/
    ├── api/              # API layer
    ├── services/         # Business logic
    ├── hooks/            # Custom hooks
    ├── components/       # Feature components
    │   ├── EventModal/
    │   │   ├── index.tsx           # Main component (< 200 lines)
    │   │   ├── EventForm.tsx       # Sub-component
    │   │   ├── EventRepeat.tsx     # Sub-component
    │   │   ├── useEventForm.ts     # Logic hook
    │   │   ├── types.ts            # Local types
    │   │   └── __tests__/
    │   │       └── EventModal.test.tsx
    │   └── EventList/
    └── utils/            # Feature utilities
```

---

## 🎨 Component Guidelines

### Guideline #1: Component Composition

#### Before (Monolithic):

```typescript
// ❌ EventUpdateModal.tsx: 1,210 lines
export function EventUpdateModal() {
  const [title, setTitle] = useState('');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [location, setLocation] = useState('');
  const [description, setDescription] = useState('');
  const [attendees, setAttendees] = useState([]);
  const [recurrence, setRecurrence] = useState(null);
  const [reminders, setReminders] = useState([]);
  // ... 35 more state variables

  const handleSubmit = async () => {
    // 200 lines of validation and submission logic
  };

  const handleRecurrenceChange = () => {
    // 150 lines of recurrence logic
  };

  return (
    <Dialog>
      {/* 800 lines of JSX */}
      <TextField value={title} onChange={e => setTitle(e.target.value)} />
      <DateTimePicker start={start} end={end} ... />
      <AttendeeSelector attendees={attendees} ... />
      <RecurrenceEditor recurrence={recurrence} ... />
      <ReminderList reminders={reminders} ... />
      {/* ... */}
    </Dialog>
  );
}
```

#### After (Composed):

```typescript
// ✅ EventModal/index.tsx: ~100 lines
export function EventModal({ eventId, onClose }: EventModalProps) {
  const { event, isLoading } = useEvent(eventId);
  const form = useEventForm(event);

  if (isLoading) return <Skeleton />;

  return (
    <Dialog onClose={onClose}>
      <EventForm {...form} />
    </Dialog>
  );
}

// ✅ EventModal/EventForm.tsx: ~150 lines
function EventForm({ form }: EventFormProps) {
  return (
    <form onSubmit={form.handleSubmit}>
      <EventBasicFields
        title={form.title}
        location={form.location}
        description={form.description}
        onChange={form.handleBasicFieldsChange}
      />
      <EventDateTimeFields
        start={form.start}
        end={form.end}
        allDay={form.allDay}
        onChange={form.handleDateTimeChange}
      />
      <EventAttendees
        attendees={form.attendees}
        onChange={form.handleAttendeesChange}
      />
      <EventRecurrence
        recurrence={form.recurrence}
        onChange={form.handleRecurrenceChange}
      />
      <EventReminders
        reminders={form.reminders}
        onChange={form.handleRemindersChange}
      />
    </form>
  );
}

// ✅ EventModal/useEventForm.ts: ~80 lines
export function useEventForm(initialEvent?: CalendarEvent) {
  const [values, setValues] = useState(initialEvent || defaultEvent);
  const { mutate: updateEvent } = useUpdateEventMutation();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const errors = validateEvent(values);
    if (errors) return;
    await updateEvent(values);
  };

  return {
    ...values,
    handleSubmit,
    handleBasicFieldsChange,
    handleDateTimeChange,
    handleAttendeesChange,
    handleRecurrenceChange,
    handleRemindersChange,
  };
}
```

**Benefits:**
- ✅ Each component < 200 lines
- ✅ Easy to test each part
- ✅ Reusable sub-components
- ✅ Clear data flow
- ✅ Better performance (selective re-renders)

---

### Guideline #2: Extract Custom Hooks

#### When to Extract a Hook?

```typescript
// ✅ Extract hook when:
// 1. Logic used in multiple components
// 2. Complex stateful logic (> 3 useState)
// 3. Side effects (useEffect)
// 4. API calls
```

#### Example: Extract Form Logic

**Before:**

```typescript
// ❌ Form logic in component
function CalendarModal() {
  const [name, setName] = useState('');
  const [color, setColor] = useState('#000');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState({});

  const validate = () => {
    const newErrors = {};
    if (!name) newErrors.name = 'Required';
    if (!color) newErrors.color = 'Required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = () => {
    if (!validate()) return;
    // API call...
  };

  return (/* JSX */);
}
```

**After:**

```typescript
// ✅ Form logic in hook
function useCalendarForm(initialValues) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const { mutate } = useCreateCalendarMutation();

  const validate = () => {
    const newErrors = {};
    if (!values.name) newErrors.name = 'Required';
    if (!values.color) newErrors.color = 'Required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    await mutate(values);
  };

  const handleChange = (field, value) => {
    setValues(prev => ({ ...prev, [field]: value }));
  };

  return { values, errors, handleChange, handleSubmit };
}

// ✅ Component uses hook
function CalendarModal() {
  const form = useCalendarForm({ name: '', color: '#000', description: '' });

  return (
    <form onSubmit={form.handleSubmit}>
      <Input
        value={form.values.name}
        onChange={e => form.handleChange('name', e.target.value)}
        error={form.errors.name}
      />
      {/* ... */}
    </form>
  );
}
```

---

### Guideline #3: Component File Organization

```typescript
// ✅ GOOD: Component folder structure

features/events/components/EventModal/
├── index.tsx                 # Main component (< 200 lines)
├── EventForm.tsx             # Sub-component
├── EventFormFields.tsx       # Sub-component
├── EventRecurrence.tsx       # Sub-component
├── useEventForm.ts           # Logic hook
├── useEventValidation.ts     # Validation hook
├── types.ts                  # Local TypeScript types
├── constants.ts              # Local constants
├── styles.ts                 # Styled components (if using)
└── __tests__/
    ├── EventModal.test.tsx
    ├── EventForm.test.tsx
    └── useEventForm.test.ts
```

**Rules:**
- `index.tsx` exports the main component
- Sub-components in same folder (if not reused elsewhere)
- Hooks in separate files with `use` prefix
- Types in `types.ts` (if complex)
- Tests co-located in `__tests__/`

---

## 🔗 Path Aliases & Imports

### Setup Path Aliases

#### Step 1: Update `tsconfig.json`

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"],
      "@/api/*": ["src/api/*"],
      "@/components/*": ["src/components/*"],
      "@/features/*": ["src/features/*"],
      "@/hooks/*": ["src/hooks/*"],
      "@/utils/*": ["src/utils/*"],
      "@/types/*": ["src/types/*"],
      "@/config": ["src/config"]
    }
  }
}
```

#### Step 2: Update `rsbuild.config.ts`

```typescript
import { defineConfig } from '@rsbuild/core';
import path from 'path';

export default defineConfig({
  // ...
  source: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@/api': path.resolve(__dirname, 'src/api'),
      '@/components': path.resolve(__dirname, 'src/components'),
      '@/features': path.resolve(__dirname, 'src/features'),
      '@/hooks': path.resolve(__dirname, 'src/hooks'),
      '@/utils': path.resolve(__dirname, 'src/utils'),
      '@/types': path.resolve(__dirname, 'src/types'),
      '@/config': path.resolve(__dirname, 'src/config'),
    },
  },
});
```

#### Step 3: Update `jest.config.js`

```javascript
module.exports = {
  // ...
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^@/api/(.*)$': '<rootDir>/src/api/$1',
    '^@/components/(.*)$': '<rootDir>/src/components/$1',
    '^@/features/(.*)$': '<rootDir>/src/features/$1',
    '^@/hooks/(.*)$': '<rootDir>/src/hooks/$1',
    '^@/utils/(.*)$': '<rootDir>/src/utils/$1',
    '^@/types/(.*)$': '<rootDir>/src/types/$1',
    '^@/config$': '<rootDir>/src/config',
  },
};
```

### Import Guidelines

```typescript
// ✅ GOOD: Use path aliases

import { Calendar } from '@/features/calendars/components/Calendar';
import { formatDate } from '@/utils/date/formatters';
import { useDebounce } from '@/hooks/useDebounce';
import config from '@/config';
import type { CalendarEvent } from '@/types/models';

// ❌ BAD: Relative imports

import { Calendar } from '../../../features/calendars/components/Calendar';
import { formatDate } from '../../../../utils/date/formatters';
import { useDebounce } from '../../../hooks/useDebounce';
```

### Import Order Convention

```typescript
// ✅ GOOD: Organized imports

// 1. External libraries
import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import dayjs from 'dayjs';

// 2. Internal aliases (alphabetical by category)
import { calendarApi } from '@/features/calendars/api/calendarApi';
import { CalendarList } from '@/features/calendars/components/CalendarList';
import { useCalendars } from '@/features/calendars/hooks/useCalendars';

import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';

import { useDebounce } from '@/hooks/useDebounce';

import { formatDate } from '@/utils/date/formatters';
import { validateEmail } from '@/utils/validation/validators';

// 3. Types
import type { Calendar } from '@/types/models';
import type { RootState } from '@/app/store';

// 4. Relative imports (only for same folder)
import { CalendarItem } from './CalendarItem';
import { useLocalState } from './useLocalState';
import type { CalendarListProps } from './types';
```

---

## 🚫 Code Duplication

### Problem: Duplication Detected

```bash
# Current duplication issues:
# 1. Event parsing logic in 3 files
# 2. Date formatting in multiple utils
# 3. Form validation patterns repeated
```

### Solution: Create Shared Utilities

#### Example 1: Consolidate Event Utils

**Before:**

```typescript
// ❌ src/features/Events/eventUtils.ts (613 lines)
export function parseEvent() { /* ... */ }
export function formatEvent() { /* ... */ }
export function validateEvent() { /* ... */ }

// ❌ src/components/Event/utils/eventUtils.tsx (different file!)
export function parseEvent() { /* ... duplicated ... */ }
export function formatEvent() { /* ... duplicated ... */ }

// ❌ src/features/Events/EventApi.ts
// Inline parsing logic duplicated again
```

**After:**

```typescript
// ✅ src/features/events/utils/eventParser.ts
export function parseCalendarEvent(raw: any): CalendarEvent {
  // Single source of truth
}

// ✅ src/features/events/utils/eventFormatter.ts
export function formatEventForAPI(event: CalendarEvent): any {
  // Single source of truth
}

// ✅ src/features/events/utils/eventValidator.ts
export function validateEvent(event: CalendarEvent): ValidationErrors {
  // Single source of truth
}

// ✅ All files import from shared utils
import { parseCalendarEvent } from '@/features/events/utils/eventParser';
```

#### Example 2: Consolidate Date Utils

**Before:**

```typescript
// ❌ Scattered date logic
// src/utils/dateUtils.ts
// src/features/Events/eventUtils.ts
// src/components/Event/utils/dateTimeHelpers.ts
```

**After:**

```typescript
// ✅ src/utils/date/formatters.ts
export function formatDateToISO(date: Date): string { /* ... */ }
export function formatDateToDisplay(date: Date): string { /* ... */ }

// ✅ src/utils/date/parsers.ts
export function parseDateFromISO(iso: string): Date { /* ... */ }
export function parseDateFromICAL(ical: string): Date { /* ... */ }

// ✅ src/utils/date/validators.ts
export function isValidDate(date: any): boolean { /* ... */ }
export function isDateInRange(date: Date, range: DateRange): boolean { /* ... */ }

// ✅ src/utils/date/calculators.ts
export function addDays(date: Date, days: number): Date { /* ... */ }
export function getWeekRange(date: Date): DateRange { /* ... */ }
```

---

## 📦 Migration Examples

### Example 1: Split EventUpdateModal (1,210 → 400 lines total)

#### New structure:

```
features/events/components/EventUpdateModal/
├── index.tsx                      # 100 lines - Main modal
├── EventUpdateForm.tsx            # 120 lines - Form wrapper
├── EventBasicFields.tsx           # 60 lines - Title, location, desc
├── EventDateTimeSection.tsx       # 80 lines - Date/time pickers
├── EventAttendeesSection.tsx      # 70 lines - Attendee management
├── EventRecurrenceSection.tsx     # 90 lines - Recurrence editor
├── EventRemindersSection.tsx      # 50 lines - Reminders
├── useEventUpdateForm.ts          # 100 lines - Form logic hook
├── useEventValidation.ts          # 60 lines - Validation logic
├── types.ts                       # 30 lines - Local types
└── __tests__/
    ├── EventUpdateModal.test.tsx
    └── useEventUpdateForm.test.ts
```

#### Migration steps:

```typescript
// ✅ Step 1: Extract form logic to hook
// useEventUpdateForm.ts
export function useEventUpdateForm(eventId: string) {
  const [values, setValues] = useState<EventFormValues>(initialValues);
  const validation = useEventValidation(values);
  const { mutate: updateEvent } = useUpdateEventMutation();

  const handleSubmit = async () => {
    if (!validation.isValid) return;
    await updateEvent(values);
  };

  return { values, setValues, validation, handleSubmit };
}

// ✅ Step 2: Create sub-components
// EventBasicFields.tsx
export function EventBasicFields({ values, onChange }: Props) {
  return (
    <Stack spacing={2}>
      <TextField
        label="Title"
        value={values.title}
        onChange={e => onChange('title', e.target.value)}
      />
      <TextField
        label="Location"
        value={values.location}
        onChange={e => onChange('location', e.target.value)}
      />
      <TextField
        label="Description"
        multiline
        rows={4}
        value={values.description}
        onChange={e => onChange('description', e.target.value)}
      />
    </Stack>
  );
}

// ✅ Step 3: Compose main component
// index.tsx
export function EventUpdateModal({ eventId, onClose }: Props) {
  const form = useEventUpdateForm(eventId);

  return (
    <Dialog open onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Update Event</DialogTitle>
      <DialogContent>
        <EventUpdateForm {...form} />
      </DialogContent>
    </Dialog>
  );
}
```

### Example 2: Extract Calendar.tsx Logic (817 → 300 lines)

#### Before:

```typescript
// ❌ Calendar.tsx: 817 lines, 43 hooks
export function Calendar() {
  // 15 useState
  // 12 useEffect
  // 8 useSelector
  // 5 useCallback
  // 3 useMemo
  // Event handlers inline (200 lines)
  // View logic inline (150 lines)
  // Rendering (467 lines)
}
```

#### After:

```typescript
// ✅ Calendar/index.tsx: ~150 lines
export function Calendar() {
  const calendar = useCalendarState();
  const events = useCalendarEvents(calendar.dateRange);
  const handlers = useCalendarHandlers();

  return (
    <CalendarContainer>
      <CalendarToolbar {...calendar} {...handlers} />
      <CalendarGrid
        events={events.data}
        view={calendar.view}
        onEventClick={handlers.handleEventClick}
        onDateClick={handlers.handleDateClick}
      />
    </CalendarContainer>
  );
}

// ✅ Calendar/useCalendarState.ts: ~80 lines
export function useCalendarState() {
  const [view, setView] = useState<CalendarView>('month');
  const [currentDate, setCurrentDate] = useState(new Date());
  const dateRange = useMemo(() => calculateRange(currentDate, view), [currentDate, view]);

  return { view, setView, currentDate, setCurrentDate, dateRange };
}

// ✅ Calendar/useCalendarEvents.ts: ~50 lines
export function useCalendarEvents(dateRange: DateRange) {
  const { calendars } = useCalendars();
  const visibleCalendarIds = useSelector(selectVisibleCalendarIds);

  const queries = useQueries(
    visibleCalendarIds.map(calId => ({
      queryKey: ['events', calId, dateRange],
      queryFn: () => fetchEvents(calId, dateRange),
    }))
  );

  const events = useMemo(() => mergeEventQueries(queries), [queries]);

  return { data: events, isLoading: queries.some(q => q.isLoading) };
}

// ✅ Calendar/useCalendarHandlers.ts: ~60 lines
export function useCalendarHandlers() {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const handleEventClick = useCallback((event: CalendarEvent) => {
    navigate(`/events/${event.id}`);
  }, [navigate]);

  const handleDateClick = useCallback((date: Date) => {
    dispatch(openEventModal({ date }));
  }, [dispatch]);

  return { handleEventClick, handleDateClick };
}
```

---

## ✅ Refactoring Checklist

### For Each Large Component (> 500 lines):

- [ ] **Analyze**
  - [ ] Count lines of code
  - [ ] List all useState/useEffect
  - [ ] Identify logical sections
  - [ ] Note duplicated logic

- [ ] **Extract Hooks**
  - [ ] Create `use[Feature]State.ts` for state management
  - [ ] Create `use[Feature]Handlers.ts` for event handlers
  - [ ] Create `use[Feature]Data.ts` for data fetching
  - [ ] Write tests for each hook

- [ ] **Split Components**
  - [ ] Create sub-components for logical sections
  - [ ] Each sub-component < 200 lines
  - [ ] Define clear prop interfaces
  - [ ] Write tests for each component

- [ ] **Organize Files**
  - [ ] Create component folder
  - [ ] Move sub-components to folder
  - [ ] Move hooks to folder
  - [ ] Add `types.ts` if needed
  - [ ] Co-locate tests

- [ ] **Update Imports**
  - [ ] Replace relative imports with path aliases
  - [ ] Organize import order
  - [ ] Remove unused imports

- [ ] **Test**
  - [ ] All existing tests pass
  - [ ] Add tests for new hooks
  - [ ] Add tests for new components
  - [ ] Coverage maintained or improved

- [ ] **Document**
  - [ ] Add JSDoc to public functions
  - [ ] Update README if needed
  - [ ] Add migration notes

---

## 🎯 Current Progress

Track refactoring of large components:

### Components > 800 lines (Priority 1)
- [ ] `EventUpdateModal.tsx` (1,210 lines) → < 500 total
- [ ] `EventDisplayPreview.tsx` (1,039 lines) → < 400 total
- [ ] `Calendar.tsx` (817 lines) → < 300 total

### Components 500-800 lines (Priority 2)
- [ ] `EventFormFields.tsx` (718 lines) → < 300 total
- [ ] `EventUtils.ts` (613 lines) → split into modules

### Architectural Improvements
- [ ] Path aliases configured
- [ ] Import statements updated (207 → < 50 relative imports)
- [ ] Utility consolidation (eliminate duplication)

---

## 📚 Additional Resources

- [React Component Patterns](https://www.patterns.dev/posts/react-component-patterns/)
- [Custom Hooks Best Practices](https://react.dev/learn/reusing-logic-with-custom-hooks)
- [TypeScript Path Mapping](https://www.typescriptlang.org/docs/handbook/module-resolution.html#path-mapping)

---

