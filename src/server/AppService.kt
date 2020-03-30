
package server

import com.google.protobuf.Empty
import covidmap.schema.*
import gust.backend.runtime.Logging
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.grpc.annotation.GrpcService
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Server-side implementation of the gRPC-based COVID Impact Map API. This API supplies functionality w.r.t. report
 * submission, facility listings, and facility stats which may be overlaid on map UIs.
 */
@Singleton
@GrpcService
class AppService @Inject constructor (private val logic: CovidmapLogic): AppGrpc.AppImplBase() {
  companion object {
    /** Private logging pipe. */
    private val logging: Logger = Logging.logger(AppService::class.java)

    /** Header containing an error message. */
    private val errorMessageHeader = Metadata.Key.of("x-error-message", Metadata.ASCII_STRING_MARSHALLER)
  }

  /**
   * Implements the `Health` service method, which simply checks the health of the App API itself, and reports back
   * whether there are any known issues, or that the service is working as intended. Transient issues with database
   * connections and backend services may cause this endpoint to report bad health. This way, routing is transparent
   * around issues with deep-backend layers when operating COVID Impact Map in a multi-homed fashion.
   *
   * This method is unary, so we are expected to prepare one (and only one) response and then send it. If the service
   * is not working correctly, we yield an error and explain why (instead of a regular response).
   *
   * This method can generally be invoked via `HTTP GET` at the endpoint `/v1/health`.
   *
   * @param request Empty protocol message, indicating a request for system health.
   * @param observer Observer for responses or errors which should be relayed back to the invoking client.
   */
  override fun health(request: Empty, observer: StreamObserver<Empty>) {
    observer.onNext(Empty.getDefaultInstance())
    observer.onCompleted()
  }

  /**
   * Retrieve a list of healthcare facilities tracked by this system, potentially filtered by any query parameters
   * present in [request]. As facilities are found to match the supplied query, they are gathered, and then sent back to
   * the client.
   *
   * If the client opts-in to payload data, it will be enclosed for each facility. If the client opts-out of payload
   * data, only a key is provided for each facility.
   *
   * @param request Request to query the facility list, potentially with filter parameters.
   * @param observer Observer for a materialized facility list response.
   */
  override fun facilities(request: GenericQuery, observer: StreamObserver<FacilityList>) {
    if (logic.validateQuery(request)) {
      logic.respond(logic.facilityQuery(request), observer)
    } else {
      logging.error("Rejecting facilities query: invalid.")
      val errMetadata = Metadata()
      errMetadata.put(errorMessageHeader, "Invalid query.")
      observer.onError(Status.INVALID_ARGUMENT.asRuntimeException())
    }
  }

  /**
   * Produce a set of stats, for each facility that is relevant within a given area. If no relevant area is passed into
   * the query to use as a geo-boundary, one is calculated by geo-locating the client's remote IP address.
   *
   * Once a relevance boundary is established, facilities are queried within that boundary, and a set of
   * (pre-calculated) stats are read back to the client for each facility within that region that additionally matches
   * or otherwise satisfies the other query parameters provided in the request.
   *
   * @param request Request for a stats list, potentially with query or filter parameters.
   * @param observer Observer for a materialized facility stats response.
   */
  override fun stats(request: StatsQuery, observer: StreamObserver<FacilityStatsList>) {
    if (logic.validateQuery(request)) {
      logic.respond(logic.facilityStats(request), observer)
    } else {
      logging.error("Rejecting stats query: invalid.")
      val errMetadata = Metadata()
      errMetadata.put(errorMessageHeader, "Invalid query.")
      observer.onError(Status.INVALID_ARGUMENT.asRuntimeException())
    }
  }

  /**
   * Accepts submission of reports for the COVID Impact Map project. Each report should specify the email address for
   * the user submitting the report, along with answers to survey questions they have provided in the form interface.
   *
   * If the submission doesn't validate, a `4xx`-series HTTP response is returned. If the submission validates fine, the
   * report is encoded as needed and stored in underlying (1) object storage, placed underneath the facility it is
   * reporting information for, and (2) analytics storage, as a row representing an individual report.
   *
   * @param request Request to submit a report for the COVID Impact Map application.
   * @param observer Observer for a response indicating an accepted report.
   */
  override fun report(request: ReportSubmission, observer: StreamObserver<Empty>) {
    if (logic.validateReport(request.email, request.report)) {

    } else {
      logging.error("Invalid report. Rejecting.")
      val errMetadata = Metadata()
      errMetadata.put(errorMessageHeader, "Invalid query.")
      observer.onError(Status.INVALID_ARGUMENT.asRuntimeException())
    }
  }
}
