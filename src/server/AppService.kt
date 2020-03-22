
package server

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import io.micronaut.grpc.annotation.GrpcService
import covidmap.schema.AppGrpc
import javax.inject.Singleton


/**
 *
 */
@Singleton
@GrpcService
class AppService: AppGrpc.AppImplBase() {
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
}
