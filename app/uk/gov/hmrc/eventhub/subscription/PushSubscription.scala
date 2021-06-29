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
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import play.api.Logging
import uk.gov.hmrc.eventhub.actors.SendEvent
import uk.gov.hmrc.eventhub.model.SubscriberWorkItem
import uk.gov.hmrc.eventhub.stream.{HttpResponseHandler, SubscriberEventSource, SubscriberHttpFlow}
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.ExecutionContext

object PushSubscription extends Logging {
  def subscriptionSource(
    subscriberEventSource: SubscriberEventSource,
    subscriberHttpFlow: SubscriberHttpFlow,
    httpResponseHandler: HttpResponseHandler
  )(implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer): Source[(SendEvent.CompletionStatus, WorkItem[SubscriberWorkItem]), NotUsed] = {
    subscriberEventSource
      .source
      .via(subscriberHttpFlow.flow)
      .mapAsync(parallelism = 8)(httpResponseHandler.handleResponse)
  }
}
