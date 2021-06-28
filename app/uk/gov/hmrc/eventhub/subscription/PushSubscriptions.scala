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

package uk.gov.hmrc.eventhub.subscription

import akka.actor.ActorSystem
import akka.stream.BoundedSourceQueue
import uk.gov.hmrc.eventhub.model.{Subscriber, SubscriberWorkItem}
import uk.gov.hmrc.eventhub.service.PublishEventService
import uk.gov.hmrc.mongo.workitem.WorkItem

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class PushSubscriptions(subscribers: List[Subscriber], publishEventService: PublishEventService)
                       (implicit actorSystem: ActorSystem, executionContext: ExecutionContext) {

  private val subscriptionQueues: Map[Subscriber, BoundedSourceQueue[WorkItem[SubscriberWorkItem]]] = subscribers.map { subscriber =>
    subscriber -> PushSubscription.subscriberQueue(subscriber, publishEventService)
  }.toMap

  def subscriberQueue(subscriber: Subscriber): Option[BoundedSourceQueue[WorkItem[SubscriberWorkItem]]] =
    subscriptionQueues.get(subscriber)

}
