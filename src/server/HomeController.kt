
package server

import gust.backend.AppController
import gust.backend.PageContextManager
import gust.backend.PageRender
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.views.View
import tools.elide.page.Context
import java.net.URI
import javax.inject.Inject


/**
 * COVID Impact Map homepage controller - responsible for serving the homepage.
 */
@Controller
@Secured("isAnonymous()")
class HomeController @Inject constructor(ctx: PageContextManager): AppController(ctx) {
  /** `/` (`HTTP GET`): Handler for the root homepage for COVID Impact Map - i.e. `/`. */
  @View("covidmap.home.page")
  @Get("/", produces = ["text/html;charset=UTF-8"])
  fun home(): MutableHttpResponse<PageRender> {
    return this.serve(
      this.context
        .title("COVID Impact Map")
        .stylesheet("covidmap.mdl")
        .stylesheet("covidmap.skin")
        .stylesheet("covidmap.styles")
        .script(Context.Scripts.JavaScript.newBuilder()
          .setUri(this.trustedResource(URI.create(ExternalResources.Firebase.app))))
        .script(Context.Scripts.JavaScript.newBuilder()
          .setUri(this.trustedResource(URI.create(ExternalResources.Firebase.analytics))))
        .script("covidmap.ui")
        .script("covidmap.main"))
  }
}
