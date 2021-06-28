/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eventhub.model

import akka.http.scaladsl.model.Uri
import com.typesafe.config.Config
import play.api.ConfigLoader
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, OFormat}
import pureconfig.ConfigReader.Result
import pureconfig._
import pureconfig.generic.auto._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

case class Subscriber(name: String, uri: Uri, bufferSize: Int, elements: Int, per: FiniteDuration)

object Subscriber {
  implicit val uriReader: ConfigReader[Uri] = new ConfigReader[Uri] {
    def from(cur: ConfigCursor): Result[Uri] = cur.asString.map(s => Uri(s))
  }

  implicit val configLoader: ConfigLoader[Map[String, List[Subscriber]]] = (rootConfig: Config, path: String) =>
    ConfigSource
      .fromConfig(rootConfig.getConfig(path))
      .load[Map[String, List[Subscriber]]] match {
      case Left(value) => throw new IllegalArgumentException(s"could not load subscriber config: ${value.toList.mkString(" | ")}")
      case Right(value) => value
    }

  implicit object FiniteDurationFormat extends Format[FiniteDuration] {
    override def reads(json: JsValue): JsResult[FiniteDuration] = json match {
      case JsString(value) => Duration(value) match {
        case infinite: Duration.Infinite => JsError(s"expected a finite duration, but got infinite: $infinite")
        case duration: FiniteDuration  => JsSuccess(duration)
      }
      case x => JsError(s"expected a JsString, but got: $x")
    }

    override def writes(o: FiniteDuration): JsValue = JsString(o.toString())
  }

  implicit object UriFormat extends Format[Uri] {
    override def reads(json: JsValue): JsResult[Uri] = json match {
      case JsString(value) => JsResult.fromTry(Try(Uri(value)))
      case x => JsError(s"expected a JsString, but got: $x")
    }

    override def writes(o: Uri): JsValue = JsString(o.toString())
  }

  implicit val fmt: OFormat[Subscriber] = Json.format[Subscriber]
}

case class Topic(name: String, subscribers: List[Subscriber])

object Topic {
  implicit val configLoader: ConfigLoader[Map[String, List[Topic]]] = (rootConfig: Config, path: String) =>
    ConfigSource
      .fromConfig(rootConfig.getConfig(path))
      .load[Map[String, List[Topic]]] match {
      case Left(value) => throw new IllegalArgumentException(s"could not load topic config: ${value.toList.mkString(" | ")}")
      case Right(value) => value
    }
}
