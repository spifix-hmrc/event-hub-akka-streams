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
import java.time.LocalDateTime
import org.mongodb.scala.Document
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats.Implicits.objectIdFormat
import play.api.libs.json.DefaultReads


import java.util.UUID


case class Event(messageId: UUID, subject: Option[String], timestamp: LocalDateTime, message: JsValue)

object Event {

  implicit val fmt = Json.format[Event]
}

case class MongoEvent(_id: ObjectId, event: Event)

object MongoEvent {
  def apply(event: Event): MongoEvent = MongoEvent(ObjectId.get, event)
  implicit val fmt = Json.format[MongoEvent]
}
