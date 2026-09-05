# Personal Notificator

A stubborn Android application that will strive to receive real-time notifications via websockets and play them as text-to-speech alerts, maintaining a local history of events.

## Motivation

The main motivation was to have an app that alerts me whenever something important happens in my home automation.
I needed to have an alert on mobile device that would inform me what happened without me looking at the screen. So natural idea was to read notifications aloud.
I could not find any app out of the box doing this, so I created my own. 


## Key features

- receives and reads aloud notifications within 2 - 3 seconds after event physically happened; this is especially difficult to achieve, as Android tries to kill such functions due to power-saving capabilities; this app uses various mechanisms to deliver notification in unstoppable manner
- notifications arrive through WebSocket connection for emitter; app attaches FCM token to backend request for authentication 
- has own configuration of TTS volume and can selectively disable TTS for given notification types
- has persistent local history to quickly check history of events
- has live WebSocket connection status indicator so you see your backend infrastructure pinging your phone assuring everything works fine
- auto-reconnects to backend ASAP - works surprisingly well with low quality mobile networks 
- auto-start on device boot
- shielded from doze mode and other Android tricks 


## Backend

The app talks to a self-hosted backend (production URL stored in secrets, not in the repository):

| Endpoint | Purpose |
|----------|---------|
| `POST /api/register` | Register FCM token + Firebase ID token |
| `WS /ws/notifications` | Persistent notification stream |

Authentication uses Firebase ID tokens (JWT) sent as `Authorization: Bearer <token>` and the FCM token in the `X-FCM-Token` header.

A local development mode is available — switch via the UI.

## Building

### Required secret files

| File | Purpose |
|------|---------|
| `google-services.json` | Firebase project configuration |
| `app/src/main/assets/config.json` | Backend REST and WebSocket URLs |
| Keystore `.jks` | APK signing key |

All secrets are managed through a single properties file referenced by Gradle. Point Gradle to it via `gradle.properties` or the command line:

```properties
# gradle.properties (local, not committed)
secretsFile=C:/work/secrets/personalnotificator/pn.properties
```

The properties file must define:

```properties
GOOGLE_SERVICES_FILE=<path>/google-services.json
CONFIG_FILE=<path>/config.json
KEYSTORE_PATH=<path>/personalnotificator.jks
KEYSTORE_PASS=...
KEY_ALIAS=...
KEY_PASS=...
```

Gradle tasks `copyGoogleServices` and `copyConfig` automatically copy the files to their expected locations before each build — no manual copying needed.

### config.json format

```json
{
  "restApiUrl": "https://<your-backend>/",
  "wsApiUrl": "wss://<your-backend>/"
}
```

### Build

```bash
./gradlew assembleDebug
# or with explicit secrets path:
./gradlew assembleDebug -PsecretsFile=C:/work/secrets/personalnotificator/pn.properties
```

Version name is derived automatically from git: `1.0-<short-hash>`, version code from commit count.

## TTS language

The TTS locale defaults to Polish and is configured in a single constant in `TtsPlayer.kt`:

```kotlin
// TtsPlayer.kt — companion object
val LOCALE: Locale = Locale("pl", "PL")  // default
```

Change that one line to switch language. Common examples:



The locale is applied on TTS engine initialization, so a restart of `NotificationForegroundService` is required after changing it (happens automatically on next app start).

## Permissions

| Permission | Why |
|-----------|-----|
| `INTERNET` | WebSocket and REST API |
| `FOREGROUND_SERVICE` + type flags | Background service continuity |
| `POST_NOTIFICATIONS` | System notifications (runtime, Android 13+) |
| `WAKE_LOCK` | Keep CPU awake during WebSocket reads |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent service from being killed |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on reboot |
| `SCHEDULE_EXACT_ALARM` | Precision service-restart scheduling |

## License

This project is licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE) for details.

For commercial licensing inquiries, contact: kmalysiak.pl@gmail.com
