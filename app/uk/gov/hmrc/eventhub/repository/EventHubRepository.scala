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


import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.{MongoCollection, SingleObservable}
import org.mongodb.scala.result.InsertOneResult
import play.api.Configuration
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory, PlayMongoRepository}

import javax.inject.Inject


class EventHubRepository @Inject()(config: Configuration, mongo: MongoComponent)(implicit ec: ExecutionContext) {


  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  val codecRegistry = fromRegistries(fromProviders(classOf[Person]), DEFAULT_CODEC_REGISTRY)



//  def createPerson2(person: Person): Future[Unit] = {
//    println(s"creating a person $person ${mongo.database.name}")
//    coll.insertOne(Person("jim", "col", 21))
//    val p: SingleObservable[InsertOneResult] = collection.insertOne(Person("jim", "c", 21))
//    println(s"tje p is $p")
//    println("done")
//    Future.successful(())
//  }

  import org.mongodb.scala._
  def createPerson(person: Person): Future[Unit] = {
    val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
    val database: MongoDatabase = mongoClient.getDatabase("event-hub").withCodecRegistry(codecRegistry)
    val collection: MongoCollection[Person] = database.getCollection("event-hub")

    val t = collection.insertOne(person).subscribe(new Observer[InsertOneResult] {
      override def onNext(result: InsertOneResult): Unit = println("Inserted")
      override def onError(e: Throwable): Unit     = println(s"Failed  ex = ${e.toString}")
      override def onComplete(): Unit              = println("Completed")
    })

    Thread.sleep(1000)
    Future.successful(())
  }





}



object Person {
  def apply(firstName: String, lastName: String): Person = Person(new ObjectId(), firstName, lastName);
}
case class Person(_id: ObjectId, firstName: String, lastName: String)
