package com.dp

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
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

  def checkPassword(username: String, password: String): Boolean = ???

  def createToken(username: String, experiationPeriodInDays: Int): String  = ???

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

  def isTokenExpired(token: String): Boolean = ???

  def isTokenValid(token: String): Boolean= ???

  val authenticatedRoute =
    (path("secureEndpoint") & get) {
      optionalHeaderValueByName("Authorization") {
        case Some(token) if isTokenExpired(token) =>
          complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token expired."))
        case Some(token) if isTokenValid(token) =>
          complete("User accessed authorized endpoint!")
        case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token is invalid, or has been tampered with"))
      }
    }
}
