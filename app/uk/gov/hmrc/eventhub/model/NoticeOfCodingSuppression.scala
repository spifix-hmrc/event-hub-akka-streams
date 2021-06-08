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

import org.joda.time.DateTime
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json._
import reactivemongo.api.bson.BSONObjectID
import reactivemongo.api.bson.{BSONHandler, BSONReader, BSONWriter}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.util.{Failure, Success}

object NoticeOfCodingSuppression {

  implicit val dateFormats: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats

  implicit val objectIdRead: Reads[BSONObjectID] = Reads[BSONObjectID] { json =>
    (json \ "$oid").validate[String].flatMap { str =>
      BSONObjectID.parse(str) match {
        case Success(bsonId) => JsSuccess(bsonId)
        case Failure(err) => JsError(__, s"Invalid BSON Object ID $json; ${err.getMessage}")
      }
    }
  }

  implicit val objectIdWrite = new Writes[BSONObjectID] {
    def writes(objId: BSONObjectID) = Json.obj(
      "$oid"  -> objId.stringify
    )
  }

  implicit val objectIdFormats = Format(objectIdRead, objectIdWrite)

  implicit val payeSuppressionItemFormat = Json.format[NoticeOfCodingSuppression]
}

case class NoticeOfCodingSuppression(preferenceId: BSONObjectID, preferenceLastUpdated: DateTime)


