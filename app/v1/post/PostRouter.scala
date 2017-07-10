package v1.post

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the PostResource controller.
  */
class PostRouter @Inject()(controller: PostController) extends SimpleRouter {

  override def routes: Routes = {
    case POST(p"/") =>
      controller.process
    case GET(p"/") =>
      controller.index
  }

}
