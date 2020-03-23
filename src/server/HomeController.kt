
package server

import gust.backend.AppController
import gust.backend.PageContextManager
import gust.backend.PageRender
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.views.View
import org.slf4j.LoggerFactory
import javax.inject.Inject


/**
 * COVID Impact Map homepage controller - responsible for serving the homepage.
 */
@Controller
@Secured("isAnonymous()")
class HomeController @Inject constructor(ctx: PageContextManager): AppController(ctx) {
  companion object {
    // Logging pipe.
    @JvmStatic private val logging = LoggerFactory.getLogger(HomeController::class.java)

    // Default name to show.
    private const val defaultName = "World"
  }

  /**
   * `/` (`HTTP GET`): Handler for the root homepage for COVID Impact Map - i.e. `/`.
   */
  @View("covidmap.home.page")
  @Get("/", produces = ["text/html;charset=UTF-8"])
  fun home(@QueryValue("name", defaultValue = defaultName) name: String): HttpResponse<PageRender> {
    if (name != defaultName)
      logging.info("Greeting user with name '$name'...")
    if (logging.isDebugEnabled)
      logging.debug("Serving home page...")
    return this.serve(
      this.context
        .title("COVID Impact Map")
        .put("name", name)
        .stylesheet("covidmap.mdl")
        .stylesheet("covidmap.styles")
        .script("covidmap.main"))
  }
}
