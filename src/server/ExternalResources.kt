package server


/** Holds links to external resources. */
object ExternalResources {
  // Version of Firebase code to load up.
  const val firebaseVersion = "7.12.0"

  // Prefix to use for Firebase assets.
  const val firebasePrefix = "https://www.gstatic.com/firebasejs/"

  /** References to Firebase JS. */
  object Firebase {
    const val app = "$firebasePrefix/$firebaseVersion/firebase-app.js"
    const val analytics = "$firebasePrefix/$firebaseVersion/firebase-analytics.js"
  }
}
