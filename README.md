# Appfiliate Android SDK

Creator attribution SDK for mobile app affiliate marketing. Track which creators, influencers, and campaigns drive installs and revenue for your Android app. Deterministic attribution via Google Install Referrer with fingerprint fallback.

**[Website](https://appfiliate.io)** | **[Documentation](https://docs.appfiliate.io)** | **[Blog](https://appfiliate.io/blog)** | **[Sign Up Free](https://app.appfiliate.io/signup)**

## Features

- 3-line integration — configure, track, done
- Deterministic attribution via Google Play Install Referrer
- No GAID/AAID required, no special permissions
- Per-creator install and revenue attribution
- Built-in creator dashboards
- Webhook integrations with RevenueCat, Superwall, Adapty, Qonversion, and Stripe
- Single dependency (Install Referrer)

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

Three lines of code. In your `Application.onCreate()` or main `Activity.onCreate()`:

```kotlin
import com.appfiliate.sdk.Appfiliate

Appfiliate.configure(this, appId = "APP_ID", apiKey = "API_KEY")
Appfiliate.trackInstall(this)
Appfiliate.setUserId(this, Purchases.sharedInstance.appUserID) // optional — for webhook integrations
```

Get your `appId` and `apiKey` from the [Appfiliate dashboard](https://app.appfiliate.io).

## Track Purchases

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

Or use [webhook integrations](https://docs.appfiliate.io) with RevenueCat, Superwall, Adapty, Qonversion, or Stripe for automatic purchase tracking.

## Check Attribution

```kotlin
val attributed = Appfiliate.isAttributed(context)
val id = Appfiliate.attributionId(context)
```

## How It Works

1. On first launch, the SDK reads the Google Play Install Referrer (deterministic) and collects device signals
2. Signals are sent to the Appfiliate attribution API
3. The API matches the install to a tracking link click
4. The result is cached locally — `trackInstall()` only fires once per install
5. Purchases are linked to the attribution via `trackPurchase()` or webhooks

Learn more: [How mobile attribution works without IDFA](https://appfiliate.io/blog/mobile-app-install-attribution-without-idfa)

## Requirements

- Android API 21+ (Android 5.0)
- Internet permission (added automatically by the SDK manifest)

## Resources

- [Getting started guide](https://docs.appfiliate.io/quick-start)
- [How to set up an affiliate program for your app](https://appfiliate.io/blog/how-to-set-up-affiliate-program-mobile-app)
- [Appfiliate vs AppsFlyer vs Branch](https://appfiliate.io/blog/appfiliate-vs-appsflyer-vs-branch)
- [What is a creator attribution SDK?](https://appfiliate.io/blog/creator-attribution-sdk)

## License

MIT
