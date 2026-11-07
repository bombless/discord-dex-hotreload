# Discord DEX Hot Reload

Android proof-of-concept for loading versioned DEX modules over an ADB-forwarded TCP connection. The host renders plugin-provided Jetpack Compose UI through a shared `HotScreen` contract.

## Stack

- Gradle Wrapper: **9.1.0**
- Android Gradle Plugin: 9.0.0
- Kotlin / Compose compiler plugin: 2.2.10
- minSdk 26 (InMemory DEX loading is not used; DexClassLoader is used)
- Host-owned Compose Runtime shared with dynamically loaded modules
- Debug-only loopback TCP HTTP server on port **47653**

## Build

Use JDK 17+ and Android SDK platform 36 / Build Tools 36.0.0.

```bash
sh ./gradlew :app:assembleDebug
sh ./gradlew :hotmodule:assembleDebug :hotmodule:makeDex
```

The sample DEX is written to `hotmodule/build/outputs/dex/classes.dex`.

## Install and forward

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb forward tcp:47653 tcp:47653
curl http://127.0.0.1:47653/health
```

## Upload a module

The entry class must implement `dev.bombless.dexhot.contract.HotScreen`. Module class names are restricted to `dev.bombless.*`.

```bash
curl -X POST \
  -H 'Content-Type: application/vnd.android.dex' \
  -H 'X-Entry-Class: dev.bombless.dexhot.sample.SampleHotScreen' \
  --data-binary @hotmodule/build/outputs/dex/classes.dex \
  http://127.0.0.1:47653/modules/sample-1
```

Other endpoints:

- `GET /health`
- `GET /versions`
- `POST /rollback` — activate the previously active version
- `POST /rollback/<version>` — activate a specific stored version

## Module authoring contract

Implement `HotScreen` in a separately compiled Android library. Its Compose Runtime, UI and Material3 dependencies must remain compatible with the host. Do not package duplicate Compose runtime classes inside the DEX. The plugin class loader delegates to the host class loader, so contract and Compose types are shared.

## Important limitations / security

- This is a development prototype, not a production update system.
- The server binds only to device loopback and is available through explicit ADB forwarding. It is disabled in non-debuggable builds.
- Uploaded DEX executes code inside the app process with the app's privileges. Never expose this port to an untrusted network or ship the update server in production.
- Version files are retained under app-private storage. The current implementation provides manual activation and previous-version rollback; it does not yet migrate arbitrary plugin state.
- A Compose plugin must be compiled with a compatible Kotlin/Compose compiler and the same Compose API surface as the host.
- The D8 task requires the configured Android SDK Build Tools. If your local SDK uses another version, run with `-PdexBuildTools=<installed-version>`.

## Wire format

HTTP/1.1 over TCP. Upload body is raw DEX bytes; set `Content-Length` and `X-Entry-Class`. Version IDs are URL path segments and restricted to letters, digits, dot, underscore and hyphen.

## License

Add a license before redistributing.
