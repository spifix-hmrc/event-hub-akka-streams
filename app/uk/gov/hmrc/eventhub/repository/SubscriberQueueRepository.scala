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

import play.api.Configuration
import uk.gov.hmrc.eventhub.model.SubscriberWorkItem
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemFields, WorkItemRepository}

import java.time.{Duration, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriberQueueRepository @Inject()(configuration : Configuration, mongo: MongoComponent)(implicit ec: ExecutionContext) extends WorkItemRepository[SubscriberWorkItem](
  mongoComponent = mongo,
  collectionName = "subscriber-queue",
  itemFormat   = SubscriberWorkItem.fmt,
  workItemFields = WorkItemFields.default
){

  override def now(): Instant =
    Instant.now()

  override val inProgressRetryAfter: Duration =
    configuration.underlying.getDuration("queue.retryAfter")

  def addSubscriberWorkItems(s: Seq[SubscriberWorkItem]): Future[Seq[WorkItem[SubscriberWorkItem]]] ={
    println(s"subscribing the following $s")
    try {
      pushNewBatch(s)
    } catch {
      case e => println(s"casthc ec $e")
        e
    }
    Future.successful(Seq.empty)
  }

}
