package es.indra.ijosa.test

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor

object Main extends App with Server {


  override implicit val system: ActorSystem = ActorSystem()
  override implicit val executor: ExecutionContextExecutor = system.dispatcher
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  val routes = securedRoutes ~ unsecuredRoutes ~ paramsRoutes ~ postRoute ~ sealedRoute
  Http().bindAndHandle(routes , config.getString("http.interface"), config.getInt("http.port"))
}
