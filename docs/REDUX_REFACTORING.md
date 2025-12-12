# 🔥 Point 2: Redux Refactoring Guidelines

> **Problem**: CalendarSlice.ts is 1,180 lines with 22 async thunks mixing API calls, data transformation, and business logic.

> **Solution**: Split into RTK Query API + Services + Simplified Slices

---

## 📋 Table of Contents

1. [Current Problems](#current-problems)
2. [New Architecture](#new-architecture)
3. [Migration Strategy](#migration-strategy)
4. [Code Examples](#code-examples)
5. [Testing Strategy](#testing-strategy)
6. [Checklist](#checklist)

---

## 🔴 Current Problems

### CalendarSlice.ts Analysis (1,180 lines)

```typescript
// ❌ BEFORE: Everything in one place
export const getCalendarsListAsync = createAsyncThunk(
  'calendars/getCalendars',
  async (_, { rejectWithValue, getState }) => {
    // API call
    const calendars = await getCalendars(user.id);

    // Data transformation
    const normalizedCalendars = rawCalendars.map((cal) => { /* ... */ });

    // Business logic (owner batching)
    for (let i = 0; i < uniqueOwnerIds.length; i += OWNER_BATCH_SIZE) {
      await Promise.all(/* ... */);
    }

    // Error handling
    errors.push(formatReduxError(error));

    return { importedCalendars, errors };
  }
);
```

**Issues:**
- 🔴 API calls mixed with state management
- 🔴 Business logic (batching) inside thunk
- 🔴 Data transformation not reusable
- 🔴 Hard to test in isolation
- 🔴 Cannot cache or invalidate properly

---

## ✅ New Architecture

### Separation of Concerns

```
┌─────────────────────────────────────────────────────────────┐
│                       COMPONENT                              │
│  Only renders UI, handles user interactions                 │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    CUSTOM HOOK                               │
│  Combines data from API + local state                       │
│  Example: useCalendars(), useCalendarEvents()               │
└─────────────────────────────────────────────────────────────┘
                            ▼
        ┌───────────────────────────────────────┐
        │                                       │
        ▼                                       ▼
┌──────────────────┐                  ┌──────────────────┐
│   RTK QUERY API  │                  │   REDUX SLICE    │
│  Server state    │                  │   UI state       │
│  Caching         │                  │   View prefs     │
│  Invalidation    │                  │   Selections     │
└──────────────────┘                  └──────────────────┘
        ▼
┌─────────────────────────────────────────────────────────────┐
│                     TRANSFORMERS                             │
│  Raw API data → Normalized app data                         │
└─────────────────────────────────────────────────────────────┘
        ▼
┌─────────────────────────────────────────────────────────────┐
│                  BUSINESS SERVICES                           │
│  Pure functions: batching, validation, calculations         │
└─────────────────────────────────────────────────────────────┘
        ▼
┌─────────────────────────────────────────────────────────────┐
│                    API ENDPOINTS                             │
│  HTTP calls only, no business logic                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Migration Strategy

### Phase 1: Setup RTK Query (1-2 days)

#### Step 1.1: Install dependencies (if needed)
```bash
# RTK already includes RTK Query
npm install @reduxjs/toolkit react-redux
```

#### Step 1.2: Create API base

**File: `src/features/calendars/api/calendarApi.ts`**

```typescript
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import { getCalendarBaseUrl } from '@/config';

export const calendarApi = createApi({
  reducerPath: 'calendarApi',
  baseQuery: fetchBaseQuery({
    baseUrl: getCalendarBaseUrl(),
    prepareHeaders: (headers) => {
      // Add auth headers here
      return headers;
    },
  }),
  tagTypes: ['Calendar', 'Event'],
  endpoints: (builder) => ({
    // Endpoints will be added here
  }),
});

export const {} = calendarApi;
```

#### Step 1.3: Register in store

**File: `src/app/store.ts`**

```typescript
import { configureStore } from '@reduxjs/toolkit';
import { calendarApi } from '@/features/calendars/api/calendarApi';

export const store = configureStore({
  reducer: {
    [calendarApi.reducerPath]: calendarApi.reducer,
    // ... other reducers
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(calendarApi.middleware),
});
```

---

### Phase 2: Extract Business Logic (2-3 days)

#### Step 2.1: Create service layer

**File: `src/features/calendars/services/ownerService.ts`**

```typescript
import { getUserDetails } from '@/features/users/api/userApi';

/**
 * Fetches owner data in batches to avoid server overload
 * Extracted from CalendarSlice.ts:89-114
 */
export async function fetchOwnersInBatches(
  ownerIds: string[],
  batchSize: number = 20
): Promise<Map<string, OwnerData>> {
  const ownerDataMap = new Map<string, OwnerData>();
  const uniqueIds = Array.from(new Set(ownerIds.filter(Boolean)));

  const fetchOwnerData = async (ownerId: string): Promise<void> => {
    try {
      const data = await getUserDetails(ownerId);
      ownerDataMap.set(ownerId, data);
    } catch (error) {
      console.error(`Failed to fetch user details for ${ownerId}:`, error);
      ownerDataMap.set(ownerId, {
        firstname: '',
        lastname: 'Unknown User',
        emails: [],
      });
    }
  };

  // Process in batches
  for (let i = 0; i < uniqueIds.length; i += batchSize) {
    const chunk = uniqueIds.slice(i, i + batchSize);
    await Promise.all(chunk.map(fetchOwnerData));
  }

  return ownerDataMap;
}

interface OwnerData {
  firstname: string;
  lastname: string;
  emails: string[];
}
```

**File: `src/features/calendars/services/calendarService.ts`**

```typescript
/**
 * Normalizes raw calendar data from API
 */
export function normalizeCalendar(rawCalendar: any, userId: string) {
  const description = rawCalendar['caldav:description'];
  let delegated = false;
  let source = rawCalendar['calendarserver:source']
    ? rawCalendar['calendarserver:source']._links.self.href
    : rawCalendar._links.self.href;

  const link = rawCalendar._links.self.href;

  if (rawCalendar['calendarserver:delegatedsource']) {
    source = rawCalendar['calendarserver:delegatedsource'];
    delegated = true;
  }

  const id = source.replace('/calendars/', '').replace('.json', '');
  const ownerId = id.split('/')[0];

  return {
    id,
    ownerId,
    delegated,
    link,
    description,
    rawCalendar,
  };
}

/**
 * Builds calendar display name
 */
export function buildCalendarName(
  rawCalendar: any,
  ownerId: string,
  currentUserId: string,
  ownerData: OwnerData
): string {
  if (ownerId !== currentUserId && rawCalendar['dav:name'] === '#default') {
    const firstName = ownerData.firstname ? `${ownerData.firstname} ` : '';
    return `${firstName}${ownerData.lastname}'s calendar`;
  }
  return rawCalendar['dav:name'];
}
```

#### Step 2.2: Create transformers

**File: `src/features/calendars/api/transformers.ts`**

```typescript
import { Calendars } from '../types';
import { normalizeCalendar, buildCalendarName } from '../services/calendarService';
import { fetchOwnersInBatches } from '../services/ownerService';
import { getCalendarVisibility } from '@/components/Calendar/utils/calendarUtils';

/**
 * Transforms API calendar response to app format
 */
export async function transformCalendarListResponse(
  response: any,
  userId: string
): Promise<Record<string, Calendars>> {
  const rawCalendars = response._embedded['dav:calendar'] as any[];

  // Step 1: Normalize all calendars
  const normalized = rawCalendars.map((cal) => normalizeCalendar(cal, userId));

  // Step 2: Fetch owner data in batches
  const ownerIds = normalized.map(({ ownerId }) => ownerId);
  const ownerDataMap = await fetchOwnersInBatches(ownerIds);

  // Step 3: Build final calendar objects
  const importedCalendars: Record<string, Calendars> = {};

  for (const { id, ownerId, delegated, link, description, rawCalendar } of normalized) {
    const ownerData = ownerDataMap.get(ownerId) || {
      firstname: '',
      lastname: 'Unknown User',
      emails: [],
    };

    const name = buildCalendarName(rawCalendar, ownerId, userId, ownerData);

    const color = {
      light: rawCalendar['apple:color'] ?? '#006BD8',
      dark: rawCalendar['X-TWAKE-Dark-theme-color'] ?? '#FFF',
    };

    const visibility = getCalendarVisibility(rawCalendar['acl']);

    importedCalendars[id] = {
      id,
      name,
      link,
      owner: `${ownerData.firstname ? `${ownerData.firstname} ` : ''}${ownerData.lastname}`,
      ownerEmails: ownerData.emails,
      description,
      delegated,
      color,
      visibility,
      events: {},
    };
  }

  return importedCalendars;
}
```

---

### Phase 3: Create RTK Query Endpoints (3-4 days)

#### Add endpoints one by one

**File: `src/features/calendars/api/calendarApi.ts`**

```typescript
export const calendarApi = createApi({
  // ... base config
  endpoints: (builder) => ({

    // ✅ GET /calendars/:userId
    getCalendars: builder.query<Record<string, Calendars>, string>({
      query: (userId) => `/calendars/${userId}.json`,
      transformResponse: (response: any, meta, userId) =>
        transformCalendarListResponse(response, userId),
      providesTags: (result) =>
        result
          ? [
              ...Object.keys(result).map((id) => ({ type: 'Calendar' as const, id })),
              { type: 'Calendar', id: 'LIST' },
            ]
          : [{ type: 'Calendar', id: 'LIST' }],
    }),

    // ✅ GET /calendars/:calId - Get calendar details with events
    getCalendarDetail: builder.query<
      { calId: string; events: CalendarEvent[] },
      { calId: string; start: string; end: string; signal?: AbortSignal }
    >({
      query: ({ calId, start, end }) => ({
        url: `/calendars/${calId}.json`,
        params: { start, end },
      }),
      transformResponse: (response: any, meta, arg) =>
        transformCalendarEventsResponse(response, arg.calId),
      providesTags: (result, error, { calId }) => [
        { type: 'Event', id: calId },
      ],
    }),

    // ✅ POST /calendars - Create calendar
    createCalendar: builder.mutation<Calendars, Partial<Calendars>>({
      query: (calendar) => ({
        url: '/calendars',
        method: 'POST',
        body: calendar,
      }),
      invalidatesTags: [{ type: 'Calendar', id: 'LIST' }],
    }),

    // ✅ PROPPATCH /calendars/:calId - Update calendar
    updateCalendar: builder.mutation<
      void,
      { calId: string; calLink: string; patch: CalendarPatch }
    >({
      query: ({ calLink, patch }) => ({
        url: calLink,
        method: 'PROPPATCH',
        body: patch,
      }),
      invalidatesTags: (result, error, { calId }) => [
        { type: 'Calendar', id: calId },
      ],
    }),

    // ✅ DELETE /calendars/:calId
    deleteCalendar: builder.mutation<void, { calLink: string; calId: string }>({
      query: ({ calLink }) => ({
        url: calLink,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { calId }) => [
        { type: 'Calendar', id: calId },
        { type: 'Calendar', id: 'LIST' },
      ],
    }),

    // ✅ PUT /calendars/:calId/events - Create event
    createEvent: builder.mutation<
      CalendarEvent,
      { calId: string; event: CalendarEvent; ownerEmail?: string }
    >({
      query: ({ calId, event, ownerEmail }) => ({
        url: `/calendars/${calId}/events`,
        method: 'PUT',
        body: event,
        headers: ownerEmail ? { 'X-Owner-Email': ownerEmail } : {},
      }),
      invalidatesTags: (result, error, { calId }) => [
        { type: 'Event', id: calId },
      ],
    }),

  }),
});

// Export hooks
export const {
  useGetCalendarsQuery,
  useGetCalendarDetailQuery,
  useCreateCalendarMutation,
  useUpdateCalendarMutation,
  useDeleteCalendarMutation,
  useCreateEventMutation,
} = calendarApi;
```

---

### Phase 4: Simplify Redux Slice (1-2 days)

**File: `src/features/calendars/slices/calendarSlice.ts`**

```typescript
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

/**
 * UI state only - server data comes from RTK Query
 */
interface CalendarUIState {
  selectedCalendarIds: string[];
  visibleCalendarIds: string[];
  activeView: 'month' | 'week' | 'day' | 'list';
  currentDate: string;
  filterText: string;
}

const initialState: CalendarUIState = {
  selectedCalendarIds: [],
  visibleCalendarIds: [],
  activeView: 'month',
  currentDate: new Date().toISOString(),
  filterText: '',
};

/**
 * CalendarSlice: 1,180 lines → ~80 lines
 * All async thunks moved to RTK Query
 */
export const calendarUISlice = createSlice({
  name: 'calendarUI',
  initialState,
  reducers: {
    selectCalendar: (state, action: PayloadAction<string>) => {
      if (!state.selectedCalendarIds.includes(action.payload)) {
        state.selectedCalendarIds.push(action.payload);
      }
    },

    deselectCalendar: (state, action: PayloadAction<string>) => {
      state.selectedCalendarIds = state.selectedCalendarIds.filter(
        (id) => id !== action.payload
      );
    },

    toggleCalendarVisibility: (state, action: PayloadAction<string>) => {
      const index = state.visibleCalendarIds.indexOf(action.payload);
      if (index > -1) {
        state.visibleCalendarIds.splice(index, 1);
      } else {
        state.visibleCalendarIds.push(action.payload);
      }
    },

    setActiveView: (state, action: PayloadAction<CalendarUIState['activeView']>) => {
      state.activeView = action.payload;
    },

    setCurrentDate: (state, action: PayloadAction<string>) => {
      state.currentDate = action.payload;
    },

    setFilterText: (state, action: PayloadAction<string>) => {
      state.filterText = action.payload;
    },

    resetUI: () => initialState,
  },
});

export const {
  selectCalendar,
  deselectCalendar,
  toggleCalendarVisibility,
  setActiveView,
  setCurrentDate,
  setFilterText,
  resetUI,
} = calendarUISlice.actions;

export default calendarUISlice.reducer;
```

**File: `src/features/calendars/slices/selectors.ts`**

```typescript
import { createSelector } from '@reduxjs/toolkit';
import { RootState } from '@/app/store';

export const selectVisibleCalendarIds = (state: RootState) =>
  state.calendarUI.visibleCalendarIds;

export const selectSelectedCalendarIds = (state: RootState) =>
  state.calendarUI.selectedCalendarIds;

export const selectActiveView = (state: RootState) =>
  state.calendarUI.activeView;

export const selectCurrentDate = (state: RootState) =>
  state.calendarUI.currentDate;

export const selectFilterText = (state: RootState) =>
  state.calendarUI.filterText;

// Memoized selectors
export const selectHasSelectedCalendars = createSelector(
  [selectSelectedCalendarIds],
  (ids) => ids.length > 0
);
```

---

### Phase 5: Create Custom Hooks (1 day)

**File: `src/features/calendars/hooks/useCalendars.ts`**

```typescript
import { useSelector } from 'react-redux';
import { useGetCalendarsQuery } from '../api/calendarApi';
import { selectVisibleCalendarIds } from '../slices/selectors';

/**
 * Centralized hook to access calendars
 * Combines RTK Query data + UI state
 */
export function useCalendars(userId: string) {
  const {
    data: calendars,
    isLoading,
    error,
    refetch,
  } = useGetCalendarsQuery(userId);

  const visibleIds = useSelector(selectVisibleCalendarIds);

  const visibleCalendars = visibleIds
    .map((id) => calendars?.[id])
    .filter(Boolean);

  const allCalendars = calendars ? Object.values(calendars) : [];

  return {
    calendars,
    allCalendars,
    visibleCalendars,
    isLoading,
    error,
    refetch,
  };
}
```

**File: `src/features/calendars/hooks/useCalendarEvents.ts`**

```typescript
import { useGetCalendarDetailQuery } from '../api/calendarApi';

/**
 * Hook to load calendar events for a date range
 */
export function useCalendarEvents(
  calId: string,
  dateRange: { start: string; end: string },
  options: { enabled?: boolean; refetchOnMount?: boolean } = {}
) {
  const { enabled = true, refetchOnMount = false } = options;

  const { data, isLoading, error, refetch } = useGetCalendarDetailQuery(
    { calId, ...dateRange },
    {
      skip: !enabled,
      refetchOnMountOrArgChange: refetchOnMount,
    }
  );

  return {
    events: data?.events || [],
    isLoading,
    error,
    refetch,
  };
}
```

---

### Phase 6: Update Components (2-3 days)

#### Before (with old CalendarSlice):

```typescript
// ❌ Component directly using Redux thunks
function CalendarList() {
  const dispatch = useDispatch();
  const calendars = useSelector((state: RootState) => state.calendars.list);
  const loading = useSelector((state: RootState) => state.calendars.loading);

  useEffect(() => {
    dispatch(getCalendarsListAsync());
  }, [dispatch]);

  if (loading) return <Spinner />;

  return (
    <div>
      {Object.values(calendars).map((cal) => (
        <CalendarItem key={cal.id} calendar={cal} />
      ))}
    </div>
  );
}
```

#### After (with RTK Query + Custom Hook):

```typescript
// ✅ Component using custom hook
function CalendarList({ userId }: { userId: string }) {
  const { allCalendars, isLoading } = useCalendars(userId);

  if (isLoading) return <Spinner />;

  return (
    <div>
      {allCalendars.map((cal) => (
        <CalendarItem key={cal.id} calendar={cal} />
      ))}
    </div>
  );
}
```

---

## 🧪 Testing Strategy

### Testing Services (Pure Functions)

```typescript
// __test__/features/calendars/services/calendarService.test.ts
import { normalizeCalendar, buildCalendarName } from '@/features/calendars/services/calendarService';

describe('calendarService', () => {
  describe('normalizeCalendar', () => {
    it('should extract calendar ID from source', () => {
      const rawCalendar = {
        _links: { self: { href: '/calendars/user1/cal1.json' } },
        'caldav:description': 'Test calendar',
      };

      const result = normalizeCalendar(rawCalendar, 'user1');

      expect(result.id).toBe('user1/cal1');
      expect(result.ownerId).toBe('user1');
    });

    it('should handle delegated calendars', () => {
      const rawCalendar = {
        _links: { self: { href: '/calendars/user2/cal2.json' } },
        'calendarserver:delegatedsource': '/calendars/user1/cal1',
        'caldav:description': 'Delegated',
      };

      const result = normalizeCalendar(rawCalendar, 'user2');

      expect(result.delegated).toBe(true);
    });
  });
});
```

### Testing RTK Query (with MSW)

```typescript
// __test__/features/calendars/api/calendarApi.test.ts
import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { renderHook, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { useGetCalendarsQuery } from '@/features/calendars/api/calendarApi';
import { store } from '@/app/store';

const server = setupServer(
  rest.get('https://api.example.com/calendars/:userId.json', (req, res, ctx) => {
    return res(ctx.json({
      _embedded: {
        'dav:calendar': [
          {
            'dav:name': 'My Calendar',
            _links: { self: { href: '/calendars/user1/cal1.json' } },
          },
        ],
      },
    }));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('calendarApi', () => {
  it('should fetch calendars', async () => {
    const wrapper = ({ children }) => (
      <Provider store={store}>{children}</Provider>
    );

    const { result } = renderHook(() => useGetCalendarsQuery('user1'), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toBeDefined();
    expect(Object.keys(result.current.data).length).toBeGreaterThan(0);
  });
});
```

### Testing Custom Hooks

```typescript
// __test__/features/calendars/hooks/useCalendars.test.ts
import { renderHook } from '@testing-library/react';
import { Provider } from 'react-redux';
import { useCalendars } from '@/features/calendars/hooks/useCalendars';
import { store } from '@/app/store';

describe('useCalendars', () => {
  it('should return calendars and loading state', () => {
    const wrapper = ({ children }) => (
      <Provider store={store}>{children}</Provider>
    );

    const { result } = renderHook(() => useCalendars('user1'), { wrapper });

    expect(result.current.isLoading).toBeDefined();
    expect(result.current.calendars).toBeDefined();
  });
});
```

---

## ✅ Migration Checklist

### For Each Async Thunk in CalendarSlice:

- [ ] **Step 1**: Identify the thunk to migrate
  - [ ] Document what it does
  - [ ] List all side effects
  - [ ] Note dependencies

- [ ] **Step 2**: Extract business logic
  - [ ] Create service function(s)
  - [ ] Write unit tests for services
  - [ ] Ensure pure functions (no side effects)

- [ ] **Step 3**: Create transformer
  - [ ] Map API response → App types
  - [ ] Write unit tests
  - [ ] Handle edge cases

- [ ] **Step 4**: Create RTK Query endpoint
  - [ ] Define query/mutation
  - [ ] Add transformResponse
  - [ ] Configure cache tags
  - [ ] Test with MSW

- [ ] **Step 5**: Create custom hook (if needed)
  - [ ] Combine RTK Query + UI state
  - [ ] Add TypeScript types
  - [ ] Write tests

- [ ] **Step 6**: Update components
  - [ ] Replace useDispatch with custom hook
  - [ ] Remove useEffect for data fetching
  - [ ] Update tests

- [ ] **Step 7**: Cleanup
  - [ ] Remove old thunk
  - [ ] Remove old reducer case
  - [ ] Update documentation
  - [ ] Verify tests pass

---

## 🎯 Current Progress

Track migration of all 22 async thunks:

### Calendars (6 thunks)
- [ ] `getCalendarsListAsync` → `useGetCalendarsQuery`
- [ ] `getTempCalendarsListAsync` → `useGetTempCalendarsQuery`
- [ ] `getCalendarDetailAsync` → `useGetCalendarDetailQuery`
- [ ] `patchCalendarAsync` → `useUpdateCalendarMutation`
- [ ] `postCalendarAsync` → `useCreateCalendarMutation`
- [ ] `deleteCalendarAsync` → `useDeleteCalendarMutation`

### Events (10 thunks)
- [ ] `putEventAsync` → `useCreateEventMutation`
- [ ] `getEventAsync` → `useGetEventQuery`
- [ ] `updateEventAsync` → `useUpdateEventMutation`
- [ ] `deleteEventAsync` → `useDeleteEventMutation`
- [ ] `deleteEventInstanceAsync` → `useDeleteEventInstanceMutation`
- [ ] `updateSeriesAsync` → `useUpdateSeriesMutation`
- [ ] `moveEventAsync` → `useMoveEventMutation`
- [ ] `importEventAsync` → `useImportEventMutation`
- [ ] ... (continue for all event thunks)

### ACL/Sharing (3 thunks)
- [ ] `updateAclAsync` → `useUpdateAclMutation`
- [ ] `addSharedCalendarAsync` → `useAddSharedCalendarMutation`
- [ ] ... (continue)

### Other (3 thunks)
- [ ] ... (list remaining thunks)

---

## 📚 Additional Resources

- [RTK Query Official Docs](https://redux-toolkit.js.org/rtk-query/overview)
- [RTK Query Best Practices](https://redux-toolkit.js.org/rtk-query/usage/customizing-queries)
- [Testing RTK Query](https://redux-toolkit.js.org/rtk-query/usage/testing)

---

**Next**: Read [Point 3: Architecture Guidelines](./ARCHITECTURE_GUIDELINES.md)
