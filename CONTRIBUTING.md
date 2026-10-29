# Contributing

Thanks for contributing to Adless Streaming.

## Principles

- Keep UI, domain behavior, playback, and each provider adapter separated.
- Treat all provider responses as untrusted input; handle network errors without crashing the app.
- Preserve attribution, source links, and license metadata for every online track.
- Do not add scraping, private-cookie access, account-token collection, ads, analytics, or trackers.
- Do not add a source unless its public API path and rights for the content are documented in `docs/source-review.md`.
- Keep dependencies pinned and prefer AndroidX or platform libraries already in use.
- Add focused unit tests for queue behavior and pure response parsing.

## Local workflow

Use JDK 17, Gradle 8.13, and Android SDK 36. Run `gradle spotlessApply` to format, then `gradle testDebugUnitTest assembleDebug` before submitting a change. Keep changes small and describe user-visible behavior, source terms reviewed, and test results in the pull request.

## Credentials and privacy

Never commit API credentials, local properties, signing keys, user audio, or tokens. Jamendo's client ID belongs in ignored `local.properties` or an environment variable. This app does not need an end-user account.

