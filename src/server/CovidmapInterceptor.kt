
package server

import ch.hsr.geohash.GeoHash
import com.google.type.LatLng
import io.grpc.*
import java.net.InetAddress
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Intercepts server-side calls to the [AppService], and performs steps to authenticate the calls before allowing them
 * to proceed. If a given call does not meet the requirements for authentication/authorization, it is rejected with a
 * `403 Forbidden`/`PERMISSION_DENIED` status. If the call is missing authentication credentials entirely, it is
 * rejected with `403 Forbidden`/`AUTHORIZATION_REQUIRED`.
 *
 * Meeting the following requirements constitute valid authentication for use of the COVID Impact Map API:
 * - **API key.** The frontend application invoking the service call must affix an API key, which is valid and un-
 *   revoked, and passes any associated validation (for instance, referrer restrictions, for web app keys, and so on).
 *
 * - **Authorization token.** The frontend application invoking the service call must affix an `Authorization` header,
 *   which specifies a `Bearer` token containing a valid, un-expired, and un-revoked Firebase authorization JWT. To
 *   learn more about Firebase Auth, see [here](https://firebase.google.com/docs/auth). To learn more about *JSON Web
 *   Tokens*, head over to [jwt.io](https://jwt.io/).
 */
@Singleton
@Immutable
class CovidmapInterceptor @Inject constructor (private val logic: CovidmapLogic): ServerInterceptor {
  companion object {
    /** HTTP header indicating the client's true IP. */
    private const val clientIpHeader = "CF-Connecting-IP"

    /** HTTP header indicating the client's location. */
    private const val clientLatLong = "K9-GSLB-Client-Location"

    /** Header containing the user's IP. */
    val cfConnectingIpHeader: Metadata.Key<String> = (
      Metadata.Key.of(clientIpHeader, Metadata.ASCII_STRING_MARSHALLER))

    /** Header containing the edge machine's Lat/Lng (or the user's lat/lng if CloudFlare is inactive). */
    val k9LocationHeader: Metadata.Key<String> = (
      Metadata.Key.of(clientLatLong, Metadata.ASCII_STRING_MARSHALLER))

    /** Specifies location data for a given user. */
    val userLocationDataKey: Context.Key<UserLocation> = Context.key("clientLocation")
  }

  /** Describes user-location detection sniffing that occurs in the interceptor. */
  data class UserLocation(
    /** Geopoint auto-inferred by edge systems. */
    private val point: LatLng,

    /** Geohash associated with the specified IP or point. */
    private val hash: String)

  /**
   * Performs the interception of RPC traffic through the [AppService], enforces authentication requirements like API
   * key presence and validity, and loads the active user through any affixed `Authorization` header. If *any* of the
   * described steps fail, the request is rejected. How it is rejected is based on the circumstances, but generally the
   * HTTP status failure code is always `403`.
   *
   * If the interceptor is able to load authentication and authorization credentials are properly validate them, it then
   * prepares the loaded values for downstream use by attaching them to the active gRPC context (see [io.grpc.Context]).
   * Keys for injected context items are exposed statically on this class, so that downstream actors may easily
   * reference them.
   *
   * @param call Server-side call being intercepted in this interceptor invocation.
   * @param metadata Metadata for the call, which roughly equates to the request's HTTP headers.
   * @param handler Tip of the handler chain for this call, which we should pass the call to, in order to continue RPC
   *        processing. This invokes the service method, any associated logic, etc., and hopefully completes the call.
   * @return Listener, which wraps the provided server [call] and [metadata], and eventually dispatches [handler] if
   *         auth details are processed and applied successfully.
   */
  override fun <Request: Any, Response: Any> interceptCall(call: ServerCall<Request, Response>,
                                                           metadata: Metadata,
                                                           handler: ServerCallHandler<Request, Response>):
                                                                                          ServerCall.Listener<Request> {
    val coordinates: LatLng? = when {
      metadata.containsKey(cfConnectingIpHeader) -> {
        val ip: String? = metadata.get(cfConnectingIpHeader)
        if (ip != null) {
          val cityResolution = logic.maxmind().tryCity(InetAddress.getByName(ip))
          if (cityResolution.isPresent) {
            val geoip = cityResolution.get()

            LatLng.newBuilder()
              .setLatitude(geoip.location.latitude)
              .setLongitude(geoip.location.longitude)
              .build()
          } else {
            null  // can't resolve: unable to trace IP
          }
        } else {
          null  // can't resolve: no IP value (weird)
        }
      }

      metadata.containsKey(k9LocationHeader) -> {
        val headerValue = metadata.get(k9LocationHeader)
        if (headerValue != null) {
          val split = headerValue.split(",")
          if (split.size == 2) {
            val latitude = split[0].toDoubleOrNull()
            val longitude = split[1].toDoubleOrNull()
            if (latitude != null && longitude != null) {
              LatLng.newBuilder()
                .setLatitude(latitude)
                .setLongitude(longitude)
                .build()
            } else {
              null  // lat or long failed to decode
            }
          } else {
            null  // more or less than (lat, long) after split
          }
        } else {
          null  // no header value at all - metadata lied to us
        }
      }

      else -> null
    }
    if (coordinates != null) {
      val ctx = Context.current().withValue(userLocationDataKey,
        UserLocation(
          point = coordinates,
          hash = GeoHash.withCharacterPrecision(
            coordinates.latitude,
            coordinates.longitude,
            FacilitiesManager.geohashCharacterSize
          ).toBase32()
        )
      )

      val old = ctx.attach()
      val result = handler.startCall(call, metadata)
      ctx.detach(old)
      return result
    }
    return handler.startCall(call, metadata)
  }
}
