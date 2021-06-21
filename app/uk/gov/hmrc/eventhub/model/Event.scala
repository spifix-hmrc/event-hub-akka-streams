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

import org.bson.types.ObjectId

import java.time.{Instant, LocalDateTime}


import play.api.libs.functional.syntax._
import play.api.libs.json._

import uk.gov.hmrc.mongo.play.json.formats.MongoFormats
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.util.UUID


case class Event(eventId: UUID, subject: Option[String], timestamp: LocalDateTime, event: JsValue)

object Event {

  implicit val fmt = Json.format[Event]
}

case class MongoEvent(_id: ObjectId, createdAt: Instant, event: Event)

object MongoEvent {

  implicit val oif      = MongoFormats.objectIdFormat
  implicit val instantF = MongoJavatimeFormats.instantFormat
  implicit val fmt = ((JsPath \ "_id").format[ObjectId]
    ~ (JsPath \ "createdAt").format[Instant]
    ~ (JsPath \ "event").format[Event]
    )(MongoEvent.apply _, unlift(MongoEvent.unapply))

  def newMongoEvent(instant: Instant, e: Event) = MongoEvent(ObjectId.get(), instant, e)
}
