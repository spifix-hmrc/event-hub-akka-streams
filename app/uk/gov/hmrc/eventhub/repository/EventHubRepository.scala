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

package uk.gov.hmrc.eventhub.repository
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{Json}

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.api.{AsyncDriver, Cursor, DB, MongoConnection}
import reactivemongo.api.bson.{BSONDocumentReader, BSONDocumentWriter, BSONObjectID, Macros, document}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.workitem.{WorkItem, WorkItemFieldNames, WorkItemRepository}
import uk.gov.hmrc.eventhub.model.NoticeOfCodingSuppression

import javax.inject.Inject

trait EventHubRepository {
  def createPerson(person: Person): Future[Unit]
}


class EventHubMongoRepository @Inject()(config: Configuration)(
                                       implicit mongo: () => DB,
                                       ec: ExecutionContext
)extends WorkItemRepository[NoticeOfCodingSuppression, BSONObjectID](
"noticeOfCodingSuppressionV2",
  mongo,
  WorkItem.workItemMongoFormat[NoticeOfCodingSuppression],
config.underlying
) {
  val mongoUri = "mongodb://localhost:27017"

  val driver = AsyncDriver()
  val parsedUri = MongoConnection.fromString(mongoUri)

  // Database and collections: Get references
  val futureConnection = parsedUri.flatMap(driver.connect(_))
  def db1: Future[DB] = futureConnection.flatMap(_.database("event-hub"))
  //def db2: Future[DB] = futureConnection.flatMap(_.database("anotherdb"))
  def personCollection = db1.map(_.collection("person"))

  implicit def personWriter: BSONDocumentWriter[Person] = Macros.writer[Person]
  // or provide a custom one

  // use personWriter
  def createPerson(person: Person): Future[Unit] = {
    println("creating a person")
    val t = personCollection.flatMap(_.insert.one(person).map(_ => {}))
    println(s"the person = $t")
    t
  }

  def updatePerson(person: Person): Future[Int] = {
    val selector = document(
      "firstName" -> person.firstName,
      "lastName" -> person.lastName
    )

    // Update the matching person
    personCollection.flatMap(_.update.one(selector, person).map(_.n))
  }

  implicit def personReader: BSONDocumentReader[Person] = Macros.reader[Person]
  // or provide a custom one

  def findPersonByAge(age: Int): Future[List[Person]] =
    personCollection.flatMap(_.find(document("age" -> age)). // query builder
      cursor[Person](). // using the result cursor
      collect[List](-1, Cursor.FailOnError[List[Person]]()))

  override def now: DateTime =
    DateTime.now

  override lazy val workItemFields: WorkItemFieldNames =
    new WorkItemFieldNames {
      val receivedAt   = "receivedAt"
      val updatedAt    = "updatedAt"
      val availableAt  = "receivedAt"
      val status       = "status"
      val id           = "_id"
      val failureCount = "failureCount"
    }

  override val inProgressRetryAfterProperty: String =
    "queue.retryAfter"

}

class WokeTest @Inject() () {

}

case class Person(firstName: String, lastName: String, age: Int)

object Person {
  implicit val residentFormat = Json.format[Person]
}
