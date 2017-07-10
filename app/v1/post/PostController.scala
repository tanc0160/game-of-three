package v1.post

import javax.inject.Inject
import play.api.Logger
import play.api.data.Form
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import akka.actor.ActorSystem
import play.api.libs.ws._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.Configuration
case class PostFormInput(number: String)

/**
  * Takes HTTP requests and produces JSON.
  */
class PostController @Inject()(cc: PostControllerComponents, actorSystem: ActorSystem,
                               ws: WSClient, config: Configuration)
                              (implicit ec: ExecutionContext)
    extends PostBaseController(cc) {

  private val logger = Logger(getClass)

  private val form: Form[PostFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "number" -> nonEmptyText
      )(PostFormInput.apply)(PostFormInput.unapply)
    )
  }

  implicit val implicitWrites = new Writes[PostFormInput] {
    def writes(post: PostFormInput): JsValue = {
      Json.obj(
        "number" -> post.number
      )
    }
  }

  def process: Action[AnyContent] = PostAction.async { implicit request =>
    logger.trace("process: ")
    processJsonPost()
  }

  private def addValue(number: Int, increment: Int): Int = number + increment

  private def divisibleByThree(number: Int): Boolean = number % 3 == 0

  private def processJsonPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
    def failure(badForm: Form[PostFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: PostFormInput) = {
      val receivingNum = Integer.parseInt(input.number)
      val number =
        List(-1, 0 , 1).map(i => addValue(receivingNum, i))
          .filter(divisibleByThree).head / 3
      println(s"Result: $number, added: ${number * 3 - receivingNum}, " +
        s"receiving: $receivingNum")
      if(number == 1) {
        println("Win the game")
      } else {
        sendToPeer(number)
      }
      Future(Created(Json.toJson(input)))
    }

    form.bindFromRequest().fold(failure, success)
  }

  private def sendToPeer(number: Int) = {
    actorSystem.scheduler.scheduleOnce(delay = 1.seconds) {
      // the block of code that will be executed
      val data = Json.obj(
        "number" -> number.toString
      )
      val urlString: String = config.get[String]("url")
      ws.url(urlString).post(data)
    }
  }

  def index: Action[AnyContent] = PostAction.async { implicit request =>
    val randomStartInput = scala.util.Random.nextInt(100)
    sendToPeer(randomStartInput)
    Future(Ok(Json.toJson(PostFormInput(randomStartInput.toString))))
  }
}
