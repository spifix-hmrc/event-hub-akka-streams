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

package uk.gov.hmrc.eventhub.service

import akka.actor.Cancellable
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, QueueOfferResult}
import uk.gov.hmrc.eventhub.model.SubscriberWorkItem
import uk.gov.hmrc.eventhub.repository.SubscriberQueueRepository
import uk.gov.hmrc.eventhub.subscription.PushSubscriptions
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class PushSubscriptionsService(
  pushSubscriptions: PushSubscriptions,
  subscriberQueueRepository: SubscriberQueueRepository,
  publishEventService: PublishEventService
)(implicit executionContext: ExecutionContext, materializer: Materializer) {

  def push: Cancellable = Source
    .tick(1.second, 1.second, ())
    .mapAsync(1)(onTick)
    .to(Sink.ignore)
    .run()

  /**
   * The message passing actor alternative is also recursive, this is a consequence of pulling one item at a time as
   * work item repo does not support batch pulls. Mongo has support for multi-document transaction but not using
   * `findAndModify`, so perhaps this recursive polling approach with a scheduler is the only viable solution, for now.
   * We could tune the tick interval and degree of parallelism to increase throughput and/or start pushing from more
   * instances of this class.
   */
  def onTick: Unit => Future[Unit] = _ =>
    subscriberQueueRepository
      .getEvent
      .flatMap {
        case None => Future.unit
        case Some(event) => sendEvent(event).flatMap(_ => onTick(()))
      }

  /**
   * TODO - discuss strategy for the scenarios below
   */
  def sendEvent(workItem: WorkItem[SubscriberWorkItem]): Future[Unit] = {
    pushSubscriptions.subscriberQueue(workItem.item.subscriber) match {
      case Some(queue) => queue.offer(workItem) match {
        case QueueOfferResult.Failure(ex) => Future.failed(ex)
        case QueueOfferResult.QueueClosed => Future.failed(new IllegalStateException("queue closed."))
        case QueueOfferResult.Enqueued => Future.unit
        case QueueOfferResult.Dropped => Future.unit
      }
      case None => publishEventService.deleteEvent(workItem).map(_ => ())
    }
  }
}
