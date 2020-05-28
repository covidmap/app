@file:Suppress("UnstableApiUsage")

package server

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture as Future
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.maxmind.db.Reader.FileMode
import com.maxmind.geoip2.DatabaseProvider
import com.maxmind.geoip2.DatabaseReader
import covidmap.schema.*
import gust.backend.runtime.Logging
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.Logger
import javax.inject.Singleton
import java.io.BufferedInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.min


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
      Executors.newScheduledThreadPool(3))

  /** Manages the MaxMind GeoIP database. */
  object MaxMindManager {
    /** Where we can find the MaxMind database. */
    const val dbname = "/maxmind.mmdb"

    /** Mode to open the MaxMind DB file in. */
    val mode = FileMode.MEMORY
  }

  companion object {
    /** Default result count limit on queries. */
    private const val defaultQueryLimit: Long = 100

    /** Maximum result count limit on queries. */
    private const val maxQueryLimit: Long = 10000

    /** Maximum offset for queries. */
    private const val maxOffsetLimit: Long = maxQueryLimit - 1

    /** Private logging pipe. */
    private val logging: Logger = Logging.logger(CovidmapLogic::class.java)

    /** Header containing an error message. */
    val errorMessageHeader: Metadata.Key<String> = (
      Metadata.Key.of("x-error-message", Metadata.ASCII_STRING_MARSHALLER))

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

  /** Retrieve the facilities manager. */
  fun facilities(): FacilitiesManager = facilities

  /** Validate a basic query for validity. */
  fun validateQuery(query: GenericQuery): Boolean {
    return (
      query.limit in 0..maxQueryLimit
      && query.offset in 0..maxOffsetLimit
    )
  }

  /** Validate a query for statistics. */
  fun validateQuery(query: StatsQuery): Boolean {
    if (query.address.isNotEmpty())  // addresses are not yet supported
      return false
    return validateQuery(query.query)
  }

  /** Validate a report before submission. */
  fun validateReport(email: String, report: Report): Boolean {
    return report.isInitialized
      && report.hasSource()
      && report.hasSurvey()
      && email.isNotEmpty()
      && email.isNotBlank()
  }

  /** Submit a task to the background executor. */
  private fun <T> task(op: () -> T): Future<T> {
    return executor.submit(op)
  }

  /** Handle RPC response after the provided future completes. */
  fun <T> respond(future: Future<T>, observer: StreamObserver<T>) {
    future.addListener(Runnable {
      try {
        if (future.isCancelled) {
          // throw a 404 if cancelled (we signal this from facility fetch)
          logging.warn("Failed to locate record: cancelling operation.")
          observer.onError(Status.NOT_FOUND.asException())
        } else {
          val value = future.get()
          if (value != null)
            observer.onNext(value)
          observer.onCompleted()
        }

      } catch (exc: Exception) {
        logging.error("An error occurred while processing an RPC operation: ${exc.message}")
        observer.onError(exc)
      }
    }, executor)
  }

  /** Supply facilities that match a query, from the stream, until the query is satisfied, or we run out of data. */
  private fun supplyUntilSatisfied(facilities: Stream<Facility>, query: GenericQuery): FacilityList {
    return FacilityList.newBuilder()
      .addAllFacility(facilities
        // apply limit
        .limit(min(if (query.limit > 0) {
          query.limit.toLong()
        } else {
          defaultQueryLimit
        }, maxQueryLimit))

        // apply offset
        .skip(min(if (query.offset > 0) {
          query.offset.toLong()
        } else {
          0
        }, maxOffsetLimit))
        .collect(Collectors.toList()))
      .build()
  }

  /** Fetch the specified facility and return it, if it can be found. */
  fun facilityFetch(key: Facility.FacilityKey): Future<Facility> {
    val facility = facilities.resolve(key.id)
    if (facility != null) {
      return Futures.immediateCancelledFuture()
    }
    return Futures.immediateFuture(facility)
  }

  /** Execute a query against the static facility dataset. */
  fun facilityQuery(query: GenericQuery): Future<FacilityList> {
    return task {
      if (query.hasNearby()) {
        supplyUntilSatisfied(
          facilities().nearby(query.nearby), query)
      } else {
        supplyUntilSatisfied(
          facilities().stream(), query)
      }
    }
  }

  /** Fetch stats for facilities nearby a given point. */
  fun facilityStats(query: StatsQuery): Future<FacilityStatsList> {
    return task {
      val facilities = facilityQuery(query.query)
      TODO("not yet implemented")
    }
  }

  /** Prepare a report and submit to the database. */
  fun prepareAndSubmitReport(key: Facility.FacilityKey, email: String, report: Report): Future<String> {
    return task {
      val reportID = UUID.randomUUID().toString().toUpperCase()
      logging.info("Delivering report at ID '$reportID' for email '$email'.")
      TODO("not yet implemented")
    }
  }
}
