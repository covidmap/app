@file:Suppress("UnstableApiUsage")

package server

import com.google.common.util.concurrent.ListenableFuture as Future
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.maxmind.db.Reader.FileMode
import com.maxmind.geoip2.DatabaseProvider
import com.maxmind.geoip2.DatabaseReader
import covidmap.schema.*
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
    const val dbname = "/maxmind.mmdb"

    /** Mode to open the MaxMind DB file in. */
    val mode = FileMode.MEMORY
  }

  companion object {
    /** Private logging pipe. */
    private val logging: Logger = Logging.logger(CovidmapLogic::class.java)

    /** Load the MaxMind GeoIP database during server startup. */
    @Throws(IOException::class)
    fun loadMaxmind(): DatabaseProvider {
      BufferedInputStream(CovidmapLogic::class.java.getResourceAsStream(MaxMindManager.dbname)).use { buffer ->
        logging.info("Loading MaxMind database (mode: ${MaxMindManager.mode.name})...")
        return DatabaseReader.Builder(buffer)
          .locales(listOf("en-US"))
          .fileMode(MaxMindManager.mode)
          .build()
      }
    }

    /** Supply an instance of the [FacilitiesManager], which loads static health facility data. */
    @Throws(IOException::class)
    fun loadFacilities(): FacilitiesManager = FacilitiesManager.load()
  }

  /** Loaded MaxMind database object. */
  private val maxmindDb: DatabaseProvider = loadMaxmind()

  /** Loaded facilities data and manager. */
  private val facilities: FacilitiesManager = loadFacilities()

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

  /** Execute a query against the static facility dataset. */
  fun facilityQuery(query: GenericQuery?): Future<FacilityList> {
    TODO("not yet implemented")
  }

  /** Fetch stats for facilities nearby a given point. */
  fun facilityStats(query: StatsQuery): Future<FacilityStatsList> {
    TODO("not yet implemented")
  }

  /** Prepare a report and submit to the database. */
  fun prepareAndSubmitReport(report: Report): Future<String> {
    TODO("not yet implemented")
  }
}
