package com.dp

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json._
object SecurityDomain extends DefaultJsonProtocol {
  case class LoginRequest(username: String, password: String)
  implicit val loginRequestFormat: RootJsonFormat[LoginRequest] = jsonFormat2(LoginRequest)
}

// extend SprayJsonSupport we get implicit unmarshaller
object JwtAuthorization extends App with SprayJsonSupport{

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher
  import SecurityDomain._

  val superSecretPasswordDb = Map(
    "admin" -> "admin",
    "ali" -> "scalaRocks1"
  )

  val algorithm = JwtAlgorithm.HS256
  // this would be fetched from a secure place.
  val secretKey = "super_secret"

  def checkPassword(username: String, password: String): Boolean = {
    // password will be checked against the db'
    superSecretPasswordDb.contains(username) && superSecretPasswordDb(username) == password
  }

  def createToken(username: String, experiationPeriodInDays: Int): String  = {
    val claims = JwtClaim(
      expiration = Some(System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(experiationPeriodInDays)),
      issuedAt = Some(System.currentTimeMillis() / 1000),
      issuer = Some("scalaIsBeast.com")
    )

    JwtSprayJson.encode(claims, secretKey, algorithm)
  }

  def isTokenExpired(token: String): Boolean = ???

  def isTokenValid(token: String): Boolean= ???

  val loginRoute =
    post {
      entity(as[LoginRequest]) {
        case LoginRequest(username, password) if checkPassword(username, password) =>
          // only use token for 1 day
          val token = createToken(username, 1)
          // directive that allows http response to use http header
          respondWithHeader(RawHeader("Access-Token", token)) {
            complete(StatusCodes.OK)
          }
        case _ => complete(StatusCodes.Unauthorized)
      }
    }

  val authenticatedRoute =
    (path("secureEndpoint") & get) {
      optionalHeaderValueByName("Authorization") {
        case Some(token) if isTokenExpired(token) =>
          complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token expired."))
        case Some(token) if isTokenValid(token) =>
          // note: in actual app this would be some data that the user would access instead of just displaying a message.
          complete("User accessed authorized endpoint!")
        case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token is invalid, or has been tampered with"))
      }
    }

  // chained route so it always go through login Route
  val route = loginRoute ~ authenticatedRoute

  Http().bindAndHandle(route, "localhost", 8080)
}
