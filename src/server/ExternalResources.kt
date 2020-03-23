package server

import core.AppConfig


/** Holds links to external resources. */
object ExternalResources {
  // Version of Firebase code to load up.
  const val firebaseVersion = "7.12.0"

  // Prefix to use for Firebase assets.
  const val firebasePrefix = "https://www.gstatic.com/firebasejs/"

  /** References for Google Maps. */
  object Maps {
    /** JS URL for Google Maps. */
    val js = "https://maps.googleapis.com/maps/api/js?callback=__init_map&key=${AppConfig.getApiKey()}"
  }

  /** References to Firebase JS. */
  object Firebase {
    const val app = "$firebasePrefix/$firebaseVersion/firebase-app.js"
    const val analytics = "$firebasePrefix/$firebaseVersion/firebase-analytics.js"
  }
}
