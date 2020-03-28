@file:Suppress("UnstableApiUsage")

package server

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.maxmind.db.Reader.FileMode
import com.maxmind.geoip2.DatabaseProvider
import com.maxmind.geoip2.DatabaseReader
import covidmap.schema.Report
import gust.backend.runtime.Logging
import org.slf4j.Logger
import javax.inject.Singleton
import java.io.BufferedInputStream
import java.io.IOException
import java.util.concurrent.Executors


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
  /** Executor for async tasks. */
  private val executor: ListeningScheduledExecutorService = MoreExecutors.listeningDecorator(
      Executors.newScheduledThreadPool(3));

  /** Manages the MaxMind GeoIP database. */
  object MaxMindManager {
    /** Where we can find the MaxMind database. */
    private const val dbname = "/maxmind.mmdb"

    /** Mode to open the MaxMind DB file in. */
    private val mode = FileMode.MEMORY

    /** Private logging pipe. */
    private val logging: Logger = Logging.logger(CovidmapLogic::class.java)

    /** Load the MaxMind GeoIP database during server startup. */
    @Throws(IOException::class)
    fun loadMaxmind(): DatabaseProvider {
      BufferedInputStream(CovidmapLogic::class.java.getResourceAsStream(dbname)).use { buffer ->
        logging.info("Loading MaxMind database (mode: ${mode.name})...")
        return DatabaseReader.Builder(buffer)
          .locales(listOf("en-US"))
          .fileMode(mode)
          .build()
      }
    }

    /** Supply an instance of the [FacilitiesManager], which loads static health facilitiy data. */
    @Throws(IOException::class)
    fun loadFacilities(): FacilitiesManager? {
      return null
    }
  }

  /** Loaded MaxMind database object. */
  private val maxmindDb: DatabaseProvider = MaxMindManager.loadMaxmind()

  /** Retrieve the MaxMind database provider. */
  fun maxmind(): DatabaseProvider = maxmindDb

  /** Validate a report before submission. */
  fun validateReport(email: String, report: Report): Boolean {
    return report.isInitialized
      && report.hasSource()
      && report.hasSurvey()
      && email.isNotEmpty()
      && email.isNotBlank()
  }

  /** Prepare a report and submit to the database. */
  fun prepareAndSubmitReport(report: Report) {

  }
}
