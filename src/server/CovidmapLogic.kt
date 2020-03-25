
package server

import com.maxmind.db.Reader.FileMode
import com.maxmind.geoip2.DatabaseProvider
import com.maxmind.geoip2.DatabaseReader
import gust.backend.runtime.Logging
import org.slf4j.Logger
import javax.inject.Singleton
import java.io.BufferedInputStream
import java.io.IOException


/**
 * Defines application logic for the COVID Impact Map app. This logic is exposed via gRPC services (like [AppService]),
 * or, alternatively, made use of directly via dependency injection in server-side controllers (i.e. [HomeController]).
 *
 * In this manner, validation logic in the COVID Impact Map app is centralized here. Some aspects of the logic contained
 * herein are packaged separately to allow sharing that logic with the frontend, which must perform a subset of the
 * tasks incumbent on the server (for instance, basic validation of data before submission to the service, which is
 * performed in duplicate on the server-side).
 */
@Singleton
class CovidmapLogic {
  /** Manages the MaxMind GeoIP database. */
  object MaxMindManager {
    /** Where we can find the MaxMind database. */
    private const val dbname = "/maxmind.mmdb"

    /** Mode to open the MaxMind DB file in. */
    private val mode = FileMode.MEMORY

    /** Private logging pipe. */
    private val logging: Logger = Logging.logger(CovidmapLogic::class.java)

    @Throws(IOException::class)
    fun loadDatabase(): DatabaseProvider {
      BufferedInputStream(CovidmapLogic::class.java.getResourceAsStream(dbname)).use { buffer ->
        logging.info("Loading MaxMind database (mode: ${mode.name})...")
        return DatabaseReader.Builder(buffer)
          .locales(listOf("en-US"))
          .fileMode(mode)
          .build()
      }
    }
  }

  /** Loaded MaxMind database object. */
  private val maxmindDb: DatabaseProvider = MaxMindManager.loadDatabase()

  /** Retrieve the MaxMind database provider. */
  fun maxmind(): DatabaseProvider = maxmindDb
}
