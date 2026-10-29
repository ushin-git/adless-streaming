# Music source review

Reviewed 2026-10-29. “No login” below means an end user can discover and play without creating an account at the music service; developer app credentials may still be necessary. Every provider is isolated behind `MusicSource` and can be disabled. Review upstream terms again before release.

## Sources implemented in the prototype

| Source | Access | Rights / terms gates | Login |
|---|---|---|---|
| Local audio | Android MediaStore after audio permission | The user is responsible for having playback rights. Files stay on device; no upload. | None |
| Jamendo | Official REST API and official stream endpoint | Only tracks with an explicitly recognized compatible license are shown. Display artist, title, source link, and license. Jamendo's API terms limit API use to noncommercial applications, disallow designing the app for offline access, and limit cache to what playback reasonably needs; Jamendo downloads stay disabled. The developer must register an app and supply a client ID. | No end-user Jamendo login; developer registration/client ID required |
| Audius | Official read-only REST API and track stream endpoint | Audius Open Music License grants streaming rights to music-player apps by default, but artists can set an alternative license. The adapter accepts the default OML and recognized CC BY / BY-SA tracks, excludes unsupported alternatives, unlisted or gated tracks, and links the creator and license. | Most read endpoints require no credentials; an API key raises rate limits |
| Internet Archive | Public advanced search + public item metadata/file endpoints | Archive disclaims guarantees about uploader-provided rights. Show only audio items with an explicit recognized CC BY, CC BY-SA, CC0, or public-domain license URL; show rights/source link and attribution. Exclude unknown/ambiguous rights. No offline downloads by default until each item’s license permits it. | No end-user login for public read endpoints |
| Wikimedia Commons audio | Public MediaWiki API using a descriptive User-Agent | Use per-file license metadata, show the author, license, file page, and required attribution. Rate-limit requests and exclude files without a clear free license. This catalog is broad but is not a mainstream commercial-music catalog. | None for public search/read |

## No-login candidates excluded from the implemented adapter set

- **YouTube / YouTube Music:** Echo uses undocumented endpoints and extraction. YouTube terms restrict automated access and copying; official API policies prohibit undocumented APIs, scraping, and offline copies. Do not implement a scraper or stream extractor.
- **Free Music Archive:** site terms say content can be streamed, but prohibit direct links/deep links to hosted audio files. No supported app integration with a clear authorization path was verified, so do not fetch/play raw FMA URLs from the app. A user can open the official site separately.
- **Free To Use:** it advertises unauthenticated API access, but current licensing excludes apps/games without a paid commercial or single-track license. Not a safe free integration.
- **Deezer / Spotify / SoundCloud / Bandcamp:** public metadata or web playback is not blanket permission for full-track streams from a third-party app. No unauthorized stream extraction, private cookie, or account-session source will be added.

## References

- Jamendo auth and developer client ID: https://developer.jamendo.com/v3.0/authentication
- Jamendo API terms: https://devportal.jamendo.com/api_terms_of_use
- Jamendo stream/download permission flag: https://developer.jamendo.com/v3.0/tracks/file
- Audius read access and app capabilities: https://docs.audius.co/api/
- Audius Open Music License: https://audius.org/open-music-license.pdf
- Internet Archive item metadata API: https://doc-tools.readthedocs.io/en/ia-test-gsod/metadata.html
- Internet Archive rights guidance: https://archivesupport.zendesk.com/hc/en-us/articles/360014759692-Rights
- Wikimedia API access / licensing: https://www.mediawiki.org/wiki/Wikimedia_APIs/Access_policy
- Wikimedia API use limits: https://foundation.wikimedia.org/wiki/Policy:Wikimedia_Foundation_API_Usage_Guidelines
- YouTube Terms: https://www.youtube.com/t/terms
- YouTube API Services policies: https://developers.google.com/youtube/terms/developer-policies
- Free Music Archive terms: https://freemusicarchive.org/terms_of_use
- Free To Use API and license scope: https://freetouse.com/api and https://freetouse.com/faq

These are source-integration notes, not legal advice. Only implement each provider within the limits and the specific rights granted for the requested track.

