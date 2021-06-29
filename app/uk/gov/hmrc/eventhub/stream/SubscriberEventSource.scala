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

package uk.gov.hmrc.eventhub.stream

import akka.NotUsed
import akka.actor.Scheduler
import akka.pattern.Patterns.after
import akka.stream.scaladsl.Source
import uk.gov.hmrc.eventhub.model.SubscriberWorkItem
import uk.gov.hmrc.eventhub.repository.SubscriberQueueRepository
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.util.concurrent.Callable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Attempts to pull from work item queue when downstream signals demand, emits as soon as there is an available work item
 */
class SubscriberEventSource(subscriberQueueRepository: SubscriberQueueRepository)
                           (implicit scheduler: Scheduler, executionContext: ExecutionContext) {

  /**
   * TODO make `after` delay configurable
   */
  private def onPull: Unit => Future[Option[(Unit, WorkItem[SubscriberWorkItem])]] = _ =>
    subscriberQueueRepository
      .getEvent
      .flatMap {
        case None => after(500.millis, scheduler, executionContext, onPullCallable)
        case Some(event) => Future.successful(Some(() -> event))
      }

  private def onPullCallable: Callable[Future[Option[(Unit, WorkItem[SubscriberWorkItem])]]] =
    new Callable[Future[Option[(Unit, WorkItem[SubscriberWorkItem])]]] {
      override def call: Future[Option[(Unit, WorkItem[SubscriberWorkItem])]] = onPull(())
    }

  def source: Source[WorkItem[SubscriberWorkItem], NotUsed] =
    Source.unfoldAsync(())(onPull)
}
