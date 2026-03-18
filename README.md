# Appfiliate Android SDK

Lightweight install attribution for mobile app affiliate marketing.

## Installation

Add JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.yourusername:appfiliate-android-sdk:1.0.0")
}
```

## Quick Start

**Three lines of code.** Add to your Application class or main Activity:

```kotlin
import com.appfiliate.sdk.Appfiliate

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Appfiliate.configure(this, appId = "app_xxx", apiKey = "key_xxx")
        Appfiliate.trackInstall(this)
    }
}
```

That's it. The SDK automatically:
- Reads the Google Play Install Referrer (deterministic attribution)
- Falls back to fingerprint matching if referrer is unavailable
- Caches the result locally

## Track Purchases

```kotlin
Appfiliate.trackPurchase(
    context = this,
    productId = "premium_monthly",
    revenue = 9.99,
    currency = "USD",
    transactionId = "GPA.1234-5678"
)
```

## Check Attribution

```kotlin
if (Appfiliate.isAttributed(this)) {
    val id = Appfiliate.getAttributionId(this)
    Log.d("Appfiliate", "Attributed! ID: $id")
}
```

## How It Works (Android)

Android has **deterministic attribution** via the Google Play Install Referrer API:

1. Creator shares a tracking link
2. User clicks → redirected to Play Store with `&referrer=af_click_id%3Dabc123`
3. Google stores the referrer server-side
4. User installs and opens the app
5. SDK reads the Install Referrer → extracts the exact click ID
6. 100% accurate match (when referrer is present)

Fallback fingerprint matching (IP + device signals) handles cases where the referrer is lost.

## Requirements

- Android 5.0+ (API 21)
- Google Play Services

## License

MIT
