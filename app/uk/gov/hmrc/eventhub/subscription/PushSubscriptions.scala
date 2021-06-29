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

import akka.NotUsed
import akka.actor.{ActorSystem, Scheduler}
import akka.stream.{Materializer, RestartSettings}
import akka.stream.scaladsl.{RestartSource, Sink}
import uk.gov.hmrc.eventhub.model.Subscriber
import uk.gov.hmrc.eventhub.repository.SubscriberQueueRepository
import uk.gov.hmrc.eventhub.service.PublishEventService
import uk.gov.hmrc.eventhub.stream.{EventPublishingHttpResponseHandler, HttpResponseHandler, SubscriberEventSource, SubscriberHttpFlow}

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class PushSubscriptions(
  subscribers: List[Subscriber],
  publishEventService: PublishEventService,
  subscriberQueueRepository: SubscriberQueueRepository,
  restartSettings: RestartSettings
)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer, scheduler: Scheduler) {

  private val _: List[NotUsed] = subscribers.map { subscriber =>
    val subscriberEventSource: SubscriberEventSource = new SubscriberEventSource(subscriberQueueRepository)
    val subscriberHttpFlow: SubscriberHttpFlow = new SubscriberHttpFlow(subscriber)
    val httpResponseHandler: HttpResponseHandler = new EventPublishingHttpResponseHandler(subscriber, publishEventService)
    val source = PushSubscription.subscriptionSource(subscriberEventSource, subscriberHttpFlow, httpResponseHandler)

    RestartSource
      .withBackoff(restartSettings) { () => source }
      .to(Sink.ignore)
      .run()
  }
}
