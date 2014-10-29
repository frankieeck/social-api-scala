package com.github.kfang.instagram

import com.github.kfang.ClientConfig
import com.github.kfang.instagram.models.{Error, AuthResponse}
import com.typesafe.config.Config
import spray.json._
import scala.util.{Failure, Try, Success}
import scalaj.http.Http

/**
 * http://instagram.com/developer/authentication/#
 */
class InstagramAPI(config: Config){

  val CLIENT_CONFIG   = new ClientConfig(config)
  val CONFIG          = config.getConfig("instagram-client")
  val CLIENT_ID       = CONFIG.getString("id")
  val CLIENT_SECRET   = CONFIG.getString("secret")
  val REDIRECT_URI    = CONFIG.getString("redirect-uri")

  //Authentication URLs
  private def AUTH_EXPLICIT_URL(redirect_uri: String): String =
    s"https://api.instagram.com/oauth/authorize/?client_id=$CLIENT_ID&redirect_uri=$redirect_uri&response_type=code"
  private def AUTH_IMPLICIT_URL(redirect_uri: String): String =
    s"https://api.instagram.com/oauth/authorize/?client_id=$CLIENT_ID&redirect_uri=$redirect_uri&response_type=token"
  private val REQUEST_ACCESS_TOKEN =
    s"https://api.instagram.com/oauth/access_token"

  private def genScopeParam(scopes: Seq[IGAuthScope]): String = {
    if(scopes.size == 0) "" else "scope=" + scopes.map(_.value).mkString("+")
  }

  def requestAccessToken(code: String, redirect_uri: String = REDIRECT_URI): AuthResponse = Try {
    val data = Map(
      "client_id" -> CLIENT_ID,
      "client_secret" -> CLIENT_SECRET,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> redirect_uri,
      "code" -> code
    )
      .toList
      .map({case (key, value) => s"$key=$value" })
      .mkString("&")

      Http
        .postData(REQUEST_ACCESS_TOKEN, data)
        .options(CLIENT_CONFIG.HTTP_OPTS)
        .asString
        .asJson
        .convertTo[AuthResponse]
  } match {
    case Success(ar) => ar
    case Failure(e)  => throw Error.parse(e)
  }

  //response_type=code
  def requestExplicitUrl(scopes: Seq[IGAuthScope], redirect_uri: String = REDIRECT_URI): String = {
    AUTH_EXPLICIT_URL(redirect_uri) + "&" + genScopeParam(scopes)
  }

  //response_type=token
  def requestImplicitUrl(scopes: Seq[IGAuthScope], redirect_uri: String = REDIRECT_URI): String = {
    AUTH_IMPLICIT_URL(redirect_uri) + "&" + genScopeParam(scopes)
  }

  //accessors for different services
  def usersService(accessToken: String): UsersService = new UsersService(accessToken, this)
  def relationshipsService(accessToken: String): RelationshipsService = new RelationshipsService(accessToken, this)

}


