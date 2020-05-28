
package server

import com.google.template.soy.data.SanitizedContent
import com.google.template.soy.data.ordainers.GsonOrdainer
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import core.AppConfig
import gust.backend.AppController
import gust.backend.PageContextManager
import gust.backend.PageRender
import gust.backend.runtime.Logging
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.views.View
import org.slf4j.Logger
import tools.elide.page.Context
import java.lang.IllegalStateException
import java.net.InetAddress
import java.net.URI
import javax.inject.Inject


/** COVID Impact Map homepage controller - responsible for serving the homepage. */
@Controller
@Secured("isAnonymous()")
class HomeController @Inject constructor (
  private val logic: CovidmapLogic,
  ctx: PageContextManager): AppController(ctx) {
  companion object {
    /** Debug flag. Surfaces exceptions in the raw. */
    private const val debug = false

    /** HTTP header indicating the client's true IP. */
    private const val clientIpHeader = "CF-Connecting-IP"

    /** HTTP header indicating the client's location. */
    private const val clientLatLong = "K9-GSLB-Client-Location"

    /** Private logging pipe. */
    private val logging: Logger = Logging.logger(HomeController::class.java)
  }

  /**
   * Estimate the client's location based on their IP address. We resolve the user's IP via the `True-Client-IP`
   * header we get from CloudFlare. If we can't find a header noting their IP, we fallback to the connection.
   *
   * @param request Request to read the IP address from, either from CF's header or the connection itself.
   */
  private fun estimateClientLocation(request: HttpRequest<*>): SanitizedContent {
    @Suppress("ConstantConditionIf") try {
      val ip: InetAddress = when {
        // firstly, look for the `True-Client-IP` header
        request.headers.contains(clientIpHeader) -> {
          logging.trace("Found `$clientIpHeader` header.")
          InetAddress.getByName(request.headers.get(clientIpHeader)
            ?: throw IllegalStateException("should not happen"))

        }

        // then, fallback to `K9-GSLB-*` headers (edge network)
        request.headers.contains(clientLatLong) -> {
          logging.trace("Found `$clientLatLong` header. Parsing lat/long...")

          // fallback to client-lat-long
          val clientLatLong = request.headers.get(clientLatLong)
            ?: throw IllegalStateException("should not happen")
          val parts = clientLatLong.split(",")
          if (parts.size == 2) {
            val latitude = parts.first().toDoubleOrNull()
            val longitude = parts[1].toDoubleOrNull()
            if (latitude != null && longitude != null) {
              return GsonOrdainer.serializeObject(EstimatedLocation(
                latitude = latitude,
                longitude = longitude,
                city = null,
                countryCode = null,
                countryLabel = null,
                continent = null,
                accuracy = null,
                confidence = null,
                metro = null,
                tz = null))
            }
          }

          logging.debug("Failed to parse client lat long Falling back to remote address.")
          request.remoteAddress.address
        }

        // then, fallback to connection remote address
        else -> {
          logging.debug("No `$clientIpHeader` header, falling back to remote address.")
          request.remoteAddress.address
        }
      }

      if (ip.hostAddress == "127.0.0.1" ||
          ip.toString() == "/0:0:0:0:0:0:0:1" ||
          ip.hostAddress.startsWith("10.") ||
          ip.hostAddress.startsWith("192.168.") ||
          ip.hostAddress.startsWith("172.16")) {
        // simulate location instead
        logging.info("Simulating AT&T park for client location.")
        return GsonOrdainer.serializeObject(EstimatedLocation(
          latitude = 37.780727,
          longitude = -122.38876,
          city = "San Francisco",
          countryCode = "US",
          countryLabel = "United States",
          continent = "NAM",
          accuracy = 55,
          confidence = 80,
          metro = 123,
          tz = "UTC+8"))
      }

      // we have an ip, resolve it
      logging.debug("Resolved user IP as '$ip'. Looking up in MaxMind...")

      val cityResolution = logic.maxmind().tryCity(ip)

      // serialize the info and return
      return GsonOrdainer.serializeObject((if (!cityResolution.isPresent) {
        logging.debug("City-level estimate was not available. Falling back to country.")
        val countryResolution = logic.maxmind().tryCountry(ip)
        if (!countryResolution.isPresent) {
          logging.warn("Country-level location estimate was not available.")
          null
        } else {
          logging.trace("Geo-located IP at COUNTRY resolution.")

          // interpret a country-resolution response
          EstimatedLocation.from(countryResolution.get())
        }
      } else {
        logging.trace("Geo-located IP at CITY resolution.")

        // interpret a city-resolution response
        EstimatedLocation.from(cityResolution.get())
      }) ?: emptyMap<String, Any>())

    } catch (err: Exception) {
      logging.error("Error while loading client IP: '$err'.")
      if (debug) throw err
      return GsonOrdainer.serializeObject(emptyMap<String, Any>())
    }
  }

  /** Data object that carries an estimate of the client user's location. */
  data class EstimatedLocation(
    /** Estimated client latitude. */
    private val latitude: Double?,

    /** Estimated client longitude. */
    private val longitude: Double?,

    /** Estimated client city. */
    private val city: String?,

    /** Resolved country value for the user. */
    private val countryCode: String?,

    /** Label for a given country. */
    private val countryLabel: String?,

    /** Estimated client continent. */
    private val continent: String?,

    /** Estimated accuracy radius. */
    private val accuracy: Int?,

    /** Confidence rating for city selection. */
    private val confidence: Int?,

    /** "Metro code" identifying this area. */
    private val metro: Int?,

    /** Timezone setting to apply. */
    private val tz: String?) {
    companion object {
      /** Generate an [EstimatedLocation] structure from a [CountryResponse]. */
      fun from(country: CountryResponse): EstimatedLocation =
        EstimatedLocation(
          null,
          null,
          null,
          country.country.isoCode,
          country.country.name,
          country.continent.code,
          null,
          country.country.confidence,
          null,
          null)

      /** Generate an [EstimatedLocation] structure from a [CityResponse]. */
      fun from(city: CityResponse): EstimatedLocation =
        EstimatedLocation(
           city.location.latitude,
           city.location.longitude,
           city.city.name,
           city.country.isoCode,
           city.country.name,
           city.continent.code,
           city.location.accuracyRadius,
           city.city.confidence,
           city.location.metroCode,
           city.location.timeZone)
    }
  }

  /** `/` (`HTTP GET`): Handler for the root homepage for COVID Impact Map - i.e. `/`. */
  @View("covidmap.home.page")
  @Get("/", produces = ["text/html;charset=UTF-8"])
  fun home(request: HttpRequest<*>): MutableHttpResponse<PageRender> {
    return this.serve(
      this.context
        .title("COVID Impact Map")
        .put("liftedJS", estimateClientLocation(request))
        .put("appContainer", AppConfig.getAppContainerId())
        .stylesheet("covidmap.mdl")
        .stylesheet("covidmap.skin")
        .stylesheet("covidmap.styles")
        .script(Context.Scripts.JavaScript.newBuilder()
          .setDefer(true)
          .setUri(this.trustedResource(URI.create(ExternalResources.Firebase.app))))
        .script(Context.Scripts.JavaScript.newBuilder()
          .setDefer(true)
          .setUri(this.trustedResource(URI.create(ExternalResources.Firebase.analytics))))
        .script("covidmap.ui")
        .script("covidmap.main")
        .script(Context.Scripts.JavaScript.newBuilder()
          .setDefer(true)
          .setUri(this.trustedResource(URI.create(ExternalResources.Maps.js)))))
  }
}
