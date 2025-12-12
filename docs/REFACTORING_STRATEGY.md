# 📋 Refactoring Strategy - Twake Calendar

> **Objective**: Resolve critical structural issues without blocking new feature development.

## 🎯 Problems to Solve

### Point 2: Monolithic Redux State (High Severity)
- `CalendarSlice.ts`: 1,180 lines with 22 async thunks
- Business logic mixed with state management
- 4 different sources of truth for some data
- 131 uses of `any` type

### Point 3: Architectural Chaos (High Severity)
- Massive components: 800-1,210 lines
- 207 relative imports `../../../`
- Logic duplication across files
- No clear separation of concerns

---

## 🗺️ Strategy Overview

```
┌─────────────────────────────────────────────────────────────┐
│                   STEP 0: FOUNDATIONS                        │
│  • Code conventions                                          │
│  • Folder structure                                          │
│  • Path aliases (@/)                                         │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              STEP 1: REDUX REFACTORING                       │
│  Point 2: CalendarSlice → RTK Query + Services              │
│  • Extract business logic                                    │
│  • Create API layers                                         │
│  • Simplify slices                                           │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│          STEP 2: ARCHITECTURAL CLEANUP                       │
│  Point 3: Components + Organization                          │
│  • Split large components                                    │
│  • Create custom hooks                                       │
│  • Eliminate duplication                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Proposed New Structure

```
src/
├── api/                      # 🆕 Centralized API clients
│   ├── base/
│   │   ├── client.ts         # Configured HTTP client (ky)
│   │   └── types.ts          # Common API types
│   └── endpoints/
│       ├── calendars.ts      # Calendar endpoints
│       ├── events.ts         # Event endpoints
│       └── users.ts          # User endpoints
│
├── config/                   # 🆕 Centralized configuration
│   ├── env.ts                # Typed environment variables
│   └── index.ts
│
├── features/                 # Organization by business domain
│   ├── calendars/
│   │   ├── api/              # 🆕 RTK Query API
│   │   │   ├── calendarApi.ts
│   │   │   ├── transformers.ts
│   │   │   └── types.ts
│   │   ├── services/         # 🆕 Pure business logic
│   │   │   ├── calendarService.ts
│   │   │   └── ownerService.ts
│   │   ├── slices/           # 🆕 UI state only
│   │   │   ├── calendarSlice.ts   (100 lines max)
│   │   │   └── selectors.ts
│   │   ├── hooks/            # 🆕 Custom hooks
│   │   │   ├── useCalendars.ts
│   │   │   └── useCalendarEvents.ts
│   │   ├── components/       # Feature-specific components
│   │   │   ├── CalendarList/
│   │   │   ├── CalendarModal/
│   │   │   └── ...
│   │   └── utils/            # Domain utilities
│   │
│   ├── events/
│   │   ├── api/
│   │   ├── services/
│   │   ├── slices/
│   │   ├── hooks/
│   │   ├── components/
│   │   │   ├── EventModal/
│   │   │   │   ├── index.tsx         (100-200 lines)
│   │   │   │   ├── EventForm.tsx
│   │   │   │   ├── EventRepeat.tsx
│   │   │   │   └── useEventForm.ts
│   │   │   └── ...
│   │   └── utils/
│   │
│   ├── users/
│   │   └── ...
│   └── settings/
│       └── ...
│
├── components/               # Reusable components (UI)
│   ├── ui/                   # 🆕 Base components
│   │   ├── Button/
│   │   ├── Dialog/
│   │   ├── Input/
│   │   └── ...
│   ├── layout/               # 🆕 Layout components
│   │   ├── Header/
│   │   ├── Sidebar/
│   │   └── ...
│   └── shared/               # 🆕 Shared business components
│       ├── AttendeeSelector/
│       ├── DateTimePicker/
│       └── ...
│
├── hooks/                    # 🆕 Global reusable hooks
│   ├── useDebounce.ts
│   ├── useMediaQuery.ts
│   └── ...
│
├── utils/                    # Global utilities
│   ├── date/                 # 🆕 Organized by category
│   │   ├── formatters.ts
│   │   ├── parsers.ts
│   │   └── timezone.ts
│   ├── validation/
│   ├── errors/
│   └── ...
│
├── types/                    # 🆕 Global TypeScript types
│   ├── api.ts
│   ├── models.ts
│   └── index.ts
│
└── app/                      # App configuration
    ├── store.ts
    ├── routes.tsx
    └── App.tsx
```

---

## 🎯 Success Metrics

### Quantifiable Objectives

| Metric | Before | Target | Measurement |
|--------|--------|--------|-------------|
| CalendarSlice size | 1,180 lines | < 150 lines | Lines of code |
| Components > 500 lines | 5 files | 0 files | wc -l |
| `any` type usage | 131 | < 20 | grep "any" |
| Relative imports `../` | 207 | < 50 | grep "../" |
| Test coverage | ~40% | > 70% | npm test |
| Build size | ? | -20% | npm run build |

---

## ⚠️ Migration Rules

### Rule #1: No Big Bang
- ✅ Migrate feature by feature
- ✅ Keep old and new code in parallel
- ❌ Never break everything at once

### Rule #2: Tests First
- ✅ Write/fix tests BEFORE refactoring
- ✅ Maintain coverage > 70% during migration
- ❌ Never reduce coverage

### Rule #3: Mandatory Review
- ✅ All major refactoring = 2 reviewers minimum
- ✅ Use guidelines as review checklist
- ❌ No merge without architect approval

### Rule #4: Documentation
- ✅ Update these docs with every change
- ✅ Comment the "why", not the "what"
- ✅ Create ADRs (Architecture Decision Records) for major decisions

---

## 📚 Related Documentation

- [**Point 1: Redux Guidelines**](./REDUX_REFACTORING.md) - How to refactor Redux
- [**Point 2: Architecture Guidelines**](./ARCHITECTURE_GUIDELINES.md) - Code conventions

---

