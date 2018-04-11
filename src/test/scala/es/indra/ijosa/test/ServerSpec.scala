package es.indra.ijosa.test

import akka.event.NoLogging
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.Config
import org.scalatest.{FlatSpec, Matchers}

class ServerSpec extends FlatSpec with Matchers with ScalatestRouteTest with Server {

  val validUserName = "niceOne"
  val pwd = "motDePas"
  val badPwd = "contraseña"


  override def testConfigSource = "akka.loglevel = DEBUG"
  override def config: Config = testConfig

  override val logger: NoLogging.type = NoLogging

  val userCredentials = BasicHttpCredentials(validUserName, pwd)


  "Secured Sealed service test" should "return OK" in {
    Get(s"/secureSealed","Request body of a get method?") ~> addCredentials(userCredentials) ~> sealedRoute ~> check {
      responseAs[String] shouldEqual s"Hello $validUserName! This is a sealed and secured route."
      println(responseAs[String])
    }
  }

  "Secured Sealed service test" should " fail" in {
    val badPasswordCredentials = BasicHttpCredentials(validUserName, badPwd)
    Get(s"/secureSealed","Request body of a get method?") ~> addCredentials(badPasswordCredentials) ~> sealedRoute ~> check {
      status shouldEqual StatusCodes.Unauthorized
      responseAs[String] shouldEqual "The supplied authentication is invalid"
      println(responseAs[String])
    }
  }

  "Secured service test" should "return OK" in {
    Get(s"/secure","Request body of a get method?") ~> addCredentials(userCredentials) ~> securedRoutes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      responseAs[String] shouldBe s"Hello $validUserName!  This is a secured route."
      println(responseAs[String])
    }
  }

  "Secured service test bad password" should "reject" in {
    val badPasswordCredentials = BasicHttpCredentials(validUserName, badPwd)
    Get(s"/secure","Request body of a get method?") ~> addCredentials(badPasswordCredentials) ~> securedRoutes ~> check {
      rejections should contain(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Basic", "secure site"
        ,Map("charset" → "UTF-8"))))
      println(rejections)
    }
  }

  "Secured service test bad userName" should " reject" in {
    val badUserCredentials = BasicHttpCredentials("badUser", pwd)
    Get(s"/secure","Request body of a get method?") ~> addCredentials(badUserCredentials) ~> securedRoutes ~> check {
      rejections should contain(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Basic", "secure site"
        ,Map("charset" → "UTF-8"))))
      println(rejections)
    }
  }



  "Not Secured service test" should "return OK" in {
    Get(s"/unsecure","Request body of a get method?") ~> unsecuredRoutes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      responseAs[String] shouldBe s"Hello! This is a not secured route"
      println(responseAs[String])
    }
  }

  "Send param" should "return OK" in {
    Get(s"/unsecureWithParam?a=1&b=2","text/plain(UTF-8)") ~> paramsRoutes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      responseAs[String] shouldBe s"Hello param! This is a unsecured param route."
      println(responseAs[String])
    }
  }

  "Send POST" should "return OK" in {
    Post(s"/postMethod","this is the post content send inside the method") ~> postRoute ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      responseAs[String] shouldBe s"This is a POST request."
      println(responseAs[String])
    }
  }
}
