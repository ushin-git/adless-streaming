package app.adless.streaming.data.source

import app.adless.streaming.data.HttpJson
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderParsingTest {
  @Test
  fun jamendoParserKeepsCreatorSourceAndLicense() {
    val track =
      parseJamendoTrack(
        JSONObject(
          """
            {
              "id": "42",
              "name": "Quiet Morning",
              "artist": { "name": "The Artist" },
              "album": { "name": "First Light" },
              "shareurl": "https://www.jamendo.com/track/42",
              "license_ccurl": "https://creativecommons.org/licenses/by/4.0/"
            }
        """
        )
      )

    assertNotNull(track)
    assertEquals("Quiet Morning", track?.title)
    assertEquals("The Artist", track?.artist)
    assertEquals("https://www.jamendo.com/track/42", track?.sourcePageUrl)
    assertEquals("https://creativecommons.org/licenses/by/4.0/", track?.licenseUrl)
  }

  @Test
  fun audiusParserRetainsTrackLinkAndDropsUnsupportedAlternativeLicense() {
    val allowed =
      parseAudiusTrack(
        JSONObject(
          """
            {
              "id": "abc",
              "title": "Open Sky",
              "user": { "name": "A Creator" },
              "permalink": "/a-creator/open-sky"
            }
        """
        )
      )
    val restricted =
      parseAudiusTrack(
        JSONObject(
          """
            {
              "id": "private-license",
              "title": "Restricted",
              "license_url": "https://creativecommons.org/licenses/by-nc/4.0/"
            }
        """
        )
      )
    val reserved =
      parseAudiusTrack(
        JSONObject(
          """
            {
              "id": "reserved",
              "title": "Reserved",
              "license": "All Rights Reserved"
            }
        """
        )
      )
    val gated =
      parseAudiusTrack(
        JSONObject(
          """
            {"id":"gated","title":"Gated","is_stream_gated":true}
        """
        )
      )
    val unsupported =
      parseAudiusTrack(
        JSONObject(
          """
            {"id":"unknown","title":"Unknown","license":"Some custom license"}
        """
        )
      )

    assertNotNull(allowed)
    assertEquals("https://audius.co/a-creator/open-sky", allowed?.sourcePageUrl)
    assertEquals("https://audius.org/open-music-license.pdf", allowed?.licenseUrl)
    assertNull(restricted)
    assertNull(reserved)
    assertNull(gated)
    assertNull(unsupported)
  }

  @Test
  fun compatibleOpenLicensesPassRightsFilter() {
    assertEquals(true, HttpJson.licenseAllowed("https://creativecommons.org/licenses/by/4.0/"))
    assertEquals(true, HttpJson.licenseAllowed("https://creativecommons.org/licenses/by-sa/4.0/"))
    assertEquals(false, HttpJson.licenseAllowed("https://creativecommons.org/licenses/by-nc/4.0/"))
    assertEquals(
      false,
      HttpJson.licenseAllowed("https://evil.example/creativecommons.org/licenses/by/4.0/"),
    )
    assertEquals(
      false,
      HttpJson.licenseAllowed("https://creativecommons.org.evil.example/licenses/by/4.0/"),
    )
  }
}
