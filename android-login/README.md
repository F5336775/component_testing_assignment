## Backend criteria mapping

- Base points after FX conversion → `LoyaltyPointsCalculator.calculate`
- Tier multipliers → `Tier` enum
- Promo warnings → `PromoClient.PromoResult.expiringSoon` → response `warnings`
- Cap total points at 50,000 → `MAX_POINTS` in `LoyaltyPointsCalculator`
- Validation rules → `PointsQuoteVerticle.handleQuote` (400 on invalid input)
- Resilience:
    - FX retry → `fetchFxWithRetry`
    - Promo timeout handling → `VertxPromoClient.timeout(300)` + fallback to 0 bonus
- Component tests:
    - uses Vert.x test extension, WireMock, JUnit 5, AssertJ
    - covers success path, caps, expiry warning, FX failure, promo timeout, validation

## Android criteria mapping

- Validation enables/disables button → `LoginUiState.isLoginEnabled` + tests
- Success → navigation event → `navigateToHome` flag in state + tests
- Error increments failure count → `failureCount` in ViewModel + tests
- Lockout after 3 failures → `isLockedOut` logic + tests
- Offline → show message, no service call → `NetworkMonitor` fake + tests
- Remember me persists token → `isRememberMe` + token state + test
