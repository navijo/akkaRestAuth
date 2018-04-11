package es.indra.ijosa.test

import akka.actor.ActorSystem
import akka.event.Logging.LogLevel
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{Credentials, DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

trait Server {

  val validUserNameHidden = "niceOne"
  val pwdHidden = "motDePas"

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter

  def logRequestEntity(route: Route, level: LogLevel)
                      (implicit m: Materializer, ex: ExecutionContext): Route = {

    def requestEntityLoggingFunction(loggingAdapter: LoggingAdapter)(req: HttpRequest): Unit = {

      implicit val um: ActorMaterializer = ActorMaterializer()
      implicit val mat: Materializer = m

      val bodyAsString = Unmarshal(req.entity).to[String]

      bodyAsString.onComplete {
        case Success(body) =>
          val logMsg = s"$req\nRequest URL: ${req.uri.toString()}\nRequest body: $body"
          loggingAdapter.log(level, logMsg)
        case Failure(t) =>
          val logMsg = s"Failed to get the body for: $req"
          loggingAdapter.error(t, logMsg)
      }
    }

    DebuggingDirectives.logRequest(LoggingMagnet(requestEntityLoggingFunction))(route)
  }


  def logRequestGetEntity(route: Route, level: LogLevel)
                         (implicit m: Materializer, ex: ExecutionContext): Route = {

    def requestEntityLoggingFunction(loggingAdapter: LoggingAdapter)(req: HttpRequest): Unit = {

      implicit val um: ActorMaterializer = ActorMaterializer()
      implicit val mat: Materializer = m

      val bodyAsString = Unmarshal(req.entity).to[String]

      bodyAsString.onComplete {
        case Success(body) =>
          val logMsg = s"$req\nRequest params in the url: ${
            req.uri.rawQueryString.get.split("&").
              mkString("\t")
          }\nRequest body: $body"
          loggingAdapter.log(level, logMsg)
        case Failure(t) =>
          val logMsg = s"Failed to get the body for: $req"
          loggingAdapter.error(t, logMsg)
      }
    }

    DebuggingDirectives.logRequest(LoggingMagnet(requestEntityLoggingFunction))(route)
  }


  def logPostEntity(route: Route, level: LogLevel)
                   (implicit m: Materializer, ex: ExecutionContext): Route = {

    def requestEntityLoggingFunction(loggingAdapter: LoggingAdapter)(req: HttpRequest): Unit = {

      implicit val um: ActorMaterializer = ActorMaterializer()
      implicit val mat: Materializer = m

      val bodyAsString = Unmarshal(req.entity).to[String]

      bodyAsString.onComplete {
        case Success(body) =>
          val logMsg = s"$req\nRequest URL: ${req.uri}\nRequest body: $body"
          loggingAdapter.log(level, logMsg)
        case Failure(t) =>
          val logMsg = s"Failed to get the body for: $req"
          loggingAdapter.error(t, logMsg)
      }
    }

    DebuggingDirectives.logRequest(LoggingMagnet(requestEntityLoggingFunction))(route)
  }


  def userAuthenticate(credentials: Credentials): Option[String] = {
    credentials match {
      case p@Credentials.Provided(userName) if p.verify(pwdHidden) && userName.equals(validUserNameHidden) => Some(userName)
      case _ => None
    }
  }


  val unsecuredRoutes: Route = logRequestEntity({
    path("unsecure") {
      complete(s"Hello! This is a not secured route")
    }
  }, Logging.InfoLevel)


  val paramsRoutes: Route = logRequestGetEntity({
    path("unsecureWithParam") {
      complete(s"Hello param! This is a unsecured param route.")
    }
  }, Logging.InfoLevel)


  val postRoute: Route = logPostEntity({
    path("postMethod") {
      post {
        complete("This is a POST request.")
      }
    }
  }, Logging.InfoLevel)


  val securedRoutes: Route = logRequestEntity({
    path("secure") {
        authenticateBasic(realm = "secure site", userAuthenticate) {
          userName => complete(s"Hello $userName!  This is a secured route.")
        }
      }
    }, Logging.InfoLevel)


  val sealedRoute: Route =   Route.seal {
    logRequestEntity({
      path("secureSealed") {
        authenticateBasic(realm = "secure site", userAuthenticate) { userName =>
          complete(s"Hello $userName! This is a sealed and secured route.")
        }
      }
    }, Logging.InfoLevel)
  }

}
