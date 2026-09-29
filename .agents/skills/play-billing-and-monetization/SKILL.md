---
name: play-billing-and-monetization
description: >-
  Use this skill when working with Google Play Billing integration, managing Pro entitlement states,
  handling in-app purchases, implementing trial timers, verifying purchase signatures, or integrating Play Store Review prompts.
---

# Google Play Billing & Monetization Skill

This skill provides comprehensive patterns and workflows for Google Play Billing Library (PBL 9.1.0), in-app monetization, RSA signature verification, rewarded ad trial timers, AdMob banner controls, and in-app review prompts in **DiceRoller**.

---

## 💎 Monetization Architecture Overview

Monetization is partitioned across four primary modules:
- **[`BillingManager.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/BillingManager.kt)**: Manages Play Billing service connection (`BillingClient`), product query, purchase flow, auto-reconnection, and purchase restoration.
- **[`Security.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/Security.kt)**: Performs cryptographic RSA verification on purchases (`SHA1withRSA` algorithm).
- **[`PrefsHelper.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/PrefsHelper.kt)**: Persists Lifetime Pro status (`KEY_PRO_PURCHASED`), 6-hour trial expiration timestamp (`KEY_PRO_TRIAL_EXPIRES_AT`), and review prompt state.
- **[`ProStatusProvider.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/ProStatusProvider.kt)**: Interface abstraction passed to ViewModels to verify active Pro entitlement.

---

## 🛍️ Product Specifications & Entitlements

- **Product ID**: `dice_roller_pro_lifetime`
- **Product Type**: `BillingClient.ProductType.INAPP`
- **Entitlement Tiers**:
  - **Free Tier**: D4, D6, D8 (unlocked for all users); up to 10 roll history items (previewed in `ModalBottomSheet`); banner ads enabled.
  - **Pro Tier (Temporary Trial or Lifetime)**: Unlocks advanced dice types (**D10, D12, and D20**); unlocks unlimited roll history (up to 10,000 records in full [`HistoryActivity`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/HistoryActivity.kt)); removes AdMob banner ads.
  - **Lifetime Pro EXCLUSIVE**: Custom dice face color tints (**Golden 🏆, Blood Red 🩸, Neon Green 🍏, Midnight Purple 🍇**) are strictly reserved for Lifetime Pro owners. Non-lifetime users receive an upsell dialog upon clicking them.

> [!IMPORTANT]
> D4, D6, and D8 are **Free** (`DiceType.proOnly = false`). Only D10, D12, and D20 have `proOnly = true`.

---

## 🔄 Purchase Execution Flow

1. **Client Setup**:
   `BillingManager.startConnection()` configures `enablePendingPurchases(enableOneTimeProducts())`, `enableAutoServiceReconnection()`, and queries product details asynchronously.
2. **Launching Checkout**:
   `BillingManager.launchPurchaseFlow(activity)` prepares `BillingFlowParams` with cached or on-demand fetched `ProductDetails` for `dice_roller_pro_lifetime`.
3. **Purchase Callback**:
   `onPurchasesUpdated(billingResult, purchases)` inspects results.
   - If `ITEM_ALREADY_OWNED`: Automatically invokes `restorePurchases()` to re-grant lost state.
   - If `OK` and `purchaseState == PURCHASED`: Passes purchase to `handlePurchase()`.
4. **Signature Verification**:
   Calls `Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, purchase.originalJson, purchase.signature)`. Uses `SHA1withRSA`. Fails safely if verification fails.
5. **Acknowledgment**:
   Calls `billingClient.acknowledgePurchase(...)` to prevent Google Play auto-refunding after 3 days.
6. **State Persistence**:
   Calls `PrefsHelper.setProPurchased(context)` to mark permanent ownership and clean up any active trial timestamp.

---

## ⏳ Temporary Pro Trial (Rewarded Ads)

- Non-Pro users can watch an AdMob rewarded video ad (`R.string.rewarded_ad_unit_id`) to unlock a **6-hour** Pro trial.
- **Activation**: `PrefsHelper.activateProTrial(context)` sets expiration time to `System.currentTimeMillis() + 6 * 3600 * 1000`.
- **Status Evaluation**: `PrefsHelper.isProActive(context)` returns `true` if `isLifetimePro(context)` OR `System.currentTimeMillis() < expiresAt`.
- **Remaining Time Display**: Formatted via `PrefsHelper.formatRemainingTime(context)` (e.g. `"5h 42m"`, `"18m"`, or `"<1m"`).

---

## ⭐️ Google Play In-App Review Flow

Integrates Google Play Review KTX (`com.google.android.play:review-ktx`):
- **Triggers**:
  1. **Roll Counter**: In [`MainActivity.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/MainActivity.kt), triggered upon completing a roll if `PrefsHelper.shouldAskForReview(context)` is `true`.
     - First prompt shows after **15** rolls (`KEY_REVIEW_NEXT_ROLL_COUNT = 15`).
     - Subsequent prompts show after every **50** rolls (`REVIEW_INTERVAL = 50`).
  2. **Settings Star Rating**: In [`SettingsActivity.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/SettingsActivity.kt), selecting 4 or 5 stars triggers `launchReviewFlow()`.
- **User Actions in Review Dialog**:
  - **"Review ⭐️"**: Launches `ReviewManager.launchReviewFlow(...)` and marks `PrefsHelper.markReviewAsked(context)`.
  - **"Later 🕒"**: Postpones review prompt for 3 days via `PrefsHelper.setReviewRemindLater(context, 3)`.
  - **"Never 🚫"**: Permanently dismisses future prompts via `PrefsHelper.markReviewAsked(context)`.
  - **Dismiss / Backdrop**: Postpones roll count threshold by 50 rolls via `PrefsHelper.postponeReviewRollCount(context, 50)`.
- **Fast-Dismiss / Quota Fallback**:
  If Google Play Review dialog completes in `< 400ms` (indicating quota exhaustion or user suppression), the app falls back to opening the Play Store app listing directly:
  `market://details?id=developer.android.vd.diceroller` with fallback to `https://play.google.com/store/apps/details?id=developer.android.vd.diceroller`.

---

## 📢 AdMob Banner Controls

- **Ad Unit**: `R.string.banner_ad_unit_id` (injected via `resValue`).
- **New User Delay**: For users with fewer than 5 rolls (`PrefsHelper.getRollCount(context) < 5`), the banner ad is delayed by 3 seconds on first launch to ensure smooth onboarding.
- **Suppression**: AdMob banner is completely hidden when `PrefsHelper.isProActive(context)` is `true`.
