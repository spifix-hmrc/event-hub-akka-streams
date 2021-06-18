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

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigValue}
import play.api.ConfigLoader
import play.api.libs.json.Json


case class Subscriber(name: String, endpoint: String)

object Subscriber {
  implicit val configLoader: ConfigLoader[Map[String, List[Subscriber]]] = (rootConfig: Config, path: String) =>
    rootConfig.getList(path).asScala.toList.map { cv =>
      val c: Config = cv.atKey("s")
      Subscriber(c.getString("s.topic"), c.getString("s.endpoint"))
    }.groupBy(_.name)

  implicit val fmt = Json.format[Subscriber]

}

case class Topic(name: String, subscribers: List[Subscriber])

object Topic {
  implicit val configLoader: ConfigLoader[Map[String, List[Topic]]] = (rootConfig: Config, path: String) =>
    rootConfig.getList(path).asScala.toList.map { cv: ConfigValue =>
      val t = cv.atKey("t")
      val name = t.getString("t.name")
      val s = t.getList("t.subscribers").asScala.toList.map{ sv: ConfigValue =>
        val s: Config = sv.atKey("s")
        Subscriber(s.getString("s.name"), s.getString("s.endpoint"))
      }
      Topic(name, s)
    }.groupBy(_.name)
}
