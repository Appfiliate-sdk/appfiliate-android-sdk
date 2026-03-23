# Appfiliate Android SDK

Lightweight install attribution for mobile app affiliate marketing. Zero-config fingerprint matching with support for Google Install Referrer for deterministic attribution.

- Under 200KB, single dependency (Install Referrer)
- No GAID/AAID required, no special permissions
- One-line install tracking, works on first launch

## Installation

### 1. Add JitPack to your `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency to your app's `build.gradle.kts`

```kotlin
dependencies {
    implementation("com.github.appfiliate:appfiliate-android-sdk:1.0.0")
}
```

## Quick Start

### Configure and track installs

In your `Application.onCreate()` or main `Activity.onCreate()`:

```kotlin
import com.appfiliate.sdk.Appfiliate

// Configure with your credentials from app.appfiliate.io
Appfiliate.configure(this, appId = "APP_ID_HERE", apiKey = "API_KEY_HERE")

// Track the install (safe to call every launch — only runs once)
Appfiliate.trackInstall(this) { result ->
    Log.d("Appfiliate", "Attributed: ${result.matched}, method: ${result.method}")
}
```

### Track purchases

After a successful in-app purchase:

```kotlin
Appfiliate.trackPurchase(
    context = this,
    productId = purchase.products.first(),
    revenue = 9.99,
    currency = "USD",
    transactionId = purchase.orderId
)
```

### Link a user ID (optional)

For server-side integrations (e.g., RevenueCat webhooks):

```kotlin
Appfiliate.setUserId(this, userId = Purchases.sharedInstance.appUserID)
```

### Check attribution status

```kotlin
val attributed = Appfiliate.isAttributed(context)
val id = Appfiliate.attributionId(context)
```

## How It Works

1. On first launch, the SDK collects device signals (model, screen size, timezone, language) and the Google Play Install Referrer
2. These signals are sent to the Appfiliate attribution API
3. The API matches the install to a tracking link click using deterministic referrer matching or fingerprint matching
4. The result is cached locally — `trackInstall()` only fires once per install
5. Subsequent purchases are linked to the attribution via `trackPurchase()`

## Requirements

- Android API 21+ (Android 5.0)
- Internet permission (added automatically by the SDK manifest)

## License

MIT
